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

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sinks and actions that call a component remove the headers of that component from the message first, except
 * the ones they document as input, so a header that comes with the message cannot change what the Kamelet does.
 */
public class KameletsRemoveHeadersTest {

    // the source files, not the catalog jar
    private static final Path KAMELETS = Path.of("../../kamelets");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static CamelContext context;

    @BeforeAll
    static void start() {
        context = new DefaultCamelContext();
    }

    @AfterAll
    static void stop() throws Exception {
        context.close();
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/remove-headers/kamelets.csv", nullValues = "")
    void removesComponentHeadersFirst(String kamelet, String pattern, String excludePattern, String removed, String kept)
            throws Exception {
        JsonNode spec = YAML.readTree(KAMELETS.resolve(kamelet + ".kamelet.yaml").toFile()).path("spec");
        JsonNode first = spec.path("template").path("from").path("steps").path(0).path("removeHeaders");

        assertEquals(pattern, first.path("pattern").asText(null), "the first step removes the headers");
        assertEquals(excludePattern, first.path("excludePattern").asText(null), "the headers it keeps");

        // the step removes the header the same way at runtime
        Message message = new DefaultMessage(context);
        message.setHeader(removed, "from the message");
        message.setHeader("file", "kept");
        String[] documented = kept == null ? new String[0] : kept.split("\\|");
        for (String header : documented) {
            message.setHeader(header, "documented input");
        }
        if (excludePattern == null) {
            message.removeHeaders(pattern);
        } else {
            message.removeHeaders(pattern, excludePattern);
        }

        assertFalse(message.getHeaders().containsKey(removed), removed + " is removed");
        assertTrue(message.getHeaders().containsKey("file"), "headers that do not start with Camel are kept");
        for (String header : documented) {
            assertTrue(message.getHeaders().containsKey(header), header + " is documented as input and kept");
        }
    }
}
