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
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.DynamicTest;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;
import static org.citrusframework.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

/**
 * Tests of the Google Cloud Kamelets against the Floci GCP emulator, which serves Pub/Sub, Cloud Storage and BigQuery
 * on one port.
 */
@CitrusSupport
public class GoogleCloudIT {

    private static final String TESTS = "google-.*";

    private final int gcpServerPort = SocketUtils.findAvailableTcpPort();

    // the emulator container and a first Camel JBang run that downloads the client libraries take a while
    @BindToRegistry
    HttpServer gcpServer = http()
                .server()
                .port(gcpServerPort)
                .timeout(120000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeGoogleCloud() {
        return new SequenceBeforeTest.Builder().onTests(TESTS).actions(
                createVariables().variable("gcp.server.port", String.valueOf(gcpServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterGoogleCloud() {
        return new SequenceAfterTest.Builder().onTests(TESTS).actions(
                purgeEndpoints().endpoint(gcpServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> google() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("google");
    }
}
