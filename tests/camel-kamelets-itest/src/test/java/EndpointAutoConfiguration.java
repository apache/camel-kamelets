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

import org.citrusframework.annotations.CitrusConfiguration;
import org.citrusframework.container.SequenceAfterTest;
import org.citrusframework.container.SequenceBeforeTest;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.springframework.http.HttpStatus;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;
import static org.citrusframework.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

@CitrusConfiguration
public class EndpointAutoConfiguration {

    private static final long SERVER_TIMEOUT = 60000L;

    private static final int HTTP_SERVER_PORT = SocketUtils.findAvailableTcpPort(8801);
    private static final int SLACK_SERVER_PORT = SocketUtils.findAvailableTcpPort(8802);
    private static final int PETSTORE_SERVER_PORT = SocketUtils.findAvailableTcpPort(8803);
    private static final int JIRA_SERVER_PORT = SocketUtils.findAvailableTcpPort(8804);

    private final HttpServer httpServer = http()
            .server()
            .port(HTTP_SERVER_PORT)
            .timeout(SERVER_TIMEOUT)
            .autoStart(true)
            .build();

    private final HttpServer slackServer = http()
            .server()
            .port(SLACK_SERVER_PORT)
            .timeout(SERVER_TIMEOUT)
            .autoStart(true)
            .build();

    private final HttpServer petstoreServer = http()
            .server()
            .port(PETSTORE_SERVER_PORT)
            .timeout(SERVER_TIMEOUT)
            .defaultStatus(HttpStatus.CREATED)
            .autoStart(true)
            .build();

    private final HttpServer jiraServer = http()
            .server()
            .port(JIRA_SERVER_PORT)
            .timeout(SERVER_TIMEOUT)
            .autoStart(true)
            .build();

    @BindToRegistry
    public HttpServer httpServer() {
        return httpServer;
    }

    @BindToRegistry
    public HttpServer slackServer() {
        return slackServer;
    }

    @BindToRegistry
    public HttpServer petstoreServer() {
        return petstoreServer;
    }

    @BindToRegistry
    public HttpServer jiraServer() {
        return jiraServer;
    }

    @BindToRegistry
    public SequenceBeforeTest beforeTest() {
        return SequenceBeforeTest.Builder.beforeTest()
                .actions(
                    // Set server ports as test variables
                    createVariables()
                            .variable("http.server.port", String.valueOf(HTTP_SERVER_PORT))
                            .variable("slack.server.port", String.valueOf(SLACK_SERVER_PORT))
                            .variable("petstore.server.port", String.valueOf(PETSTORE_SERVER_PORT))
                            .variable("jira.server.port", String.valueOf(JIRA_SERVER_PORT))
                )
                .build();
    }

    @BindToRegistry
    public SequenceAfterTest afterTest() {
        return SequenceAfterTest.Builder.afterTest()
                .actions(
                    // Auto purge Http server endpoint
                    purgeEndpoints()
                            .endpoint(httpServer)
                            .endpoint(slackServer)
                            .endpoint(petstoreServer)
                            .endpoint(jiraServer)
                )
                .build();
    }

}
