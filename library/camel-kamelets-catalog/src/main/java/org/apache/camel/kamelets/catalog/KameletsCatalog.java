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
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.camelk.v1alpha1.Kamelet;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.apache.camel.kamelets.catalog.model.KameletAnnotationsNames;
import org.apache.camel.kamelets.catalog.model.KameletLabelNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KameletsCatalog {

    static final String KAMELETS_DIR = "kamelets";
    private static final Logger LOG = LoggerFactory.getLogger(KameletsCatalog.class);
    private static final String KAMELETS_FILE_SUFFIX = ".kamelet.yaml";
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Map<String, Kamelet> kameletModels;
    private final List<String> kameletNames;

    public KameletsCatalog() {
        kameletModels = initCatalog();
        kameletNames = kameletModels.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static Map<String, Kamelet> initCatalog() {
        Map<String, Kamelet> kameletModels = new HashMap<>();

        try (ScanResult scanResult = new ClassGraph().acceptPaths("/" + KAMELETS_DIR + "/").scan()) {
            for (Resource resource : scanResult.getAllResources()) {

                try (InputStream is = resource.open()) {
                    String name = sanitizeFileName(resource.getPath());
                    Kamelet kamelet = MAPPER.readValue(is, Kamelet.class);

                    LOG.debug("Loading kamelet from: {}, path: {}, name: {}",
                        resource.getClasspathElementFile(),
                        resource.getPath(),
                        name);

                    kameletModels.put(name, kamelet);
                } catch (IOException e) {
                    LOG.warn("Cannot init Kamelet Catalog with content of " + resource.getPath(), e);
                }
            }
        }

        return Collections.unmodifiableMap(kameletModels);
    }

    private static String sanitizeFileName(String fileName) {
        int index = fileName.lastIndexOf(KAMELETS_FILE_SUFFIX);
        if (index > 0) {
            fileName = fileName.substring(0, index);
        }
        return fileName.substring(9);
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

    public List<Kamelet> getKameletsByGroups(String group) {
        List<Kamelet> collect = kameletModels.entrySet().stream()
                .filter(x -> x.getValue().getMetadata().getAnnotations().get(KameletAnnotationsNames.KAMELET_ANNOTATION_GROUP).contains(group))
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
                .filter(x -> x.getValue().getMetadata().getAnnotations().get(KameletAnnotationsNames.KAMELET_ANNOTATION_PROVIDER).equalsIgnoreCase(provider))
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

    public void getAllKameletDependencies() {
        Map<String, Kamelet> treeMap = new TreeMap<>(kameletModels);
        for (Map.Entry<String, Kamelet> entry: treeMap.entrySet()) {
            StringBuilder builder = new StringBuilder();
            for (String dep: entry.getValue().getSpec().getDependencies()) {
                builder.append(dep + System.lineSeparator());
            }
            System.out.println(entry.getKey());
            System.out.println("---------------------------------------------------------------------------------------------------");
            System.out.println(builder.toString());
            builder.append(System.lineSeparator());
        }
    }

    public Map<String, Object> getKameletTemplate(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getTemplate();
        } else {
            return null;
        }
    }
}
