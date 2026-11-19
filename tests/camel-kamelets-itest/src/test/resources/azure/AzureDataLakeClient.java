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

import com.azure.core.http.HttpHeaderName;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Data Lake storage client for the emulator. The azure-storage-datalake component takes the one
 * DataLakeServiceClient of the registry when credentialType is SERVICE_CLIENT_INSTANCE. The emulator serves the Data
 * Lake API to requests whose Host header names the dfs endpoint of the account, so the client sets it.
 */
public class AzureDataLakeClient extends RouteBuilder {

    @PropertyInject("azure.url")
    private String url;

    @PropertyInject("azure.accountName")
    private String accountName;

    @PropertyInject("azure.accessKey")
    private String accessKey;

    @BindToRegistry("dataLakeServiceClient")
    public DataLakeServiceClient dataLakeServiceClient() {
        String host = accountName + ".dfs.core.windows.net";
        return new DataLakeServiceClientBuilder()
                .endpoint(url)
                .credential(new StorageSharedKeyCredential(accountName, accessKey))
                .addPolicy((context, next) -> {
                    context.getHttpRequest().setHeader(HttpHeaderName.HOST, host);
                    return next.process();
                })
                .buildClient();
    }

    @Override
    public void configure() {
        // only the client, no routes
    }
}
