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

import java.nio.file.Path;

import org.citrusframework.annotations.CitrusConfiguration;
import org.citrusframework.camel.jbang.CamelJBangSettings;
import org.citrusframework.container.SequenceBeforeSuite;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.spi.BindToRegistry;
import org.springframework.http.HttpStatus;

import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

@CitrusConfiguration
public class EndpointAutoConfiguration {

    @BindToRegistry
    public HttpServer httpServer() {
        return http()
                .server()
                .port(8081)
                .defaultStatus(HttpStatus.CREATED)
                .timeout(120000L)
                .autoStart(true)
                .build();
    }

    @BindToRegistry
    public SequenceBeforeSuite setup() {
        // TODO: Workaround - remove when Citrus 4.5.1 is released
        return SequenceBeforeSuite.Builder.beforeSuite()
                .actions(context -> {
                    Path workDir = CamelJBangSettings.getWorkDir();
                    if (!workDir.toFile().exists() && !workDir.toFile().mkdirs()) {
                        throw new CitrusRuntimeException("Failed to create JBang working directory: %s".formatted(workDir.toAbsolutePath().toString()));
                    }
                })
                .build();
    }

}
