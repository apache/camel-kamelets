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
 * Runs http-sink against a local server with a password that has characters with a meaning in an address, to check
 * that the whole password is used for the login and nothing of it ends up in the address of the request.
 */
public class HttpSinkSecretTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final String PASSWORD = "p&ss+w=rd";

    private final BlockingQueue<String> requests = new LinkedBlockingQueue<>();
    private HttpServer server;
    private CamelContext context;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", http -> {
            requests.add(http.getRequestURI().getPath() + " query=" + http.getRequestURI().getRawQuery()
                         + " authorization=" + http.getRequestHeaders().getFirst("Authorization"));
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
    void passwordWithSpecialCharactersIsSentWhole() {
        String url = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort()
                     + "/secure";

        context.createProducerTemplate().sendBody("kamelet:http-sink?url=" + url
                                                  + "&authMethod=Basic&authUsername=user&authPassword=RAW(" + PASSWORD
                                                  + ")&authenticationPreemptive=true",
                "hello");

        String basic = Base64.getEncoder().encodeToString(("user:" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        assertEquals("/secure query=null authorization=Basic " + basic, requests.poll());
    }
}
