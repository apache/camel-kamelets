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

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Cosmos DB client for the emulator. The azure-cosmosdb component takes the one CosmosAsyncClient of the registry,
 * so the Kamelet talks to the emulator in gateway mode, over HTTPS without checking its self-signed certificate.
 */
public class AzureCosmosClient extends RouteBuilder {

    @PropertyInject("azure.cosmosUrl")
    private String url;

    @PropertyInject("azure.accessKey")
    private String accessKey;

    @BindToRegistry("cosmosAsyncClient")
    public CosmosAsyncClient cosmosAsyncClient() {
        System.setProperty("COSMOS.EMULATOR_SERVER_CERTIFICATE_VALIDATION_DISABLED", "true");
        return new CosmosClientBuilder()
                .endpoint(url)
                .key(accessKey)
                .gatewayMode()
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();
    }

    @Override
    public void configure() {
        // only the client, no routes
    }
}
