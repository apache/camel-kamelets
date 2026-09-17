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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Catalog-wide conventions for the Kamelet YAML files.
 *
 * Ported from the Go validator that used to live in script/validator. The checks,
 * their order and their messages are kept as they were, so the same catalog
 * produces the same output.
 */
public final class CatalogValidator {

    static final String TYPE_LABEL = "camel.apache.org/kamelet.type";
    static final String ICON_ANNOTATION = "camel.apache.org/kamelet.icon";
    static final String VERSION_ANNOTATION = "camel.apache.org/catalog.version";
    static final String PROVIDER_ANNOTATION = "camel.apache.org/provider";
    static final String EXPECTED_PROVIDER = "Apache Software Foundation";
    static final String CREDENTIALS_DESCRIPTOR = "urn:camel:group:credentials";
    static final String DEPRECATED_DESCRIPTOR_PREFIX = "urn:alm:descriptor:com.tectonic.ui";

    private static final Pattern PARAM = Pattern.compile("\\{\\{[?]?([A-Za-z0-9\\-._]+)(?:[:][^}]*)?}}");

    /**
     * The classic English stop word list. Words here are exempt from the
     * property-title capitalization rule.
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in",
            "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the",
            "their", "then", "there", "these", "they", "this", "to", "was", "will", "with");

    /**
     * Words the Go implementation also treated as stop words, through a list that was
     * far broader than the classic one. They are kept so that this port does not start
     * failing titles the catalog already contains, such as "Cache name" or
     * "Period between Polls". Tightening the rule and fixing those titles is a separate
     * change.
     */
    private static final Set<String> EXTRA_EXEMPT = Set.of(
            "name", "between", "from", "now", "about", "after", "before", "down", "during",
            "only", "over", "same", "some", "than", "too", "under", "up", "very", "when",
            "where", "which", "while", "who", "why", "you", "your");

    /** Kamelets whose declared and used parameters are deliberately allowed to differ. */
    private static final Set<String> USED_PARAMS_EXEMPT = Set.of(
            "azure-storage-blob-source",
            "aws-s3-event-based-source",
            "ceph-event-based-source",
            "aws-sqs-source",
            "set-kafka-key-action",
            "azure-storage-blob-event-based-source",
            "google-storage-event-based-source",
            "elasticsearch-search-source",
            "opensearch-search-source",
            "kafka-azure-schema-registry-source",
            "kafka-azure-schema-registry-sink",
            "kafka-batch-azure-schema-registry-source",
            "cassandra-sink",
            "data-type-action");

    private CatalogValidator() {
    }

    /** One parsed Kamelet plus the file it came from. */
    static final class KameletInfo {
        final String name;
        final File file;
        final Map<String, Object> doc;

