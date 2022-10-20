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
        List<ComponentModel.EndpointHeaderModel> headersRedshiftSource = catalog.getKameletSupportedHeaders("aws-redshift-source");
        assertEquals(0, headersRedshiftSource.size());
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
        List<ComponentModel.EndpointHeaderModel> headersJmsAmqp10Source= catalog.getKameletSupportedHeaders("jms-amqp-10-source");
        assertEquals(14, headersJmsAmqp10Source.size());
        List<ComponentModel.EndpointHeaderModel> headersJmsAmqp10Sink= catalog.getKameletSupportedHeaders("jms-amqp-10-sink");
        assertEquals(17, headersJmsAmqp10Sink.size());
        List<ComponentModel.EndpointHeaderModel> headersActivemqSource= catalog.getKameletSupportedHeaders("jms-apache-activemq-source");
        assertEquals(0, headersActivemqSource.size());
        List<ComponentModel.EndpointHeaderModel> headersActivemqSink= catalog.getKameletSupportedHeaders("jms-apache-activemq-sink");
        assertEquals(0, headersActivemqSink.size());
        List<ComponentModel.EndpointHeaderModel> headersJmsArtemisSource= catalog.getKameletSupportedHeaders("jms-apache-artemis-source");
        assertEquals(14, headersJmsArtemisSource.size());
        List<ComponentModel.EndpointHeaderModel> headersJmsArtemisSink= catalog.getKameletSupportedHeaders("jms-apache-artemis-sink");
        assertEquals(17, headersJmsArtemisSink.size());
        List<ComponentModel.EndpointHeaderModel> headersJmsIBMSource= catalog.getKameletSupportedHeaders("jms-ibm-mq-source");
        assertEquals(14, headersJmsIBMSource.size());
        List<ComponentModel.EndpointHeaderModel> headersJmsIBMSink= catalog.getKameletSupportedHeaders("jms-ibm-mq-sink");
        assertEquals(17, headersJmsIBMSink.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaSource= catalog.getKameletSupportedHeaders("kafka-source");
        assertEquals(9, headersKafkaSource.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaSink= catalog.getKameletSupportedHeaders("kafka-sink");
        assertEquals(5, headersKafkaSink.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaSSLSource= catalog.getKameletSupportedHeaders("kafka-ssl-source");
        assertEquals(9, headersKafkaSSLSource.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaSSLSink= catalog.getKameletSupportedHeaders("kafka-ssl-sink");
        assertEquals(5, headersKafkaSSLSink.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaNotSecuredSource= catalog.getKameletSupportedHeaders("kafka-not-secured-source");
        assertEquals(9, headersKafkaNotSecuredSource.size());
        List<ComponentModel.EndpointHeaderModel> headersKafkaNotSecuredSink= catalog.getKameletSupportedHeaders("kafka-not-secured-sink");
        assertEquals(5, headersKafkaNotSecuredSink.size());
        List<ComponentModel.EndpointHeaderModel> headersKubeNamespacesSource= catalog.getKameletSupportedHeaders("kubernetes-namespaces-source");
        assertEquals(2, headersKubeNamespacesSource.size());
        List<ComponentModel.EndpointHeaderModel> headersKubeNodesSource= catalog.getKameletSupportedHeaders("kubernetes-nodes-source");
        assertEquals(2, headersKubeNodesSource.size());
        List<ComponentModel.EndpointHeaderModel> headersKubePodsSource= catalog.getKameletSupportedHeaders("kubernetes-pods-source");
        assertEquals(2, headersKubePodsSource.size());
        List<ComponentModel.EndpointHeaderModel> headersLogSink= catalog.getKameletSupportedHeaders("log-sink");
        assertEquals(0, headersLogSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMailSource= catalog.getKameletSupportedHeaders("mail-source");
        assertEquals(0, headersMailSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMailSink= catalog.getKameletSupportedHeaders("mail-sink");
        assertEquals(8, headersMailSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMariaDBSource= catalog.getKameletSupportedHeaders("mariadb-source");
        assertEquals(0, headersMariaDBSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMariaDBSink= catalog.getKameletSupportedHeaders("mariadb-sink");
        assertEquals(8, headersMariaDBSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMinioSource= catalog.getKameletSupportedHeaders("minio-source");
        assertEquals(14, headersMinioSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMinioSink= catalog.getKameletSupportedHeaders("minio-sink");
        assertEquals(21, headersMinioSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMongodbChangesStreamSource= catalog.getKameletSupportedHeaders("mongodb-changes-stream-source");
        assertEquals(3, headersMongodbChangesStreamSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMongoDbSink= catalog.getKameletSupportedHeaders("mongodb-sink");
        assertEquals(12, headersMongoDbSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMongoDbSource= catalog.getKameletSupportedHeaders("mongodb-source");
        assertEquals(3, headersMongoDbSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMQTTSink= catalog.getKameletSupportedHeaders("mqtt-sink");
        assertEquals(3, headersMQTTSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMQTTSource= catalog.getKameletSupportedHeaders("mqtt-source");
        assertEquals(2, headersMQTTSource.size());
        List<ComponentModel.EndpointHeaderModel> headersMQTT5Sink= catalog.getKameletSupportedHeaders("mqtt5-sink");
        assertEquals(3, headersMQTT5Sink.size());
        List<ComponentModel.EndpointHeaderModel> headersMQTT5Source= catalog.getKameletSupportedHeaders("mqtt5-source");
        assertEquals(2, headersMQTT5Source.size());
        List<ComponentModel.EndpointHeaderModel> headersMySQLSink= catalog.getKameletSupportedHeaders("mysql-sink");
        assertEquals(8, headersMySQLSink.size());
        List<ComponentModel.EndpointHeaderModel> headersMySQLSource= catalog.getKameletSupportedHeaders("mysql-source");
        assertEquals(0, headersMySQLSource.size());
        List<ComponentModel.EndpointHeaderModel> headersNatsSink= catalog.getKameletSupportedHeaders("nats-sink");
        assertEquals(5, headersNatsSink.size());
        List<ComponentModel.EndpointHeaderModel> headersNatsSource= catalog.getKameletSupportedHeaders("nats-source");
        assertEquals(5, headersNatsSource.size());
        List<ComponentModel.EndpointHeaderModel> headersOracleDBSink= catalog.getKameletSupportedHeaders("oracle-database-sink");
        assertEquals(8, headersOracleDBSink.size());
        List<ComponentModel.EndpointHeaderModel> headersOracleDBSource= catalog.getKameletSupportedHeaders("oracle-database-source");
        assertEquals(0, headersOracleDBSource.size());
        List<ComponentModel.EndpointHeaderModel> headersPostgreSQLSink= catalog.getKameletSupportedHeaders("postgresql-sink");
        assertEquals(8, headersPostgreSQLSink.size());
        List<ComponentModel.EndpointHeaderModel> headersPostgreSQLSource= catalog.getKameletSupportedHeaders("postgresql-source");
        assertEquals(0, headersPostgreSQLSource.size());
        List<ComponentModel.EndpointHeaderModel> headersPulsarSink= catalog.getKameletSupportedHeaders("pulsar-sink");
        assertEquals(3, headersPulsarSink.size());
        List<ComponentModel.EndpointHeaderModel> headersPulsarSource= catalog.getKameletSupportedHeaders("pulsar-source");
        assertEquals(11, headersPulsarSource.size());
        List<ComponentModel.EndpointHeaderModel> headersRabbitMQSource= catalog.getKameletSupportedHeaders("rabbitmq-source");
        assertEquals(23, headersRabbitMQSource.size());
        List<ComponentModel.EndpointHeaderModel> headersRedisSink= catalog.getKameletSupportedHeaders("redis-sink");
        assertEquals(29, headersRedisSink.size());
        List<ComponentModel.EndpointHeaderModel> headersRedisSource= catalog.getKameletSupportedHeaders("redis-source");
        assertEquals(28, headersRedisSource.size());
        List<ComponentModel.EndpointHeaderModel> headersRestOpenAPISink= catalog.getKameletSupportedHeaders("rest-openapi-sink");
        assertEquals(0, headersRestOpenAPISink.size());
        List<ComponentModel.EndpointHeaderModel> headersSalesforceCreateSink= catalog.getKameletSupportedHeaders("salesforce-create-sink");
        assertEquals(1, headersSalesforceCreateSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSalesforceDeleteSink= catalog.getKameletSupportedHeaders("salesforce-delete-sink");
        assertEquals(1, headersSalesforceDeleteSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSalesforceUpdateSink= catalog.getKameletSupportedHeaders("salesforce-update-sink");
        assertEquals(1, headersSalesforceUpdateSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSalesforceSource= catalog.getKameletSupportedHeaders("salesforce-source");
        assertEquals(18, headersSalesforceSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSCPSink= catalog.getKameletSupportedHeaders("scp-sink");
        assertEquals(0, headersSCPSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSFTPSink= catalog.getKameletSupportedHeaders("sftp-sink");
        assertEquals(8, headersSFTPSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSFTPSource= catalog.getKameletSupportedHeaders("sftp-source");
        assertEquals(10, headersSFTPSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSlackSink= catalog.getKameletSupportedHeaders("slack-sink");
        assertEquals(0, headersSlackSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSlackSource= catalog.getKameletSupportedHeaders("slack-source");
        assertEquals(0, headersSlackSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSolrSink= catalog.getKameletSupportedHeaders("solr-sink");
        assertEquals(5, headersSolrSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSolrSource= catalog.getKameletSupportedHeaders("solr-source");
        assertEquals(5, headersSolrSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSplunkHecSink= catalog.getKameletSupportedHeaders("splunk-hec-sink");
        assertEquals(1, headersSplunkHecSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSplunkSink= catalog.getKameletSupportedHeaders("splunk-sink");
        assertEquals(0, headersSplunkSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSplunkSource= catalog.getKameletSupportedHeaders("splunk-source");
        assertEquals(0, headersSplunkSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSqlServerSink= catalog.getKameletSupportedHeaders("sqlserver-sink");
        assertEquals(8, headersSqlServerSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSqlServerSource= catalog.getKameletSupportedHeaders("sqlserver-source");
        assertEquals(0, headersSqlServerSource.size());
        List<ComponentModel.EndpointHeaderModel> headersSSHSink= catalog.getKameletSupportedHeaders("ssh-sink");
        assertEquals(4, headersSSHSink.size());
        List<ComponentModel.EndpointHeaderModel> headersSSHSource= catalog.getKameletSupportedHeaders("ssh-source");
        assertEquals(4, headersSSHSource.size());
        List<ComponentModel.EndpointHeaderModel> headersTelegramSink= catalog.getKameletSupportedHeaders("telegram-sink");
        assertEquals(6, headersTelegramSink.size());
        List<ComponentModel.EndpointHeaderModel> headersTelegramSource= catalog.getKameletSupportedHeaders("telegram-source");
        assertEquals(5, headersTelegramSource.size());
    }
}
