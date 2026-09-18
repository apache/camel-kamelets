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
 * Every Kamelet is a component in its own right, linked to the artifacts it
 * declares through the CycloneDX dependency graph, so a scanner can attribute a
 * finding to the Kamelets it actually affects. Kamelet components carry no purl:
 * they are not Maven artifacts and should not be looked up as such.
 *
 * Both kinds of dependency are listed, but they are marked differently.
 * "mvn:" coordinates are pinned by the catalog, so an advisory against one is
 * fixed here. "camel:<component>" artifacts are versioned by the runtime, not by
 * the catalog; they carry the version this catalog builds against and a property
 * saying so, because an advisory against one is fixed by moving Camel.
 */
@Mojo(name = "generate-catalog-sbom", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class GenerateCatalogSbomMojo extends AbstractMojo {

    private static final String MVN_PREFIX = "mvn:";
    private static final String CAMEL_PREFIX = "camel:";
    private static final String VERSIONED_BY = "camel.apache.org/versioned-by";
    private static final String KAMELET_REF_PREFIX = "kamelet:";
    private static final String ROOT_REF = "camel-kamelets-catalog";

    @Parameter(property = "kamelets.dir", defaultValue = "${project.basedir}/../../kamelets")
    private File kameletsDir;

    @Parameter(property = "kamelets.sbom.dir", defaultValue = "${project.basedir}/../../camel-kamelets-sbom")
    private File outputDir;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String projectVersion;

    @Parameter(defaultValue = "${camel.version}", readonly = true)
    private String camelVersion;

    @Override
    public void execute() throws MojoExecutionException {
        List<CatalogValidator.KameletInfo> kamelets;
        try {
            kamelets = CatalogValidator.listKamelets(kameletsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read the Kamelets in " + kameletsDir, e);
        }

        // artifact ref -> component, and kamelet -> the refs it declares; both
        // sorted so the document only changes when the catalog does
        Map<String, Map<String, Object>> artifacts = new TreeMap<>();
        Map<String, TreeSet<String>> declares = new TreeMap<>();

        for (CatalogValidator.KameletInfo k : kamelets) {
            TreeSet<String> refs = declares.computeIfAbsent(k.name, n -> new TreeSet<>());
            for (String dep : k.dependencies()) {
                Map<String, Object> component = null;
                if (dep.startsWith(MVN_PREFIX)) {
                    component = component(dep.substring(MVN_PREFIX.length()));
                } else if (dep.startsWith(CAMEL_PREFIX)) {
                    component = camelComponent(dep.substring(CAMEL_PREFIX.length()));
                }
                if (component != null) {
                    String ref = (String) component.get("bom-ref");
                    artifacts.putIfAbsent(ref, component);
                    refs.add(ref);
                }
            }
        }

        List<Map<String, Object>> components = new ArrayList<>();
        List<Map<String, Object>> dependencies = new ArrayList<>();

        TreeSet<String> kameletRefs = new TreeSet<>();
        for (Map.Entry<String, TreeSet<String>> e : declares.entrySet()) {
            String ref = KAMELET_REF_PREFIX + e.getKey();
            kameletRefs.add(ref);
            components.add(kameletComponent(e.getKey(), ref));
            dependencies.add(dependency(ref, e.getValue()));
        }
        components.addAll(artifacts.values());

        // the catalog itself depends on every Kamelet; the artifacts end the graph
        dependencies.add(0, dependency(ROOT_REF, kameletRefs));
        for (String ref : artifacts.keySet()) {
            dependencies.add(dependency(ref, new TreeSet<>()));
        }

        long pinnedCount = artifacts.values().stream().filter(c -> !"org.apache.camel".equals(c.get("group"))).count();

        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("bomFormat", "CycloneDX");
        bom.put("specVersion", "1.6");
        bom.put("version", 1);
        bom.put("metadata", metadata());
        bom.put("components", components);
        bom.put("dependencies", dependencies);

        Path dest = outputDir.toPath().resolve("camel-kamelets-catalog-sbom.json");
        try {
            Files.createDirectories(dest.getParent());
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Files.writeString(dest, mapper.writeValueAsString(bom) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write " + dest, e);
        }
        getLog().info(String.format("\"%s\" written (%d kamelets, %d pinned artifacts, %d camel components)",
                dest, kamelets.size(), pinnedCount, artifacts.size() - pinnedCount));
    }

    /**
     * No timestamp and no serial number on purpose: the file is committed, so it
     * should only change when the pinned dependencies change, not on every build.
     */
    private Map<String, Object> metadata() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("bom-ref", ROOT_REF);
        root.put("type", "library");
        root.put("group", "org.apache.camel.kamelets");
        root.put("name", "camel-kamelets");
        root.put("version", projectVersion);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("component", root);
        return metadata;
    }

    private Map<String, Object> component(String coordinate) throws MojoExecutionException {
        String[] parts = coordinate.split(":");
        if (parts.length < 3) {
            throw new MojoExecutionException("Cannot parse dependency coordinate \"mvn:" + coordinate + "\"");
        }
        return artifact(parts[0], parts[1], parts[2], null);
    }

    /**
     * A Camel artifact, carried at the version this catalog builds against. That
     * version is not a catalog decision, so it is labelled: a scanner should treat
     * it as "the Camel this catalog targets" rather than something fixable here.
     */
    private Map<String, Object> camelComponent(String component) {
        return artifact("org.apache.camel", "camel-" + component, camelVersion, "runtime");
    }

    /** A Maven artifact, keyed in the graph by its purl. */
    private Map<String, Object> artifact(String group, String name, String version, String versionedBy) {
        String purl = purl(group, name, version);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bom-ref", purl);
        out.put("type", "library");
        out.put("group", group);
        out.put("name", name);
        out.put("version", version);
        out.put("purl", purl);
        if (versionedBy != null) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("name", VERSIONED_BY);
            property.put("value", versionedBy);
            out.put("properties", List.of(property));
        }
        return out;
    }

    /**
     * A Kamelet, deliberately without a purl: it is a route template shipped inside
     * the catalog jar, not an artifact a scanner can resolve on its own.
     */
    private Map<String, Object> kameletComponent(String name, String ref) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bom-ref", ref);
        out.put("type", "library");
        out.put("name", name);
        out.put("version", projectVersion);
        return out;
    }

    private Map<String, Object> dependency(String ref, TreeSet<String> dependsOn) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ref", ref);
        out.put("dependsOn", new ArrayList<>(dependsOn));
        return out;
    }

    private static String purl(String group, String artifact, String version) {
        return "pkg:maven/" + enc(group) + "/" + enc(artifact) + "@" + enc(version);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
