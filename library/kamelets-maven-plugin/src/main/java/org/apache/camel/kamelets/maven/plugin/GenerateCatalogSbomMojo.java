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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Writes a CycloneDX SBOM describing the third party artifacts the catalog itself
 * pins, taken from the "mvn:group:artifact:version" entries in spec.dependencies.
 *
 * This is deliberately not the same document as camel-kamelets-sbom, which
 * cyclonedx-maven-plugin aggregates from the Maven reactor and which therefore
 * describes what builds the catalog rather than what a Kamelet pulls in at runtime.
 *
 * "camel:<component>" dependencies are left out on purpose: the catalog does not
 * choose their version, the runtime does, so pinning one here would attribute
 * advisories to the catalog that belong to whichever Camel release is in use.
 */
@Mojo(name = "generate-catalog-sbom", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class GenerateCatalogSbomMojo extends AbstractMojo {

    private static final String MVN_PREFIX = "mvn:";
    private static final String DECLARED_BY = "camel.apache.org/declared-by";

    @Parameter(property = "kamelets.dir", defaultValue = "${project.basedir}/../../kamelets")
    private File kameletsDir;

    @Parameter(property = "kamelets.sbom.dir", defaultValue = "${project.basedir}/../../camel-kamelets-sbom")
    private File outputDir;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String projectVersion;

    @Override
    public void execute() throws MojoExecutionException {
        List<CatalogValidator.KameletInfo> kamelets;
        try {
            kamelets = CatalogValidator.listKamelets(kameletsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read the Kamelets in " + kameletsDir, e);
        }

        // coordinate -> kamelets declaring it, both sorted so the file is stable
        Map<String, TreeSet<String>> pinned = new TreeMap<>();
        for (CatalogValidator.KameletInfo k : kamelets) {
            for (String dep : k.dependencies()) {
                if (dep.startsWith(MVN_PREFIX)) {
                    pinned.computeIfAbsent(dep.substring(MVN_PREFIX.length()), c -> new TreeSet<>()).add(k.name);
                }
            }
        }

        List<Map<String, Object>> components = new ArrayList<>();
        for (Map.Entry<String, TreeSet<String>> e : pinned.entrySet()) {
            components.add(component(e.getKey(), e.getValue()));
        }

        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("bomFormat", "CycloneDX");
        bom.put("specVersion", "1.6");
        bom.put("version", 1);
        bom.put("metadata", metadata());
        bom.put("components", components);

        Path dest = outputDir.toPath().resolve("camel-kamelets-catalog-sbom.json");
        try {
            Files.createDirectories(dest.getParent());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Files.writeString(dest, mapper.writeValueAsString(bom) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write " + dest, e);
        }
        getLog().info(String.format("\"%s\" written (%d pinned artifacts from %d kamelets)",
                dest, components.size(), kamelets.size()));
    }

    /**
     * No timestamp and no serial number on purpose: the file is committed, so it
     * should only change when the pinned dependencies change, not on every build.
     */
    private Map<String, Object> metadata() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "library");
        root.put("group", "org.apache.camel.kamelets");
        root.put("name", "camel-kamelets");
        root.put("version", projectVersion);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("component", root);
        return metadata;
    }

    private Map<String, Object> component(String coordinate, TreeSet<String> declaredBy) throws MojoExecutionException {
        String[] parts = coordinate.split(":");
        if (parts.length < 3) {
            throw new MojoExecutionException("Cannot parse dependency coordinate \"mvn:" + coordinate + "\"");
        }
        String group = parts[0];
        String artifact = parts[1];
        String version = parts[2];

        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", DECLARED_BY);
        property.put("value", String.join(",", declaredBy));

        Map<String, Object> component = new LinkedHashMap<>();
        component.put("type", "library");
        component.put("group", group);
        component.put("name", artifact);
        component.put("version", version);
        component.put("purl", purl(group, artifact, version));
        component.put("properties", List.of(property));
        return component;
    }

    private static String purl(String group, String artifact, String version) {
        return "pkg:maven/" + enc(group) + "/" + enc(artifact) + "@" + enc(version);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
