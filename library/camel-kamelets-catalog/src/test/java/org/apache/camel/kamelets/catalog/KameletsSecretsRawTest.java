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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A secret (an option with format password) that goes into the address of an endpoint must be wrapped in RAW(), or a
 * secret with a character such as & or + is cut into another parameter, or changed, by the parsing of the address.
 */
public class KameletsSecretsRawTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    void secretsInEndpointAddressesAreRaw() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(KAMELETS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".kamelet.yaml")).sorted().toList()) {
                JsonNode spec = YAML.readTree(file.toFile()).path("spec");
                List<String> secrets = new ArrayList<>();
                for (Map.Entry<String, JsonNode> p : spec.path("definition").path("properties").properties()) {
                    if ("password".equals(p.getValue().path("format").asText())) {
                        secrets.add(p.getKey());
                    }
                }
                if (!secrets.isEmpty()) {
                    String kamelet = file.getFileName().toString().replace(".kamelet.yaml", "");
                    check(kamelet, spec.path("template"), secrets, problems);
                }
            }
        }

        assertEquals(List.of(), problems, "secrets in an endpoint address without RAW()");
    }

    private static void check(String kamelet, JsonNode node, List<String> secrets, List<String> problems) {
        if (node.isArray()) {
            node.forEach(n -> check(kamelet, n, secrets, problems));
        } else if (node.isObject()) {
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                String key = e.getKey();
                JsonNode value = e.getValue();
                if ("beans".equals(key)) {
                    // a secret set as a bean property is not parsed as part of an address
                    continue;
                }
                if (("uri".equals(key) || "to".equals(key) || "toD".equals(key)) && value.isTextual()) {
                    String uri = value.asText();
                    int query = uri.indexOf('?');
                    checkValue(kamelet, key, query < 0 ? uri : uri.substring(0, query), secrets, problems);
                    if (query >= 0) {
                        for (String param : uri.substring(query + 1).split("&")) {
                            int eq = param.indexOf('=');
                            checkValue(kamelet, param.substring(0, Math.max(eq, 0)), param.substring(eq + 1), secrets,
                                    problems);
                        }
                    }
                } else if ("parameters".equals(key) && value.isObject()) {
                    for (Map.Entry<String, JsonNode> p : value.properties()) {
                        checkValue(kamelet, p.getKey(), p.getValue().asText(), secrets, problems);
                    }
                } else {
                    check(kamelet, value, secrets, problems);
                }
            }
        }
    }

    private static void checkValue(String kamelet, String name, String value, List<String> secrets, List<String> problems) {
        for (String secret : secrets) {
            boolean uses = value.contains("{{" + secret + "}}") || value.contains("{{?" + secret + "}}");
            if (uses && !(value.startsWith("RAW(") && value.endsWith(")"))) {
                problems.add(kamelet + ": " + name + "=" + value);
            }
        }
    }
}
