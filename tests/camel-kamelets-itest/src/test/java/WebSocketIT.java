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

import java.util.stream.Stream;

import org.citrusframework.api.common.TestLoader;
import org.citrusframework.container.SequenceAfterTest;
import org.citrusframework.container.SequenceBeforeTest;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.junit.jupiter.CitrusTestFactory;
import org.citrusframework.junit.jupiter.CitrusTestFactorySupport;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.DynamicTest;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;
import static org.citrusframework.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

/**
 * Tests the websocket-source and websocket-sink Kamelets against a WebSocket server that is a Camel route in the
 * integration of the test, and the opening handshake against a Citrus HTTP server, which refuses it.
 */
@CitrusSupport
public class WebSocketIT {

    private static final String TESTS = "websocket-.*";

    private final int resultServerPort = ServerPorts.next();
    private final int controlServerPort = ServerPorts.next();
    private final int sourceServerPort = ServerPorts.next();
    private final int sinkServerPort = ServerPorts.next();

    /** Receives what the routes of the test got, and the opening handshake of the sink. */
    @BindToRegistry
    HttpServer websocketResultServer = http()
                .server()
                .port(resultServerPort)
                .timeout(60000L)
                .autoStart(true)
                .build();

    /** Tells the sink test when to send its next message, so that the messages reach the result server in order. */
    @BindToRegistry
    HttpServer websocketControlServer = http()
                .server()
                .port(controlServerPort)
                .timeout(60000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeWebSocket() {
        return new SequenceBeforeTest.Builder().onTests(TESTS).actions(
                createVariables()
                        .variable("websocket.result.port", String.valueOf(resultServerPort))
                        .variable("websocket.control.port", String.valueOf(controlServerPort))
                        .variable("websocket.source.port", String.valueOf(sourceServerPort))
                        .variable("websocket.sink.port", String.valueOf(sinkServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterWebSocket() {
        return new SequenceAfterTest.Builder().onTests(TESTS).actions(
                purgeEndpoints().endpoint(websocketResultServer).endpoint(websocketControlServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> websocket() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("websocket");
    }
}
