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
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.spi.BindToRegistry;
import org.springframework.http.HttpStatus;

import static org.citrusframework.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;
import static org.citrusframework.jbang.actions.JBangAction.Builder.jbang;

@CitrusConfiguration
public class EndpointAutoConfiguration {

    private final HttpServer httpServer = http()
            .server()
            .port(8081)
            .defaultStatus(HttpStatus.CREATED)
            .timeout(60000L)
            .autoStart(true)
            .build();

    @BindToRegistry
    public HttpServer httpServer() {
        return httpServer;
    }

    @BindToRegistry
    public SequenceAfterTest afterTest() {
        return SequenceAfterTest.Builder.afterTest()
                .actions(
                    // Workaround to stop all Camel JBang integrations after test - remove when Citrus 4.5.2 is released
                    jbang().app("camel@apache/camel").command("stop"),
                    // Auto purge Http server endpoint
                    purgeEndpoints().endpoint(httpServer)
                )
                .build();
    }

}
