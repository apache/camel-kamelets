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

package org.apache.camel.kamelets.utils.format.converter.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.camel.util.StringHelper;

public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
        // prevent instantiation of utility class
    }

    /**
     * Checks given value to be a Json array of object representation.
     * @param value
     * @return
     */
    public static boolean isJson(String value) {
        if (value == null) {
            return false;
        }

        return isJsonObject(value) || isJsonArray(value);
    }

    /**
     * Checks given value could be JSON object string.
     * @param value
     * @return
     */
    public static boolean isJsonObject(String value) {
        if (value == null || value.isEmpty() || value.isBlank()) {
            return false;
        }

        final String trimmed = value.trim();

        return trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}';
    }

    /**
     * Checks given value could be JSON array string.
     * @param value
     * @return
     */
    public static boolean isJsonArray(String value) {
        if (value == null || value.isEmpty() || value.isBlank()) {
            return false;
        }

        final String trimmed = value.trim();

        return trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']';
    }

    /**
     * Converts array json node to a list of json object strings. Used when splitting a
     * json array with split EIP.
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public static List<String> arrayToJsonBeans(JsonNode json) throws JsonProcessingException {
        List<String> jsonBeans = new ArrayList<>();

        if (json.isArray()) {
            Iterator<JsonNode> it = json.elements();
            while (it.hasNext()) {
                Object item = it.next();
                if (item instanceof TextNode) {
                    jsonBeans.add(StringHelper.removeLeadingAndEndingQuotes(((TextNode) item).asText()));
                } else {
                    jsonBeans.add(MAPPER.writeValueAsString(item));
                }
            }

            return jsonBeans;
        }

        return jsonBeans;
    }
}
