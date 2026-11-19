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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the HTTP Kamelets against a local server, with headers on the message that would send the request to another
 * server, path or query, to check that the request goes where the options say.
 */
public class HttpKameletsTargetTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final Path SPEC = Path.of("src/test/resources/remove-headers/orders-openapi.yaml");
    private static final String KEY = "secret-key";

    private final BlockingQueue<String> configured = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> other = new LinkedBlockingQueue<>();
    private HttpServer configuredServer;
    private HttpServer otherServer;
    private int status = 200;
    private CamelContext context;

    @BeforeEach
    void start() throws Exception {
        configuredServer = startServer(configured);
        otherServer = startServer(other);
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
        configuredServer.stop(0);
        otherServer.stop(0);
    }

    @Test
    void restOpenApiSinkCallsTheConfiguredService() throws Exception {
        send("kamelet:rest-openapi-sink?specification=file:" + SPEC.toAbsolutePath()
             + "&operation=listOrders&host=" + url(configuredServer), "");

        assertEquals("GET /api/orders query=null key=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void ogcApiFeaturesActionCallsTheConfiguredService() throws Exception {
        send("kamelet:ogcapi-features-action?url=" + url(configuredServer) + "&collection=lakes&limit=1", "");

        assertEquals("GET /collections/lakes/items query=limit=1&bbox=-180,-90,180,90&& key=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void azureFunctionsSinkCallsTheConfiguredFunctionWithTheKeyInAHeader() throws Exception {
        Exchange result = send("kamelet:azure-functions-sink?url=" + url(configuredServer) + "/api/hello&key=" + KEY, "hello");

        assertEquals("POST /api/hello query=null key=" + KEY, configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
        assertFalse(result.getMessage().getHeaders().containsKey("x-functions-key"), "the key does not go on");
    }

    @Test
    void azureFunctionsSinkCallsAnAnonymousFunctionWithoutAKey() throws Exception {
        send("kamelet:azure-functions-sink?url=" + url(configuredServer) + "/api/hello", "hello");

        assertEquals("POST /api/hello query=null key=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void azureFunctionsSinkKeepsTheKeyOutOfAFailedMessage() {
        status = 404;

        ProducerTemplate template = context.createProducerTemplate();
        Exchange result = template.request("kamelet:azure-functions-sink?url=" + url(configuredServer)
                                           + "/api/missing&key=" + KEY,
                exchange -> exchange.getMessage().setBody("hello"));

        assertEquals("POST /api/missing query=null key=" + KEY, configured.poll());
        assertNotNull(result.getException(), "a 404 fails the message");
        assertFalse(result.getException().getMessage().contains(KEY), result.getException().getMessage());
        assertFalse(result.getMessage().getHeaders().containsKey("x-functions-key"), "the key does not go on");
    }

    /**
     * Sends a message with the headers that, when the Kamelet keeps them, send the request to another server, path,
     * query or method.
     */
    private Exchange send(String uri, String body) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelHttpUri", url(otherServer) + "/other-uri");
        headers.put("CamelHttpPath", "/other-path");
        headers.put("CamelHttpQuery", "other=query");
        headers.put("CamelHttpRawQuery", "other=raw-query");
        headers.put("CamelHttpMethod", "DELETE");
        headers.put("CamelRestHttpUri", url(otherServer) + "/other-rest-uri");
        headers.put("CamelRestHttpQuery", "other=rest-query");

        ProducerTemplate template = context.createProducerTemplate();
        Exchange result = template.request(uri, exchange -> {
            exchange.getMessage().setHeaders(headers);
            exchange.getMessage().setBody(body);
        });
        if (result.getException() != null) {
            throw new CamelExecutionException("the call failed", result, result.getException());
        }
        return result;
    }

    private static String url(HttpServer server) {
        return "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort();
    }

    /**
     * Starts a server that records each request as "METHOD /path query=... key=..." and answers with an empty GeoJSON
     * feature collection.
     */
    private HttpServer startServer(BlockingQueue<String> requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", (HttpExchange http) -> {
            requests.add(http.getRequestMethod() + " " + http.getRequestURI().getPath()
                         + " query=" + http.getRequestURI().getRawQuery()
                         + " key=" + http.getRequestHeaders().getFirst("x-functions-key"));
            http.getRequestBody().readAllBytes();
            byte[] answer = "{\"type\":\"FeatureCollection\",\"features\":[]}".getBytes(StandardCharsets.UTF_8);
            http.getResponseHeaders().add("Content-Type", "application/json");
            http.sendResponseHeaders(status, answer.length);
            http.getResponseBody().write(answer);
            http.close();
        });
        server.start();
        return server;
    }
}
