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
 * The SQL database Kamelets other than PostgreSQL (see {@link PostgresIT}): each test starts the database in a
 * container, and checks the rows through the command line client of the database inside it.
 */
@CitrusSupport
public class DatabaseIT {

    private final int httpServerPort = ServerPorts.next();

    @BindToRegistry
    HttpServer httpServer = http()
                .server()
                .port(httpServerPort)
                .timeout(60000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeDatabase() {
        return new SequenceBeforeTest.Builder().actions(
                createVariables().variable("http.server.port", String.valueOf(httpServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterDatabase() {
        return new SequenceAfterTest.Builder().actions(
                purgeEndpoints().endpoint(httpServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> mariadb() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("database.mariadb");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> mysql() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("database.mysql");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> sqlserver() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("database.sqlserver");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> oracle() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("database.oracle");
    }
}
