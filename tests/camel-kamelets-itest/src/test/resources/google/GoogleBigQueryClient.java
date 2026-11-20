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

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQueryOptions;
import org.apache.camel.BindToRegistry;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.bigquery.GoogleBigQueryConnectionFactory;

/**
 * A BigQuery client for the emulator. The google-bigquery component takes the one connection factory of the registry,
 * so the Kamelet talks to the emulator without logging in.
 */
public class GoogleBigQueryClient extends RouteBuilder {

    @PropertyInject("gcp.url")
    private String url;

    @PropertyInject("gcp.projectId")
    private String projectId;

    @BindToRegistry("bigQueryConnectionFactory")
    public GoogleBigQueryConnectionFactory bigQueryConnectionFactory() {
        return new GoogleBigQueryConnectionFactory(BigQueryOptions.newBuilder()
                .setHost(url)
                .setProjectId(projectId)
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService());
    }

    @Override
    public void configure() {
        // only the client, no routes
    }
}
