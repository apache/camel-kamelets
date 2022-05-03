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
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.github.classgraph.ClassGraph;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.kamelets.catalog.model.KameletTypeEnum;
import org.apache.camel.tooling.model.ComponentModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        assertEquals(12, props.getProperties().keySet().size());
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
        assertEquals(2, deps.size());
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
    void testOptionsParams() throws Exception {
        String[] bannedDeps = {"mvn:", "camel:gson", "camel:core", "camel:kamelet", "github:apache.camel-kamelets:camel-kamelets-utils:main-SNAPSHOT"};
        List<String> bannedDepsList = Arrays.asList(bannedDeps);
        DefaultCamelCatalog cc = new DefaultCamelCatalog();
        List<String> names = catalog.getKameletsName();
        for (String name:
             names) {
            Map<String, Object> kd = catalog.getKameletTemplate(name);
            Map<String,Object> f = (Map) kd.get("from");
            Map<String,Object> p = (Map) f.get("parameters");
            List<String> deps = catalog.getKameletDependencies(name).stream()
                    .filter(Predicate.not(bannedDepsList::contains)).collect(Collectors.toList());
            String cleanName;
            if (!deps.isEmpty()) {
                if (deps.get(0).equals("camel:jackson") && deps.size() > 1) {
                    cleanName = deps.get(1).replace("camel:", "");
                } else {
                    cleanName = deps.get(0).replace("camel:", "");
                }
                if (cleanName.equalsIgnoreCase("cassandraql")) {
                    cleanName = "cql";
                }
                if (cleanName.equalsIgnoreCase("aws2-ddb") && name.equals("aws-ddb-streams-source")) {
                    cleanName = "aws2-ddb-streams";
                }
                if (p != null && !p.isEmpty()) {
                    ComponentModel componentModel = cc.componentModel(cleanName);
                    if (componentModel != null) {
                        List<ComponentModel.EndpointOptionModel> ce = componentModel.getEndpointOptions();
                        List<String> ceInternal =
                                ce.stream()
                                        .map(ComponentModel.EndpointOptionModel::getName)
                                        .collect(Collectors.toList());
                    for (Map.Entry<String, Object> entry : p.entrySet()) {
                            if (!entry.getKey().equals("period") && (!name.equals("kafka-ssl-source") && !name.equals("google-sheets-source") && !name.equals("google-mail-source") && !name.equals("google-calendar-source") && !name.equals("timer-source") && !name.equals("cron-source") && !name.equals("fhir-source"))) {
                                assertTrue(ceInternal.contains(entry.getKey()));
                            }
                        }
                    }
                }
            }
        }
    }
}
