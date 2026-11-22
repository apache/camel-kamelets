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
import org.citrusframework.container.SequenceBeforeTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.junit.jupiter.CitrusTestFactory;
import org.citrusframework.junit.jupiter.CitrusTestFactorySupport;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.DynamicTest;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;

/**
 * The SOAP Kamelets: soap-source serves the WSDL of the tests, and soap-action calls it. Each test serves its services
 * on ports of its own, so that a service of one test does not hold the port of the next while it stops.
 */
@CitrusSupport
public class SoapIT {

    private final int backendPort = ServerPorts.next();
    private final int frontPort = ServerPorts.next();
    private final int soap12Port = ServerPorts.next();

    @BindToRegistry
    public SequenceBeforeTest beforeSoap() {
        return new SequenceBeforeTest.Builder().onTests("soap-*").actions(
                createVariables()
                        .variable("soap.backend.port", String.valueOf(backendPort))
                        .variable("soap.front.port", String.valueOf(frontPort))
                        .variable("soap.soap12.port", String.valueOf(soap12Port))
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> soap() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("soap");
    }
}
