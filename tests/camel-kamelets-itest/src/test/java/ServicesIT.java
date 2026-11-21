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
 * Tests of the Kamelets for hosted services: FHIR against a HAPI FHIR server, SNMP against an snmpd agent and IBM
 * Cloud Object Storage against the S3 API of Floci, each in a container, and Telegram and Salesforce against the
 * Citrus HTTP server, which stands in for the service with canned answers in the format of its API.
 */
@CitrusSupport
public class ServicesIT {

    private static final String TESTS = "(fhir|telegram|snmp|salesforce|ibm)-.*";

    private final int servicesServerPort = ServerPorts.next();

    // a container and a first Camel JBang run that downloads the client libraries take a while
    @BindToRegistry
    HttpServer servicesServer = http()
                .server()
                .port(servicesServerPort)
                .timeout(120000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeServices() {
        return new SequenceBeforeTest.Builder().onTests(TESTS).actions(
                createVariables().variable("services.server.port", String.valueOf(servicesServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterServices() {
        return new SequenceAfterTest.Builder().onTests(TESTS).actions(
                purgeEndpoints().endpoint(servicesServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> fhir() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("fhir");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> telegram() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("telegram");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> snmp() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("snmp");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> ibmCos() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("ibmcos");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> salesforce() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("salesforce");
    }
}
