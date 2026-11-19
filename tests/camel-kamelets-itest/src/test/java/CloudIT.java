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
 * Tests of the Kamelets of S3-compatible object storages, Ceph and MinIO, against the S3 API of Floci, and of
 * the GitHub sources against canned answers of the GitHub API.
 */
@CitrusSupport
public class CloudIT {

    private static final String TESTS = "(ceph|minio|github)-.*";

    private final int cloudHttpServerPort = SocketUtils.findAvailableTcpPort();

    // a container and a first Camel JBang run that downloads the libraries take a while
    @BindToRegistry
    HttpServer cloudHttpServer = http()
                .server()
                .port(cloudHttpServerPort)
                .timeout(120000L)
                .autoStart(true)
                .build();

    // stands in for the GitHub API, with canned answers
    private final int gitHubApiPort = SocketUtils.findAvailableTcpPort();

    @BindToRegistry
    GitHubApiStub gitHubApi = new GitHubApiStub(gitHubApiPort);

    @BindToRegistry
    HttpServer gitHubApiServer = http()
                .server()
                .port(gitHubApiPort)
                .endpointAdapter(gitHubApi)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeCloud() {
        return new SequenceBeforeTest.Builder().onTests(TESTS).actions(
                createVariables()
                        .variable("cloud.http.server.port", String.valueOf(cloudHttpServerPort))
                        .variable("github.api.port", String.valueOf(gitHubApiPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterCloud() {
        return new SequenceAfterTest.Builder().onTests(TESTS).actions(
                purgeEndpoints().endpoint(cloudHttpServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> ceph() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("ceph");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> minio() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("minio");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> github() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("github");
    }
}
