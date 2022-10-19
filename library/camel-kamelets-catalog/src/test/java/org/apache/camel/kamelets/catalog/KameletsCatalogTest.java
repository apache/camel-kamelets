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
package org.apache.camel.kamelets.catalog;

import io.fabric8.camelk.v1alpha1.Kamelet;
import io.fabric8.camelk.v1alpha1.JSONSchemaProps;
import io.github.classgraph.ClassGraph;
import org.apache.camel.kamelets.catalog.model.KameletTypeEnum;
import org.apache.camel.tooling.model.ComponentModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

public class KameletsCatalogTest {
    static KameletsCatalog catalog;

    @BeforeAll
    public static void createKameletsCatalog() {
        catalog = new KameletsCatalog();
    }

    @Test
    void testGetKameletsName() throws Exception {
        List<String> names = catalog.getKameletsName();
        assertTrue(!names.isEmpty());
    }

    @Test
    void testGetKamelets() throws Exception {
        Map<String, Kamelet> kamelets = catalog.getKamelets();
        assertTrue(!kamelets.isEmpty());
    }
    
    @Test
    void testGetKameletsDefinition() throws Exception {
        JSONSchemaProps props = catalog.getKameletDefinition("aws-sqs-source");
        assertEquals(14, props.getProperties().keySet().size());
        assertTrue(props.getProperties().keySet().contains("queueNameOrArn"));
    }

    @Test
    void testGetKameletsRequiredProperties() throws Exception {
        List<String> props = catalog.getKameletRequiredProperties("aws-sqs-source");
        assertEquals(2, props.size());
        assertTrue(props.contains("queueNameOrArn"));
    }

    @Test
    void testGetKameletsDefinitionNotExists() throws Exception {
        JSONSchemaProps props = catalog.getKameletDefinition("word");
        assertNull(props);
    }

    @Test
    void testGetKameletsByProvider() throws Exception {
        List<Kamelet> c = catalog.getKameletByProvider("Apache Software Foundation");
        assertTrue(!c.isEmpty());
        c = catalog.getKameletByProvider("Eclipse");
        assertTrue(c.isEmpty());
    }

    @Test
    void testGetKameletsByType() throws Exception {
        List<Kamelet> c = catalog.getKameletsByType(KameletTypeEnum.SOURCE.type());
        assertTrue(!c.isEmpty());
        c = catalog.getKameletsByType(KameletTypeEnum.SINK.type());
        assertTrue(!c.isEmpty());
        c = catalog.getKameletsByType(KameletTypeEnum.ACTION.type());
        assertTrue(!c.isEmpty());
    }

    @Test
    void testGetKameletsByGroup() throws Exception {
        List<Kamelet> c = catalog.getKameletsByGroups("AWS S3");
        assertTrue(!c.isEmpty());
        c = catalog.getKameletsByGroups("AWS SQS");
        assertTrue(!c.isEmpty());
        c = catalog.getKameletsByGroups("Not-existing-group");
        assertTrue(c.isEmpty());
    }

    @Test
    void testGetKameletsDependencies() throws Exception {
        List<String> deps = catalog.getKameletDependencies("aws-sqs-source");
        assertEquals(4, deps.size());
        deps = catalog.getKameletDependencies("cassandra-sink");
        assertEquals(3, deps.size());
        assertEquals("camel:jackson", deps.get(0));
    }

    @Test
    void testGetKameletsTemplate() throws Exception {
        Map<String, Object> template = catalog.getKameletTemplate("aws-sqs-source");
        assertNotNull(template);
    }
    
    @Test
    void testAllKameletFilesLoaded() throws Exception {
        int numberOfKameletFiles = new ClassGraph().acceptPaths("/" + KameletsCatalog.KAMELETS_DIR + "/").scan().getAllResources().size();
        assertEquals(numberOfKameletFiles, catalog.getKameletsName().size(), "Some embedded kamelet definition files cannot be loaded.");
    }

    @Test
    void testAllKameletDependencies() throws Exception {
        catalog.getAllKameletDependencies();
    }

