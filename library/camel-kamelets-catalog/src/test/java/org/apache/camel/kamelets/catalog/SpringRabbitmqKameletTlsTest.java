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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs spring-rabbitmq-sink and spring-rabbitmq-source against a local server that only does the TLS handshake (or
 * reads the AMQP protocol header), to check how they connect to the broker with each protocol.
 */
public class SpringRabbitmqKameletTlsTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final Path RESOURCES = Path.of("src/test/resources/spring-rabbitmq");
    // the broker certificate is for the host name localhost only
    private static final String BROKER_KEYSTORE = "broker-keystore.p12";
    private static final String TRUSTSTORE = "truststore.p12";
    private static final String STORE_PASSWORD = "changeit";
    private static final long TIMEOUT_SECONDS = 20;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ServerSocket server;
    private CamelContext context;

    @AfterEach
    void stop() throws Exception {
        if (server != null) {
            server.close();
        }
        if (context != null) {
            context.stop();
        }
        executor.shutdownNow();
    }

    @ParameterizedTest
    @ValueSource(strings = { "spring-rabbitmq-sink", "spring-rabbitmq-source" })
    void amqpsChecksBrokerCertificateWithJavaTrustStore(String kamelet) throws Exception {
        CompletableFuture<Boolean> handshake = startTlsServer();

        connect(kamelet, "localhost", "protocol=amqps");

        assertFalse(handshake.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the certificate of the broker is not in the trust store of Java, so the connection must fail");
    }

    @ParameterizedTest
    @ValueSource(strings = { "spring-rabbitmq-sink", "spring-rabbitmq-source" })
    void amqpsAcceptsBrokerCertificateFromTrustStore(String kamelet) throws Exception {
        CompletableFuture<Boolean> handshake = startTlsServer();

        connect(kamelet, "localhost", "protocol=amqps&" + trustStoreOptions());

        assertTrue(handshake.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the certificate of the broker is in sslTruststoreLocation, so the connection must succeed");
    }

    @ParameterizedTest
    @ValueSource(strings = { "spring-rabbitmq-sink", "spring-rabbitmq-source" })
    void amqpsVerifiesBrokerHostName(String kamelet) throws Exception {
        CompletableFuture<Boolean> handshake = startTlsServer();

        // the certificate names localhost, not the IP address
        connect(kamelet, InetAddress.getLoopbackAddress().getHostAddress(), "protocol=amqps&" + trustStoreOptions());

        assertFalse(handshake.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "the certificate of the broker does not name the host, so the connection must fail");
    }

    @ParameterizedTest
    @ValueSource(strings = { "spring-rabbitmq-sink", "spring-rabbitmq-source" })
    void amqpConnectsWithoutTls(String kamelet) throws Exception {
        CompletableFuture<String> header = startPlainServer();

        connect(kamelet, "localhost", "protocol=amqp");

        assertEquals("AMQP", header.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private static String trustStoreOptions() {
        return "sslTruststoreLocation=RAW(file:" + RESOURCES.resolve(TRUSTSTORE).toAbsolutePath()
               + ")&sslTruststorePassword=RAW(" + STORE_PASSWORD + ")";
    }

    /**
     * Starts the Kamelet, which connects to the broker at once: the sink when it sends a message, the source when its
     * route starts.
     */
    private void connect(String kamelet, String host, String options) throws Exception {
        String uri = "kamelet:" + kamelet + "?host=" + host + "&port=" + server.getLocalPort()
                     + "&exchangeName=orders&" + options;

        context = new DefaultCamelContext();
        context.getComponent("kamelet", KameletComponent.class)
                .setLocation("file:" + KAMELETS.toAbsolutePath().normalize());
        if (kamelet.endsWith("-source")) {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from(uri).process(exchange -> {
                    });
                }
            });
            context.start();
        } else {
            context.start();
            ProducerTemplate template = context.createProducerTemplate();
            try {
                template.sendBody(uri, "hello");
            } catch (CamelExecutionException e) {
                // the server is not a broker: it only does the TLS handshake or reads the protocol header
            }
        }
    }

    /**
     * Starts a TLS server with the broker certificate, for the first connection only.
     *
     * @return completes with whether the TLS handshake with the Kamelet succeeded, once it connected
     */
    private CompletableFuture<Boolean> startTlsServer() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(RESOURCES.resolve(BROKER_KEYSTORE))) {
            keyStore.load(is, STORE_PASSWORD.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, STORE_PASSWORD.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocket tls = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket();
        tls.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server = tls;

        CompletableFuture<Boolean> handshake = new CompletableFuture<>();
        executor.submit(() -> {
            try (Socket socket = accept()) {
                try {
                    ((SSLSocket) socket).startHandshake();
                    handshake.complete(true);
                } catch (IOException e) {
                    handshake.complete(false);
                }
            } catch (Exception e) {
                handshake.completeExceptionally(e);
            }
        });
        return handshake;
    }

    /**
     * Starts a plain server, for the first connection only.
     *
     * @return completes with the first 4 bytes the Kamelet sent
     */
    private CompletableFuture<String> startPlainServer() throws Exception {
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

        CompletableFuture<String> header = new CompletableFuture<>();
        executor.submit(() -> {
            try (Socket socket = accept()) {
                byte[] bytes = socket.getInputStream().readNBytes(4);
                header.complete(new String(bytes, StandardCharsets.US_ASCII));
            } catch (Exception e) {
                header.completeExceptionally(e);
            }
        });
        return header;
    }

    private Socket accept() throws IOException {
        server.setSoTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        Socket socket = server.accept();
        socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        return socket;
    }
}
