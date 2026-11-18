/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.kamelets.catalog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sun.net.httpserver.HttpServer;
import org.apache.camel.CamelContext;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs http-sink against a local server with an Authorization header on the message, to check that the login comes
 * only from the options of the Kamelet.
 */
public class HttpSinkAuthorizationTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final Map<String, Object> CALLER_CREDENTIALS
            = Map.of("Authorization", "Bearer caller-token", "Proxy-Authorization", "Basic Y2FsbGVyOnB3");

    private final BlockingQueue<String> requests = new LinkedBlockingQueue<>();
    private HttpServer server;
    private CamelContext context;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", http -> {
            requests.add("authorization=" + http.getRequestHeaders().getFirst("Authorization")
                         + " proxy-authorization=" + http.getRequestHeaders().getFirst("Proxy-Authorization"));
            http.getRequestBody().readAllBytes();
            http.sendResponseHeaders(200, -1);
            http.close();
        });
        server.start();
        context = new DefaultCamelContext();
        context.getComponent("kamelet", KameletComponent.class)
                .setLocation("file:" + KAMELETS.toAbsolutePath().normalize());
        context.start();
    }

    @AfterEach
    void stop() {
        if (context != null) {
            context.stop();
        }
        server.stop(0);
    }

    @Test
    void credentialOfTheMessageIsNotSentOn() {
        context.createProducerTemplate().sendBodyAndHeaders("kamelet:http-sink?url=" + url(), "hello",
                CALLER_CREDENTIALS);

        assertEquals("authorization=null proxy-authorization=null", requests.poll());
    }

    @Test
    void configuredLoginIsUsedInsteadOfTheMessage() {
        context.createProducerTemplate().sendBodyAndHeaders("kamelet:http-sink?url=" + url()
                                                            + "&authMethod=Basic&authUsername=operator&authPassword=op-pass"
                                                            + "&authenticationPreemptive=true",
                "hello", CALLER_CREDENTIALS);

        String basic = Base64.getEncoder().encodeToString("operator:op-pass".getBytes(StandardCharsets.UTF_8));
        assertEquals("authorization=Basic " + basic + " proxy-authorization=null", requests.poll());
    }

    private String url() {
        return "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort()
               + "/target";
    }
}