    @Test
    void testSupportedHeaders() throws Exception {
        List<ComponentModel.EndpointHeaderModel> headersSource = catalog.getKameletSupportedHeaders("aws-s3-source");
        assertEquals(18, headersSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSink = catalog.getKameletSupportedHeaders("aws-s3-sink");
        assertEquals(25, headersSink.size());
        List<ComponentModel.EndpointHeaderModel> headerNotExistent = catalog.getKameletSupportedHeaders("aws-not-exists");
        assertEquals(0, headerNotExistent.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureSink = catalog.getKameletSupportedHeaders("azure-eventhubs-sink");
        assertEquals(2, headersAzureSink.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureFunctionsSink = catalog.getKameletSupportedHeaders("azure-functions-sink");
        assertEquals(8, headersAzureFunctionsSink.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureServicebusSource = catalog.getKameletSupportedHeaders("azure-servicebus-source");
        assertEquals(22, headersAzureServicebusSource.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureStorageBlobSource = catalog.getKameletSupportedHeaders("azure-storage-blob-source");
        assertEquals(34, headersAzureStorageBlobSource.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureStorageBlobSink = catalog.getKameletSupportedHeaders("azure-storage-blob-sink");
        assertEquals(32, headersAzureStorageBlobSink.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureStorageBlobChangeefeedSource = catalog.getKameletSupportedHeaders("azure-storage-blob-changefeed-source");
        assertEquals(34, headersAzureStorageBlobChangeefeedSource.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureStorageQueueSource = catalog.getKameletSupportedHeaders("azure-storage-queue-source");
        assertEquals(6, headersAzureStorageQueueSource.size());
        List<ComponentModel.EndpointHeaderModel> headersAzureStorageQueueSink = catalog.getKameletSupportedHeaders("azure-storage-queue-sink");
        assertEquals(16, headersAzureStorageQueueSink.size());
        List<ComponentModel.EndpointHeaderModel> headersCqlSink = catalog.getKameletSupportedHeaders("cassandra-sink");
        assertEquals(1, headersCqlSink.size());
        List<ComponentModel.EndpointHeaderModel> headersCqlSource = catalog.getKameletSupportedHeaders("cassandra-source");
        assertEquals(1, headersCqlSource.size());
        List<ComponentModel.EndpointHeaderModel> headersCouchbaseSink = catalog.getKameletSupportedHeaders("couchbase-sink");
        assertEquals(2, headersCouchbaseSink.size());
        List<ComponentModel.EndpointHeaderModel> headersDropboxSource = catalog.getKameletSupportedHeaders("dropbox-source");
        assertEquals(0, headersDropboxSource.size());
        List<ComponentModel.EndpointHeaderModel> headersDropboxSink = catalog.getKameletSupportedHeaders("dropbox-source");
        assertEquals(0, headersDropboxSink.size());
        List<ComponentModel.EndpointHeaderModel> headersESIndexSink = catalog.getKameletSupportedHeaders("elasticsearch-index-sink");
        assertEquals(8, headersESIndexSink.size());
        List<ComponentModel.EndpointHeaderModel> headersESSearchSource = catalog.getKameletSupportedHeaders("elasticsearch-search-source");
        assertEquals(8, headersESSearchSource.size());
        List<ComponentModel.EndpointHeaderModel> headersExecSink = catalog.getKameletSupportedHeaders("exec-sink");
        assertEquals(0, headersExecSink.size());
        List<ComponentModel.EndpointHeaderModel> headersFhirSource = catalog.getKameletSupportedHeaders("fhir-source");
        assertEquals(0, headersFhirSource.size());
        List<ComponentModel.EndpointHeaderModel> headersFileWatchSource = catalog.getKameletSupportedHeaders("file-watch-source");
        assertEquals(10, headersFileWatchSource.size());
        List<ComponentModel.EndpointHeaderModel> headersFtpSource = catalog.getKameletSupportedHeaders("ftp-source");
        assertEquals(10, headersFtpSource.size());
        List<ComponentModel.EndpointHeaderModel> headersFtpSink = catalog.getKameletSupportedHeaders("ftp-sink");
        assertEquals(8, headersFtpSink.size());
        List<ComponentModel.EndpointHeaderModel> headersFtpsSource = catalog.getKameletSupportedHeaders("ftps-source");
        assertEquals(10, headersFtpsSource.size());
        List<ComponentModel.EndpointHeaderModel> headersFtpsSink = catalog.getKameletSupportedHeaders("ftps-sink");
        assertEquals(8, headersFtpsSink.size());
        List<ComponentModel.EndpointHeaderModel> headersGhCommitSource = catalog.getKameletSupportedHeaders("github-commit-source");
        assertEquals(7, headersGhCommitSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGhEventSource = catalog.getKameletSupportedHeaders("github-event-source");
        assertEquals(7, headersGhEventSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGhPrCommentSource = catalog.getKameletSupportedHeaders("github-pullrequest-comment-source");
        assertEquals(7, headersGhPrCommentSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGhPrSource = catalog.getKameletSupportedHeaders("github-pullrequest-source");
        assertEquals(7, headersGhPrSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGhTagSource = catalog.getKameletSupportedHeaders("github-tag-source");
        assertEquals(7, headersGhTagSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleBigquerySink = catalog.getKameletSupportedHeaders("google-bigquery-sink");
        assertEquals(4, headersGoogleBigquerySink.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleCalendarSource = catalog.getKameletSupportedHeaders("google-calendar-source");
        assertEquals(1, headersGoogleCalendarSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleFunctionsSink = catalog.getKameletSupportedHeaders("google-functions-sink");
        assertEquals(5, headersGoogleFunctionsSink.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleMailSource = catalog.getKameletSupportedHeaders("google-mail-source");
        assertEquals(6, headersGoogleMailSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGooglePubsubSink = catalog.getKameletSupportedHeaders("google-pubsub-sink");
        assertEquals(3, headersGooglePubsubSink.size());
        List<ComponentModel.EndpointHeaderModel> headersGooglePubsubSource = catalog.getKameletSupportedHeaders("google-pubsub-source");
        assertEquals(4, headersGooglePubsubSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleSheetsSource = catalog.getKameletSupportedHeaders("google-sheets-source");
        assertEquals(6, headersGoogleSheetsSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleStorageSource = catalog.getKameletSupportedHeaders("google-storage-source");
        assertEquals(20, headersGoogleStorageSource.size());
        List<ComponentModel.EndpointHeaderModel> headersGoogleStorageSink = catalog.getKameletSupportedHeaders("google-storage-sink");
        assertEquals(13, headersGoogleStorageSink.size());
        List<ComponentModel.EndpointHeaderModel> headersHttpSource = catalog.getKameletSupportedHeaders("http-source");
        assertEquals(5, headersHttpSource.size());
        List<ComponentModel.EndpointHeaderModel> headersHttpSink = catalog.getKameletSupportedHeaders("http-sink");
        assertEquals(14, headersHttpSink.size());
        List<ComponentModel.EndpointHeaderModel> headersHttpSecuredSource = catalog.getKameletSupportedHeaders("http-secured-source");
        assertEquals(5, headersHttpSecuredSource.size());
        List<ComponentModel.EndpointHeaderModel> headersHttpSecuredSink = catalog.getKameletSupportedHeaders("http-secured-sink");
        assertEquals(14, headersHttpSecuredSink.size());
        List<ComponentModel.EndpointHeaderModel> headersInfinispanSource = catalog.getKameletSupportedHeaders("infinispan-source");
        assertEquals(6, headersInfinispanSource.size());
        List<ComponentModel.EndpointHeaderModel> headersInfinispanSink = catalog.getKameletSupportedHeaders("infinispan-sink");
        assertEquals(14, headersInfinispanSink.size());
        List<ComponentModel.EndpointHeaderModel> headersJiraAddCommentSink = catalog.getKameletSupportedHeaders("jira-add-comment-sink");
        assertEquals(16, headersJiraAddCommentSink.size());
        List<ComponentModel.EndpointHeaderModel> headersJiraAddIssueSink = catalog.getKameletSupportedHeaders("jira-add-issue-sink");
        assertEquals(16, headersJiraAddIssueSink.size());
        List<ComponentModel.EndpointHeaderModel> headersJiraSource= catalog.getKameletSupportedHeaders("jira-source");
        assertEquals(3, headersJiraSource.size());
        List<ComponentModel.EndpointHeaderModel> headersJiraOauthSource= catalog.getKameletSupportedHeaders("jira-oauth-source");
        assertEquals(3, headersJiraOauthSource.size());
    }
}
