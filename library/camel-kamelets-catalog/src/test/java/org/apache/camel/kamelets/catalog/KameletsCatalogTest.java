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
        verifyHeaders("aws-s3-source", 18);
        verifyHeaders("aws-s3-sink", 25);
        verifyHeaders("aws-cloudtrail-source", 0);
        verifyHeaders("aws-redshift-source", 0);
        verifyHeaders("aws-not-exists", 0);
        verifyHeaders("azure-eventhubs-sink", 2);
        verifyHeaders("azure-functions-sink", 8);
        verifyHeaders("azure-servicebus-source", 22);
        verifyHeaders("azure-storage-blob-source", 34);
        verifyHeaders("azure-storage-blob-sink", 33);
        verifyHeaders("azure-storage-blob-changefeed-source", 34);
        verifyHeaders("azure-storage-queue-source", 6);
        verifyHeaders("azure-storage-queue-sink", 16);
        verifyHeaders("cassandra-sink", 1);
        verifyHeaders("cassandra-source", 1);
        verifyHeaders("couchbase-sink", 2);
        verifyHeaders("dropbox-source", 0);
        verifyHeaders("dropbox-source", 0);
        verifyHeaders("elasticsearch-index-sink", 8);
        verifyHeaders("elasticsearch-search-source", 8);
        verifyHeaders("exec-sink", 0);
        verifyHeaders("fhir-source", 0);
        verifyHeaders("file-watch-source", 10);
        verifyHeaders("ftp-source", 10);
        verifyHeaders("ftp-sink", 8);
        verifyHeaders("ftps-source", 10);
        verifyHeaders("ftps-sink", 8);
        verifyHeaders("github-commit-source", 7);
        verifyHeaders("github-event-source", 7);
        verifyHeaders("github-pullrequest-comment-source", 7);
        verifyHeaders("github-pullrequest-source", 7);
        verifyHeaders("github-tag-source", 7);
        verifyHeaders("google-bigquery-sink", 4);
        verifyHeaders("google-calendar-source", 1);
        verifyHeaders("google-functions-sink", 5);
        verifyHeaders("google-mail-source", 6);
        verifyHeaders("google-pubsub-sink", 3);
        verifyHeaders("google-pubsub-source", 4);
        verifyHeaders("google-sheets-source", 6);
        verifyHeaders("google-storage-source", 20);
        verifyHeaders("google-storage-sink", 13);
        verifyHeaders("http-source", 5);
        verifyHeaders("http-sink", 14);
        verifyHeaders("http-secured-source", 5);
        verifyHeaders("http-secured-sink", 14);
        verifyHeaders("infinispan-source", 6);
        verifyHeaders("infinispan-sink", 14);
        verifyHeaders("jira-add-comment-sink", 16);
        verifyHeaders("jira-add-issue-sink", 16);
        verifyHeaders("jira-source", 3);
        verifyHeaders("jira-oauth-source", 3);
        verifyHeaders("jms-amqp-10-source", 14);
        verifyHeaders("jms-amqp-10-sink", 17);
        verifyHeaders("jms-apache-activemq-source", 14);
        verifyHeaders("jms-apache-activemq-sink", 17);
        verifyHeaders("jms-apache-artemis-source", 14);
        verifyHeaders("jms-apache-artemis-sink", 17);
        verifyHeaders("jms-ibm-mq-source", 14);
        verifyHeaders("jms-ibm-mq-sink", 17);
        verifyHeaders("kafka-source", 9);
        verifyHeaders("kafka-sink", 5);
        verifyHeaders("kafka-ssl-source", 9);
        verifyHeaders("kafka-ssl-sink", 5);
        verifyHeaders("kafka-not-secured-source", 9);
        verifyHeaders("kafka-not-secured-sink", 5);
        verifyHeaders("kubernetes-namespaces-source", 2);
        verifyHeaders("kubernetes-nodes-source", 2);
        verifyHeaders("kubernetes-pods-source", 2);
        verifyHeaders("log-sink", 0);
        verifyHeaders("mail-source", 0);
        verifyHeaders("mail-sink", 8);
        verifyHeaders("mariadb-source", 0);
        verifyHeaders("mariadb-sink", 8);
        verifyHeaders("minio-source", 14);
        verifyHeaders("minio-sink", 21);
        verifyHeaders("mongodb-changes-stream-source", 3);
        verifyHeaders("mongodb-sink", 12);
        verifyHeaders("mongodb-source", 3);
        verifyHeaders("mqtt-sink", 3);
        verifyHeaders("mqtt-source", 2);
        verifyHeaders("mqtt5-sink", 3);
        verifyHeaders("mqtt5-source", 2);
        verifyHeaders("mysql-sink", 8);
        verifyHeaders("mysql-source", 0);
        verifyHeaders("nats-sink", 5);
        verifyHeaders("nats-source", 5);
        verifyHeaders("oracle-database-sink", 8);
        verifyHeaders("oracle-database-source", 0);
        verifyHeaders("postgresql-sink", 8);
        verifyHeaders("postgresql-source", 0);
        verifyHeaders("pulsar-sink", 3);
        verifyHeaders("pulsar-source", 11);
        verifyHeaders("rabbitmq-source", 23);
        verifyHeaders("redis-sink", 29);
        verifyHeaders("redis-source", 28);
        verifyHeaders("rest-openapi-sink", 0);
        verifyHeaders("salesforce-create-sink", 1);
        verifyHeaders("salesforce-delete-sink", 1);
        verifyHeaders("salesforce-update-sink", 1);
        verifyHeaders("salesforce-source", 18);
        verifyHeaders("scp-sink", 0);
        verifyHeaders("sftp-sink", 8);
        verifyHeaders("sftp-source", 10);
        verifyHeaders("slack-sink", 0);
        verifyHeaders("slack-source", 0);
        verifyHeaders("solr-sink", 5);
        verifyHeaders("solr-source", 5);
        verifyHeaders("splunk-hec-sink", 1);
        verifyHeaders("splunk-sink", 0);
        verifyHeaders("splunk-source", 0);
        verifyHeaders("sqlserver-sink", 8);
        verifyHeaders("sqlserver-source", 0);
        verifyHeaders("ssh-sink", 4);
        verifyHeaders("ssh-source", 4);
        verifyHeaders("telegram-sink", 6);
        verifyHeaders("telegram-source", 5);
        verifyHeaders("timer-source", 2);
        verifyHeaders("twitter-directmessage-source", 2);
        verifyHeaders("twitter-timeline-source", 1);
        verifyHeaders("twitter-search-source", 7);
        verifyHeaders("webhook-source", 0);
        verifyHeaders("websocket-source", 4);
        verifyHeaders("wttrin-source", 5);
    }

    void verifyHeaders(String name, int expected) {
        List<ComponentModel.EndpointHeaderModel> headers = catalog.getKameletSupportedHeaders(name);
        assertEquals(expected, headers.size());
    }


    @Test
    void testGetKameletScheme() throws Exception {
        assertEquals("aws2-s3", catalog.getKameletScheme("aws-s3"));
        assertEquals("aws2-sqs", catalog.getKameletScheme("aws-sqs"));
        assertNull(catalog.getKameletScheme("not-known"));
    }
}
