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
package org.apache.camel.kamelets.maven.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateIndexMojoTest {

    @TempDir
    Path dir;

    @Test
    void theAttributesOfEachKameletSortedByName() throws Exception {
        Files.writeString(dir.resolve("zeta-source.kamelet.yaml"), """
                apiVersion: camel.apache.org/v1
                kind: Kamelet
                metadata:
                  name: zeta-source
                  annotations:
                    camel.apache.org/kamelet.support.level: "Preview"
                    camel.apache.org/kamelet.icon: "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="
                    camel.apache.org/provider: "Apache Software Foundation"
                    camel.apache.org/kamelet.group: "Zeta"
                    camel.apache.org/kamelet.namespace: "Misc"
                  labels:
                    camel.apache.org/kamelet.type: "source"
                spec:
                  definition:
                    title: "Zeta Source"
                    description: |-
                      Receive from Zeta.

                      On two lines.
                """);
        Files.writeString(dir.resolve("alpha-sink.kamelet.yaml"), """
                apiVersion: camel.apache.org/v1
                kind: Kamelet
                metadata:
                  name: alpha-sink
                  labels:
                    camel.apache.org/kamelet.type: "sink"
                spec:
                  definition:
                    title: "Alpha Sink"
                """);
        Files.writeString(dir.resolve("README.md"), "not a Kamelet");

        List<Map<String, Object>> index = GenerateIndexMojo.index(dir);

        assertEquals(2, index.size());
        // without the attributes it does not have
        assertEquals(Map.of("name", "alpha-sink", "type", "sink", "title", "Alpha Sink"), index.get(0));
        Map<String, Object> zeta = index.get(1);
        assertEquals(List.of("name", "type", "supportLevel", "title", "description", "group", "namespace", "provider"),
                List.copyOf(zeta.keySet()));
        assertEquals("Preview", zeta.get("supportLevel"));
        assertEquals("Receive from Zeta.\n\nOn two lines.", zeta.get("description"));
        // no icon
        assertFalse(GenerateIndexMojo.toJson(index).contains("base64"));
        assertTrue(GenerateIndexMojo.toJson(index).startsWith("{\n  \"kamelets\" : [ {\n    \"name\" : \"alpha-sink\""));
    }

    @Test
    void aKameletWithoutName() throws Exception {
        Files.writeString(dir.resolve("nameless.kamelet.yaml"), "metadata:\n  labels: {}\n");

        assertThrows(MojoExecutionException.class, () -> GenerateIndexMojo.index(dir));
    }

    @Test
    void theKameletsOfThisProject() throws Exception {
        Path kamelets = Paths.get("../../kamelets");
        long files;
        try (Stream<Path> list = Files.list(kamelets)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(".kamelet.yaml")).count();
        }

        List<Map<String, Object>> index = GenerateIndexMojo.index(kamelets);

        assertEquals(files, index.size());
        for (Map<String, Object> kamelet : index) {
            // what a list of the Kamelets shows
            for (String key : List.of("type", "supportLevel", "title", "description")) {
                assertTrue(kamelet.containsKey(key), kamelet.get("name") + " has no " + key);
            }
        }
    }
}