        KameletInfo(String name, File file, Map<String, Object> doc) {
            this.name = name;
            this.file = file;
            this.doc = doc;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map(Map<String, Object> parent, String key) {
            if (parent == null) {
                return null;
            }
            Object v = parent.get(key);
            return v instanceof Map ? (Map<String, Object>) v : null;
        }

        Map<String, Object> metadata() {
            return map(doc, "metadata");
        }

        Map<String, Object> spec() {
            return map(doc, "spec");
        }

        Map<String, Object> definition() {
            return map(spec(), "definition");
        }

        Map<String, Object> template() {
            return map(spec(), "template");
        }

        Map<String, Object> properties() {
            Map<String, Object> def = definition();
            Map<String, Object> p = map(def, "properties");
            return p != null ? p : new LinkedHashMap<>();
        }

        String label(String key) {
            Map<String, Object> labels = map(metadata(), "labels");
            Object v = labels != null ? labels.get(key) : null;
            return v != null ? v.toString() : "";
        }

        String annotation(String key) {
            Map<String, Object> ann = map(metadata(), "annotations");
            Object v = ann != null ? ann.get(key) : null;
            return v != null ? v.toString() : "";
        }

        @SuppressWarnings("unchecked")
        List<String> required() {
            Object v = definition() != null ? definition().get("required") : null;
            List<String> out = new ArrayList<>();
            if (v instanceof List) {
                for (Object o : (List<Object>) v) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }

        @SuppressWarnings("unchecked")
        List<String> dependencies() {
            Object v = spec() != null ? spec().get("dependencies") : null;
            List<String> out = new ArrayList<>();
            if (v instanceof List) {
                for (Object o : (List<Object>) v) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
    }

    /** Reads every *.kamelet.yaml in the directory, ordered by file path. */
    static List<KameletInfo> listKamelets(File dir) throws IOException {
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".kamelet.yaml"));
        if (files == null) {
            throw new IOException("cannot list dir \"" + dir + "\"");
        }
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        sorted.sort(Comparator.comparing(File::getPath));

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<KameletInfo> out = new ArrayList<>(sorted.size());
        for (File f : sorted) {
            Map<String, Object> doc;
            try {
                doc = mapper.readValue(f, Map.class);
            } catch (IOException e) {
                throw new IOException("kamelet \"" + f.getName() + "\" is not a valid YAML file", e);
            }
            Object md = doc.get("metadata");
            Object name = md instanceof Map ? ((Map<?, ?>) md).get("name") : null;
            out.add(new KameletInfo(name != null ? name.toString() : "", f, doc));
        }
        return out;
    }

    /**
     * Runs every check in the order the Go validator used: the structural ones first,
     * because the rest assume they passed.
     */
    static List<String> validate(List<KameletInfo> kamelets) {
        List<String> errors = new ArrayList<>();
        verifyFileNames(kamelets, errors);
        verifyKameletType(kamelets, errors);
        verifyParameters(kamelets, errors);
        if (!errors.isEmpty()) {
            return errors;
        }
        verifyAnnotations(kamelets, errors);
        verifyDescriptors(kamelets, errors);
        verifyDuplicates(kamelets, errors);
        verifyUsedParams(kamelets, errors);
        verifyTemplateConvention(kamelets, errors);
        verifyDependencies(kamelets, errors);
        verifyCatalogVersion(kamelets, errors);
        return errors;
    }

    private static String q(Object o) {
        return "\"" + o + "\"";
    }

    static void verifyFileNames(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            String expected = k.file.getName().replaceAll("\\.kamelet\\.yaml$", "");
            if (!k.name.equals(expected)) {
                errors.add("file " + q(k.file.getPath()) + " does not match the name of the contained kamelet: " + q(k.name));
            }
        }
    }

    static void verifyKameletType(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            String tp = k.label(TYPE_LABEL);
            if ("source".equals(tp) || "sink".equals(tp) || "action".equals(tp)) {
                String suffix = "-" + tp;
                if (!k.name.endsWith(suffix)) {
                    errors.add("name of kamelet " + q(k.name) + " does not end with " + q(suffix));
                }
            } else {
                errors.add("kamelet " + q(k.name) + " contains an invalid value for the " + TYPE_LABEL + " label: " + q(tp));
            }
        }
    }

