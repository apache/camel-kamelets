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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates the index of the Kamelets, {@value #INDEX} in the camel-kamelets JAR: for each Kamelet, sorted by name, its
 * name, type, support level, title, summary, description, group, namespace and provider (an attribute that a Kamelet
 * does not have is left out). The summary is the first paragraph of the description, see
 * {@link KameletsCatalog#summary(String)}: a list of the Kamelets shows the summary, and a search looks in the whole
 * description. A tool that lists the Kamelets, such as the Camel CLI, reads this small file instead of the YAML of
 * every Kamelet (2.3 MB with their icons), which takes hundreds of milliseconds to parse.
 * <p>
 * The index is written into the resources of camel-kamelets and committed with the Kamelets, like the copy of the
 * Kamelets there and nav.adoc: the build of main regenerates it.
 * <p>
 * The format:
 *
 * <pre>
 * {
 *   "kamelets" : [ {
 *     "name" : "aws-s3-sink",
 *     "type" : "sink",
 *     "supportLevel" : "Stable",
 *     "title" : "AWS S3 Sink",
 *     "summary" : "Upload data to an Amazon S3 Bucket.",
 *     "description" : "Upload data to an Amazon S3 Bucket.\n\n...",
 *     "group" : "AWS S3",
 *     "namespace" : "AWS",
 *     "provider" : "Apache Software Foundation"
 *   }, ... ]
 * }
 * </pre>
 */
@Mojo(name = "generate-index", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class GenerateIndexMojo extends AbstractMojo {

    /**
     * The index in the camel-kamelets JAR.
     */
    public static final String INDEX = "META-INF/kamelets-index.json";

    private static final String SUFFIX = ".kamelet.yaml";

    /**
     * Directory holding the *.kamelet.yaml files.
     */
    @Parameter(property = "kamelets.dir", defaultValue = "${project.basedir}/../../kamelets")
    private File kameletsDir;

    /**
     * The resources of camel-kamelets, which the index is written into.
     */
    @Parameter(property = "kamelets.resources.dir",
               defaultValue = "${project.basedir}/../camel-kamelets/src/main/resources")
    private File resourcesDir;

    @Override
    public void execute() throws MojoExecutionException {
        Path dest = resourcesDir.toPath().resolve(INDEX);
        try {
            List<Map<String, Object>> kamelets = index(kameletsDir.toPath());
            String content = toJson(kamelets);
            Files.createDirectories(dest.getParent());
            if (Files.isRegularFile(dest) && content.equals(Files.readString(dest, StandardCharsets.UTF_8))) {
                getLog().info(String.format("\"%s\" is up to date (%d kamelets)", dest, kamelets.size()));
                return;
            }
            Files.writeString(dest, content, StandardCharsets.UTF_8);
            getLog().info(String.format("\"%s\" written (%d kamelets)", dest, kamelets.size()));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write file " + dest, e);
        }
    }

    /**
     * The entries of the index of the Kamelets of the directory, sorted by name.
     */
    static List<Map<String, Object>> index(Path dir) throws IOException, MojoExecutionException {
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(SUFFIX) && Files.isRegularFile(p)).toList();
        }
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        List<Map<String, Object>> answer = new ArrayList<>(files.size());
        for (Path file : files) {
            answer.add(entry(file, yaml.readValue(file.toFile(), Map.class)));
        }
        answer.sort(Comparator.comparing(e -> (String) e.get("name")));
        return answer;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entry(Path file, Map<String, Object> kamelet) throws MojoExecutionException {
        Map<String, Object> metadata = map(kamelet.get("metadata"));
        Object name = metadata.get("name");
        if (name == null) {
            throw new MojoExecutionException("No metadata.name in " + file);
        }
        Map<String, Object> labels = map(metadata.get("labels"));
        Map<String, Object> annotations = map(metadata.get("annotations"));
        Map<String, Object> definition = map(map(kamelet.get("spec")).get("definition"));

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        put(entry, "type", labels.get("camel.apache.org/kamelet.type"));
        put(entry, "supportLevel", annotations.get("camel.apache.org/kamelet.support.level"));
        put(entry, "title", definition.get("title"));
        Object description = definition.get("description");
        put(entry, "summary", description != null ? KameletsCatalog.summary(description.toString()) : null);
        put(entry, "description", description);
        put(entry, "group", annotations.get("camel.apache.org/kamelet.group"));
        put(entry, "namespace", annotations.get("camel.apache.org/kamelet.namespace"));
        put(entry, "provider", annotations.get("camel.apache.org/provider"));
        return entry;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static void put(Map<String, Object> entry, String key, Object value) {
        if (value != null) {
            entry.put(key, value.toString());
        }
    }

    static String toJson(List<Map<String, Object>> kamelets) throws IOException {
        ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        return json.writeValueAsString(Map.of("kamelets", kamelets)) + "\n";
    }
}
