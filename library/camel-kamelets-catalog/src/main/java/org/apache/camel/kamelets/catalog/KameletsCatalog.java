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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.camelk.v1alpha1.Kamelet;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.camel.kamelets.catalog.model.KameletLabelNames;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KameletsCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(KameletsCatalog.class);
    private static final String KAMELETS_DIR = "kamelets";
    private static final String KAMELETS_FILE_SUFFIX = ".kamelet.yaml";
    private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private Map<String, Kamelet> kameletModels = new HashMap<>();
    private List<String> kameletNames = new ArrayList<>();

    public KameletsCatalog() throws IOException {
        initCatalog();
        kameletNames = kameletModels.keySet().stream().sorted(Comparator.naturalOrder()).map(x -> x).collect(Collectors.toList());
    }

    private void initCatalog() throws IOException {
        List<String> resourceNames;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/" + KAMELETS_DIR + "/").scan()) {
            resourceNames = scanResult.getAllResources().getPaths();
        }
        for (String fileName:
                resourceNames) {
            Kamelet kamelet = mapper.readValue(KameletsCatalog.class.getResourceAsStream("/" + fileName), Kamelet.class);
            kameletModels.put(sanitizeFileName(fileName), kamelet);
        }
    }

    private String sanitizeFileName(String fileName) {
        int index = fileName.lastIndexOf(KAMELETS_FILE_SUFFIX);
        if (index > 0) {
            fileName = fileName.substring(0, index);
        }
        String finalName = fileName.substring(9);
        return finalName;
    }


    public Map<String, Kamelet> getKamelets() {
        return kameletModels;
    }

    public List<String> getKameletsName() {
        return kameletNames;
    }

    public List<Kamelet> getKameletsByName(String name) {
        List<Kamelet> collect = kameletModels.entrySet().stream()
                .filter(x -> x.getKey().contains(name))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return collect;
    }

    public List<Kamelet> getKameletsByType(String type) {
        List<Kamelet> collect = kameletModels.entrySet().stream()
                .filter(x -> x.getValue().getMetadata().getLabels().get(KameletLabelNames.KAMELET_LABEL_TYPE).contains(type))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return collect;
    }

    public JSONSchemaProps getKameletDefinition(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
                return kamelet.getSpec().getDefinition();
        } else {
            return null;
        }
    }

    public List<Kamelet> getKameletByProvider(String provider) {
        List<Kamelet> collect = kameletModels.entrySet().stream()
                .filter(x -> x.getValue().getMetadata().getAnnotations().get(KameletLabelNames.KAMELET_LABEL_PROVIDER).equalsIgnoreCase(provider))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            return collect;
        } else {
            return Collections.emptyList();
        }
    }

    public List<String> getKameletRequiredProperties(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getDefinition().getRequired();
        } else {
            return null;
        }
    }

    public List<String> getKameletDependencies(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getDependencies();
        } else {
            return null;
        }
    }

    public JsonNode getKameletFlow(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getFlow();
        } else {
            return null;
        }
    }
}
