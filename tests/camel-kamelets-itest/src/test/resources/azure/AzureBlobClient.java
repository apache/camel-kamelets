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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A Blob storage client for the emulator. The azure-storage-blob component takes the one BlobServiceClient of the
 * registry, so the Kamelets talk to the emulator, which serves the account under the path /{account}.
 */
public class AzureBlobClient extends RouteBuilder {

    private static final HttpHeaderName SERVER_ENCRYPTED = HttpHeaderName.fromString("x-ms-request-server-encrypted");

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
                .addPolicy(serverEncryptedHeader())
                .buildClient();
    }

    /**
     * The emulator leaves the x-ms-request-server-encrypted header out of its answer to Append Block, which Azure
     * always sends and the SDK reads without a null check, so this adds it.
     */
    private static HttpPipelinePolicy serverEncryptedHeader() {
        return (context, next) -> next.process()
                .map(response -> response.getHeaderValue(SERVER_ENCRYPTED) != null ? response : withServerEncrypted(response));
    }

    private static HttpResponse withServerEncrypted(HttpResponse response) {
        HttpHeaders headers = new HttpHeaders(response.getHeaders()).set(SERVER_ENCRYPTED, "true");
        return new HttpResponse(response.getRequest()) {
            @Override
            public int getStatusCode() {
                return response.getStatusCode();
            }

            @Override
            @Deprecated
            public String getHeaderValue(String name) {
                return headers.getValue(name);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }

            @Override
            public Flux<ByteBuffer> getBody() {
                return response.getBody();
            }

            @Override
            public Mono<byte[]> getBodyAsByteArray() {
                return response.getBodyAsByteArray();
            }

            @Override
            public Mono<String> getBodyAsString() {
                return response.getBodyAsString();
            }

            @Override
            public Mono<String> getBodyAsString(Charset charset) {
                return response.getBodyAsString(charset);
            }
        };
    }

    @Override
    public void configure() {
        // only the client, no routes
    }
}
