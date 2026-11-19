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
import java.util.Base64;
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
    private static final String TOKEN = "kamelet-token";
    // the credentials of the caller, on the incoming message, must not be sent on
    private static final String CALLER_AUTHORIZATION = "Bearer caller-token";
    private static final String CALLER_PROXY_AUTHORIZATION = "Basic Y2FsbGVyOnByb3h5";
    private static final String CALLER_KEY = "caller-key";

    private final BlockingQueue<String> configured = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> other = new LinkedBlockingQueue<>();
    private HttpServer configuredServer;
    private HttpServer otherServer;
    private int status = 200;
    // when true, a request without an Authorization header gets 401 with a Basic challenge
    private boolean challenge;
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

        assertEquals("GET /api/orders query=null key=null auth=null proxy=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void ogcApiFeaturesActionCallsTheConfiguredService() throws Exception {
        send("kamelet:ogcapi-features-action?url=" + url(configuredServer) + "&collection=lakes&limit=1", "");

        assertEquals("GET /collections/lakes/items query=limit=1&bbox=-180,-90,180,90&& key=null auth=null proxy=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void azureFunctionsSinkCallsTheConfiguredFunctionWithTheKeyInAHeader() throws Exception {
        Exchange result = send("kamelet:azure-functions-sink?url=" + url(configuredServer) + "/api/hello&key=" + KEY, "hello");

        assertEquals("POST /api/hello query=null key=" + KEY + " auth=null proxy=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
        assertFalse(result.getMessage().getHeaders().containsKey("x-functions-key"), "the key does not go on");
    }

    @Test
    void azureFunctionsSinkCallsAnAnonymousFunctionWithoutAKey() throws Exception {
        send("kamelet:azure-functions-sink?url=" + url(configuredServer) + "/api/hello", "hello");

        assertEquals("POST /api/hello query=null key=null auth=null proxy=null", configured.poll());
        assertTrue(other.isEmpty(), "no request goes to the other server");
    }

    @Test
    void azureFunctionsSinkKeepsTheKeyOutOfAFailedMessage() {
        status = 404;

        ProducerTemplate template = context.createProducerTemplate();
        Exchange result = template.request("kamelet:azure-functions-sink?url=" + url(configuredServer)
                                           + "/api/missing&key=" + KEY,
                exchange -> exchange.getMessage().setBody("hello"));

        assertEquals("POST /api/missing query=null key=" + KEY + " auth=null proxy=null", configured.poll());
        assertNotNull(result.getException(), "a 404 fails the message");
        assertFalse(result.getException().getMessage().contains(KEY), result.getException().getMessage());
        assertFalse(result.getMessage().getHeaders().containsKey("x-functions-key"), "the key does not go on");
    }

    @Test
    void restOpenApiSinkSendsTheBearerTokenOfTheOptions() throws Exception {
        Exchange result = send("kamelet:rest-openapi-sink?specification=file:" + SPEC.toAbsolutePath()
                               + "&operation=listOrders&host=" + url(configuredServer) + "&authBearerToken=" + TOKEN, "");

        assertEquals("GET /api/orders query=null key=null auth=Bearer " + TOKEN + " proxy=null", configured.poll());
        assertFalse(result.getMessage().getHeaders().containsKey("Authorization"), "the token does not go on");
    }

    @Test
    void restOpenApiSinkLogsInWithBasicWhenTheServiceAsks() throws Exception {
        challenge = true;
        // RAW keeps & and + of the password as they are
        send("kamelet:rest-openapi-sink?specification=file:" + SPEC.toAbsolutePath()
             + "&operation=listOrders&host=" + url(configuredServer)
             + "&authUsername=ann&authPassword=RAW(p&ss+word)", "");

        assertEquals("GET /api/orders query=null key=null auth=null proxy=null", configured.poll());
        assertEquals("GET /api/orders query=null key=null auth=" + basic("ann", "p&ss+word") + " proxy=null",
                configured.poll());
    }

    @Test
    void ogcApiFeaturesActionSendsTheBearerTokenOfTheOptions() throws Exception {
        send("kamelet:ogcapi-features-action?url=" + url(configuredServer) + "&collection=lakes&limit=1"
             + "&authMethod=Bearer&authBearerToken=" + TOKEN + "&authenticationPreemptive=true", "");

        assertEquals("GET /collections/lakes/items query=limit=1&bbox=-180,-90,180,90&& key=null auth=Bearer " + TOKEN
                     + " proxy=null", configured.poll());
    }

    @Test
    void ogcApiFeaturesActionLogsInWithBasicWhenTheServiceAsks() throws Exception {
        challenge = true;
        send("kamelet:ogcapi-features-action?url=" + url(configuredServer) + "&collection=lakes&limit=1"
             + "&authUsername=ann&authPassword=RAW(p&ss+word)", "");

        assertEquals("GET /collections/lakes/items query=limit=1&bbox=-180,-90,180,90&& key=null auth=null proxy=null",
                configured.poll());
        assertEquals("GET /collections/lakes/items query=limit=1&bbox=-180,-90,180,90&& key=null auth="
                     + basic("ann", "p&ss+word") + " proxy=null", configured.poll());
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
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
        headers.put("Authorization", CALLER_AUTHORIZATION);
        headers.put("Proxy-Authorization", CALLER_PROXY_AUTHORIZATION);
        if (uri.startsWith("kamelet:azure-functions-sink")) {
            // a header of the Azure Functions login, which only that Kamelet removes
            headers.put("x-functions-key", CALLER_KEY);
        }

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
     * Starts a server that records each request as "METHOD /path query=... key=... auth=... proxy=..." and answers with an empty GeoJSON
     * feature collection.
     */
    private HttpServer startServer(BlockingQueue<String> requests) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", (HttpExchange http) -> {
            requests.add(http.getRequestMethod() + " " + http.getRequestURI().getPath()
                         + " query=" + http.getRequestURI().getRawQuery()
                         + " key=" + http.getRequestHeaders().getFirst("x-functions-key")
                         + " auth=" + http.getRequestHeaders().getFirst("Authorization")
                         + " proxy=" + http.getRequestHeaders().getFirst("Proxy-Authorization"));
            http.getRequestBody().readAllBytes();
            if (challenge && http.getRequestHeaders().getFirst("Authorization") == null) {
                http.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"orders\"");
                http.sendResponseHeaders(401, -1);
                http.close();
                return;
            }
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
