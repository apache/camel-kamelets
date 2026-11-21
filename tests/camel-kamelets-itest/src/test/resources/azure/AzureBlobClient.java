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

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Blob storage client for the emulator. The azure-storage-blob component takes the one BlobServiceClient of the
 * registry, so the Kamelets talk to the emulator, which serves the account under the path /{account}.
 */
public class AzureBlobClient extends RouteBuilder {

    @PropertyInject("azure.url")
    private String url;

    @PropertyInject("azure.accountName")
    private String accountName;

    @PropertyInject("azure.accessKey")
    private String accessKey;

    @BindToRegistry("blobServiceClient")
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint(url + "/" + accountName)
                .credential(new StorageSharedKeyCredential(accountName, accessKey))
                .buildClient();
    }

    @Override
    public void configure() {
        // only the client, no routes
    }
}