    static void verifyParameters(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            if (k.definition() == null) {
                errors.add("kamelet " + q(k.name) + " does not contain the JSON schema definition");
                continue;
            }
            if (k.template() == null) {
                errors.add("kamelet " + q(k.name) + " does not contain the Template specification");
                continue;
            }
            Set<String> seenRequired = new HashSet<>();
            for (String p : k.required()) {
                if (!seenRequired.add(p)) {
                    errors.add("required kamelet property " + q(p) + " is listed twice in kamelet " + q(k.name));
                }
            }
            Object titleObj = k.definition().get("title");
            String title = titleObj != null ? titleObj.toString() : "";
            if (title.isEmpty()) {
                errors.add("kamelet " + q(k.name) + " does not contain title");
            } else {
                String tp = k.label(TYPE_LABEL);
                if (tp.length() > 1) {
                    String expectedSuffix = tp.substring(0, 1).toUpperCase() + tp.substring(1);
                    if (!title.endsWith(expectedSuffix)) {
                        errors.add("kamelet " + q(k.name) + " title " + q(title) + " does not ends with " + q(expectedSuffix));
                    }
                }
            }
            Object descObj = k.definition().get("description");
            if (descObj == null || descObj.toString().isEmpty()) {
                errors.add("kamelet " + q(k.name) + " does not contain description");
            }
            if (!"object".equals(String.valueOf(k.definition().get("type")))) {
                errors.add("kamelet " + q(k.name) + " does not contain a definition of type \"object\"");
            }
            for (Map.Entry<String, Object> e : k.properties().entrySet()) {
                verifyProperty(k, e.getKey(), asMap(e.getValue()), errors);
            }
            Map<String, Object> props = k.properties();
            for (String r : k.required()) {
                if (!props.containsKey(r)) {
                    errors.add("required property " + q(r) + " in kamelet " + q(k.name) + " is not defined");
                }
            }
        }
    }

    private static void verifyProperty(KameletInfo k, String key, Map<String, Object> p, List<String> errors) {
        if (p == null) {
            return;
        }
        String type = str(p.get("type"));
        String title = str(p.get("title"));
        String description = str(p.get("description"));

        if (type.isEmpty()) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " does not contain type");
        }
        if (title.isEmpty()) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " does not contain title");
        } else {
            for (String w : title.split(" ")) {
                if (w.isEmpty() || isExempt(w)) {
                    continue;
                }
                String first = w.substring(0, 1);
                if (!first.toUpperCase().equals(first)) {
                    errors.add("property " + q(key) + " in kamelet " + q(k.name)
                            + " does has non-capitalized word in the title: " + q(w));
                }
            }
        }
        if (title.startsWith("The ")) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " has a title starting with \"The \"");
        }
        if (title.startsWith("A ")) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " has a title starting with \"A \"");
        }
        if (title.startsWith("An ")) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " has a title starting with \"An \"");
        }
        if (description.isEmpty()) {
            errors.add("property " + q(key) + " in kamelet " + q(k.name) + " does not contain a description");
        }
    }

    /**
     * A title word is exempt from capitalization when it is a stop word, or when it
     * carries trailing punctuation: the Go implementation compared the raw word against
     * a punctuation-stripped list, so "checks." never matched and was skipped.
     */
    private static boolean isExempt(String word) {
        String lower = word.toLowerCase();
        if (STOP_WORDS.contains(lower) || EXTRA_EXEMPT.contains(lower)) {
            return true;
        }
        return !lower.equals(stripPunctuation(lower));
    }

    private static String stripPunctuation(String w) {
        return w.replaceAll("[^\\p{Alnum}]", "");
    }

    static void verifyAnnotations(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            if (k.annotation(ICON_ANNOTATION).isEmpty()) {
                errors.add("kamelet " + q(k.name) + " does not contain the " + ICON_ANNOTATION + " annotation");
            }
            if (k.annotation(VERSION_ANNOTATION).isEmpty()) {
                errors.add("kamelet " + q(k.name) + " does not contain the " + VERSION_ANNOTATION
                        + " annotation (should match project version in pom.xml)");
            }
            String provider = k.annotation(PROVIDER_ANNOTATION);
            if (!EXPECTED_PROVIDER.equals(provider)) {
                errors.add("kamelet " + q(k.name) + " does not contain the right value for the " + PROVIDER_ANNOTATION
                        + " annotation: expected " + q(EXPECTED_PROVIDER) + ", found " + q(provider));
            }
        }
    }

    static void verifyDescriptors(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            if (k.definition() == null) {
                errors.add("kamelet " + q(k.name) + " does not contain the JSON schema definition");
                continue;
            }
            for (Map.Entry<String, Object> e : k.properties().entrySet()) {
                Map<String, Object> p = asMap(e.getValue());
                if (p == null) {
                    continue;
                }
                List<String> descriptors = strList(p.get("x-descriptors"));
                if ("password".equals(str(p.get("format"))) && !descriptors.contains(CREDENTIALS_DESCRIPTOR)) {
                    errors.add("property " + q(e.getKey()) + " in kamelet " + q(k.name)
                            + " has \"password\" format but misses descriptor " + q(CREDENTIALS_DESCRIPTOR));
                }
                boolean deprecated = descriptors.stream().anyMatch(d -> d.startsWith(DEPRECATED_DESCRIPTOR_PREFIX));
                if (deprecated) {
                    errors.add("property " + q(e.getKey()) + " in kamelet " + q(k.name)
                            + " uses the deprecated x-descriptor prefix " + q(DEPRECATED_DESCRIPTOR_PREFIX)
                            + "; use " + q(CREDENTIALS_DESCRIPTOR) + " instead");
                }
                Set<String> seen = new HashSet<>();
                for (String d : descriptors) {
                    if (!seen.add(d)) {
                        errors.add("property " + q(e.getKey()) + " in kamelet " + q(k.name)
                                + " lists the x-descriptor " + q(d) + " more than once");
                    }
                }
            }
        }
    }

    static void verifyDuplicates(List<KameletInfo> kamelets, List<String> errors) {
        Set<String> titles = new HashSet<>();
        Set<String> descriptions = new HashSet<>();
        for (KameletInfo k : kamelets) {
            if (k.definition() == null) {
                errors.add("kamelet " + q(k.name) + " does not contain the JSON schema definition");
                continue;
            }
            String title = str(k.definition().get("title"));
            if (!titles.add(title)) {
                errors.add("kamelet " + q(k.name) + " has duplicate title " + q(title));
            }
            String description = str(k.definition().get("description"));
            if (!descriptions.add(description)) {
                errors.add("kamelet " + q(k.name) + " has duplicate description " + q(description));
            }
        }
    }

    static void verifyTemplateConvention(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            if (k.template() == null) {
                continue;
            }
            String template = k.template().toString();
            String tp = k.label(TYPE_LABEL);
            if ("source".equals(tp) && !template.contains("kamelet:sink")) {
                errors.add("source kamelet " + q(k.name) + " must route to \"kamelet:sink\"");
            } else if ("sink".equals(tp) && !template.contains("kamelet:source")) {
                errors.add("sink kamelet " + q(k.name) + " must consume from \"kamelet:source\"");
            }
        }
    }

    static void verifyDependencies(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            for (String dep : k.dependencies()) {
                if (!dep.startsWith("camel:") && !dep.startsWith("mvn:") && !dep.startsWith("github:apache/")) {
                    errors.add("kamelet " + q(k.name) + " declares dependency " + q(dep)
                            + " that is not a \"camel:\", \"mvn:group:artifact:version\" or \"github:apache/...\" reference");
                }
            }
        }
    }

    static void verifyCatalogVersion(List<KameletInfo> kamelets, List<String> errors) {
        Map<String, Integer> versions = new HashMap<>();
        for (KameletInfo k : kamelets) {
            versions.merge(k.annotation(VERSION_ANNOTATION), 1, Integer::sum);
        }
        if (versions.size() > 1) {
            Set<String> summary = new TreeSet<>();
            for (Map.Entry<String, Integer> e : versions.entrySet()) {
                summary.add(q(e.getKey()) + " (" + e.getValue() + " kamelets)");
            }
            errors.add("inconsistent " + VERSION_ANNOTATION + " across the catalog: " + String.join(", ", summary));
        }
    }

    static void verifyUsedParams(List<KameletInfo> kamelets, List<String> errors) {
        for (KameletInfo k : kamelets) {
            if (USED_PARAMS_EXEMPT.contains(k.name)) {
                continue;
            }
            Set<String> used = new TreeSet<>();
            collectParams(k.template(), used);

            Set<String> declared = new TreeSet<>(k.properties().keySet());
            declared.addAll(beanNames(k));

            for (String p : used) {
                if (!declared.contains(p)) {
                    errors.add("parameter " + q(p) + " is not declared in the definition of kamelet " + q(k.name));
                }
            }
            for (String p : declared) {
                if (!used.contains(p)) {
                    errors.add("parameter " + q(p) + " is declared in kamelet " + q(k.name) + " but never used");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> beanNames(KameletInfo k) {
        Set<String> out = new HashSet<>();
        Map<String, Object> template = k.template();
        Object beans = template != null ? template.get("beans") : null;
        if (beans instanceof List) {
            for (Object b : (List<Object>) beans) {
                Map<String, Object> bm = asMap(b);
                if (bm != null && bm.get("name") != null) {
                    out.add(bm.get("name").toString());
                }
            }
        }
        return out;
    }

    /** Walks the template and collects every {{param}} placeholder it mentions. */
    @SuppressWarnings("unchecked")
    private static void collectParams(Object node, Set<String> params) {
        if (node instanceof String) {
            Matcher m = PARAM.matcher((String) node);
            while (m.find()) {
                params.add(m.group(1));
            }
        } else if (node instanceof List) {
            for (Object c : (List<Object>) node) {
                collectParams(c, params);
            }
        } else if (node instanceof Map) {
            for (Object c : ((Map<String, Object>) node).values()) {
                collectParams(c, params);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) {
            for (Object c : (List<Object>) o) {
                out.add(String.valueOf(c));
            }
        }
        return out;
    }
}
