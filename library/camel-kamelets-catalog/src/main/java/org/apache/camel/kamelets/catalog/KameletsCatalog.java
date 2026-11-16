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
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.kamelets.catalog.model.KameletAnnotationsNames;
import org.apache.camel.kamelets.catalog.model.KameletLabelNames;
import org.apache.camel.kamelets.catalog.model.KameletPrefixSchemeEnum;
import org.apache.camel.kamelets.catalog.model.KameletTypeEnum;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.v1.Kamelet;
import org.apache.camel.v1.kameletspec.datatypes.Headers;
import org.apache.camel.v1.kameletspec.datatypes.Types;
import org.apache.camel.v1.kameletspec.DataTypes;
import org.apache.camel.v1.kameletspec.Definition;
import org.apache.camel.v1.kameletspec.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KameletsCatalog {

    static final String KAMELETS_DIR = "kamelets";
    private static final Logger LOG = LoggerFactory.getLogger(KameletsCatalog.class);
    private static final String KAMELETS_FILE_SUFFIX = ".kamelet.yaml";
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Map<String, Kamelet> kameletModels;
    private final List<String> kameletNames;
    // created when a method needs it
    private volatile DefaultCamelCatalog cc;

    public KameletsCatalog() {
        kameletModels = initCatalog();
        kameletNames = kameletModels.keySet().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static Map<String, Kamelet> initCatalog() {
        Map<String, Kamelet> kameletModels = new HashMap<>();
        try {
            if (loadFromKameletsDirectories(kameletModels)) {
                return Collections.unmodifiableMap(kameletModels);
            }
        } catch (IOException | URISyntaxException | RuntimeException e) {
            LOG.debug("Cannot list the Kamelets of the class path, scanning it instead", e);
        }
        kameletModels.clear();
        ClassGraphScan.load(kameletModels);
        return Collections.unmodifiableMap(kameletModels);
    }

    /**
     * Loads the Kamelets of the {@code kamelets} directories that the class loader has: the entries of each JAR under
     * {@code kamelets/}, or the files of each directory. Listing these takes a fraction of a scan of the whole class
     * path with ClassGraph (about 50 ms against 250 ms with the 340 JARs of the Camel CLI), which looks at every JAR.
     *
     * @return {@code false} when it found no Kamelets this way (such as a class loader whose JARs have no directory
     *         entries), so the class path is scanned instead
     */
    static boolean loadFromKameletsDirectories(Map<String, Kamelet> kameletModels)
            throws IOException, URISyntaxException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = KameletsCatalog.class.getClassLoader();
        }
        Enumeration<URL> urls = cl.getResources(KAMELETS_DIR + "/");
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if ("file".equals(url.getProtocol())) {
                Path dir = Paths.get(url.toURI());
                List<Path> files;
                try (Stream<Path> walk = Files.walk(dir)) {
                    files = walk.filter(p -> p.getFileName().toString().endsWith(KAMELETS_FILE_SUFFIX)).sorted()
                            .collect(Collectors.toList());
                }
                for (Path file : files) {
                    String path = KAMELETS_DIR + "/" + dir.relativize(file).toString().replace('\\', '/');
                    try (InputStream is = Files.newInputStream(file)) {
                        put(kameletModels, path, dir, is);
                    } catch (IOException e) {
                        LOG.warn("Cannot init Kamelet Catalog with content of " + path, e);
                    }
                }
            } else {
                URLConnection connection = url.openConnection();
                if (!(connection instanceof JarURLConnection)) {
                    // not a JAR or a directory: ClassGraph scans the class path
                    return false;
                }
                JarURLConnection jar = (JarURLConnection) connection;
                // a JarFile of its own, closed below
                jar.setUseCaches(false);
                try (JarFile file = jar.getJarFile()) {
                    Enumeration<JarEntry> entries = file.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String path = entry.getName();
                        if (path.startsWith(KAMELETS_DIR + "/") && path.endsWith(KAMELETS_FILE_SUFFIX)) {
                            try (InputStream is = file.getInputStream(entry)) {
                                put(kameletModels, path, file.getName(), is);
                            } catch (IOException e) {
                                LOG.warn("Cannot init Kamelet Catalog with content of " + path, e);
                            }
                        }
                    }
                }
            }
        }
        return !kameletModels.isEmpty();
    }

    private static void put(Map<String, Kamelet> kameletModels, String path, Object source, InputStream is)
            throws IOException {
        String name = sanitizeFileName(path);
        Kamelet kamelet = MAPPER.readValue(is, Kamelet.class);
        LOG.debug("Loading kamelet from: {}, path: {}, name: {}", source, path, name);
        kameletModels.put(name, kamelet);
    }

    /**
     * The scan of the class path with ClassGraph, when the class loader cannot list its {@code kamelets} directories. In
     * a class of its own, so the classes of ClassGraph are loaded only when it is used.
     */
    static final class ClassGraphScan {

        static void load(Map<String, Kamelet> kameletModels) {
            try (ScanResult scanResult = new ClassGraph().acceptPaths("/" + KAMELETS_DIR + "/").scan()) {
                for (Resource resource : scanResult.getAllResources()) {
                    try (InputStream is = resource.open()) {
                        put(kameletModels, resource.getPath(), resource.getClasspathElementFile(), is);
                    } catch (IOException e) {
                        LOG.warn("Cannot init Kamelet Catalog with content of " + resource.getPath(), e);
                    }
                }
            }
        }
    }

    private DefaultCamelCatalog camelCatalog() {
        DefaultCamelCatalog answer = cc;
        if (answer == null) {
            // two threads may each create one: the same content
            answer = new DefaultCamelCatalog();
            cc = answer;
        }
        return answer;
    }

    private static String sanitizeFileName(String fileName) {
        int index = fileName.lastIndexOf(KAMELETS_FILE_SUFFIX);
        if (index > 0) {
            fileName = fileName.substring(0, index);
        }
        return fileName.substring(9);
    }

    private static String labelValue(Kamelet kamelet, String key) {
        if (kamelet.getMetadata() != null && kamelet.getMetadata().getLabels() != null) {
            return kamelet.getMetadata().getLabels().get(key);
        }
        return null;
    }

    private static String annotationValue(Kamelet kamelet, String key) {
        if (kamelet.getMetadata() != null && kamelet.getMetadata().getAnnotations() != null) {
            return kamelet.getMetadata().getAnnotations().get(key);
        }
        return null;
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
        return kameletModels.entrySet().stream()
                .filter(x -> {
                    String value = labelValue(x.getValue(), KameletLabelNames.KAMELET_LABEL_TYPE);
                    return value != null && value.contains(type);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public List<Kamelet> getKameletsByNamespace(String namespace) {
        return kameletModels.entrySet().stream()
                .filter(x -> {
                    String value = annotationValue(x.getValue(), KameletAnnotationsNames.KAMELET_ANNOTATION_NAMESPACE);
                    return value != null && value.contains(namespace);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public List<Kamelet> getKameletsByGroups(String group) {
        return kameletModels.entrySet().stream()
                .filter(x -> {
                    String value = annotationValue(x.getValue(), KameletAnnotationsNames.KAMELET_ANNOTATION_GROUP);
                    return value != null && value.contains(group);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public Definition getKameletDefinition(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getDefinition();
        } else {
            return null;
        }
    }

    public List<Kamelet> getKameletByProvider(String provider) {
        return kameletModels.entrySet().stream()
                .filter(x -> {
                    String value = annotationValue(x.getValue(), KameletAnnotationsNames.KAMELET_ANNOTATION_PROVIDER);
                    return value != null && value.equalsIgnoreCase(provider);
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
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

    public boolean hasDataTypes(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            if (!kamelet.getSpec().getDataTypes().isEmpty()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public Map<String, DataTypes> getDataTypes(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getDataTypes();
        } else {
            return null;
        }
    }

    public void getAllKameletDependencies() {
        Map<String, Kamelet> treeMap = new TreeMap<>(kameletModels);
        for (Map.Entry<String, Kamelet> entry : treeMap.entrySet()) {
            StringBuilder builder = new StringBuilder();
            for (String dep : entry.getValue().getSpec().getDependencies()) {
                builder.append(dep + System.lineSeparator());
            }
            System.out.println(entry.getKey());
            System.out.println("---------------------------------------------------------------------------------------------------");
            System.out.println(builder.toString());
            builder.append(System.lineSeparator());
        }
    }

    public Template getKameletTemplate(String name) {
        Kamelet kamelet = kameletModels.get(name);
        if (kamelet != null) {
            return kamelet.getSpec().getTemplate();
        } else {
            return null;
        }
    }

    public List<ComponentModel.EndpointHeaderModel> getKameletSupportedHeaders(String name) {
        List<ComponentModel.EndpointHeaderModel> resultingHeaders = new ArrayList<>();
        Kamelet local = kameletModels.get(name);
        if (ObjectHelper.isNotEmpty(local)) {
            // What the Kamelet declares about itself wins. The component list describes
            // everything the component can emit, which both over-reports headers this
            // template never surfaces and misses the ones the template adds itself.
            List<ComponentModel.EndpointHeaderModel> declared = getDeclaredHeaders(local);
            if (!declared.isEmpty()) {
                return declared;
            }
            String camelType = determineCamelType(local);
            String kameletName = local.getMetadata().getName();
            int lastIndex = kameletName.lastIndexOf("-");
            String prefixName = local.getMetadata().getName().substring(0, lastIndex);
            String schemeName = enumValue(prefixName);
            if (schemeName != null) {
                ComponentModel componentModel = camelCatalog().componentModel(schemeName);
                if (componentModel != null && ObjectHelper.isNotEmpty(componentModel.getEndpointHeaders())) {
                    List<ComponentModel.EndpointHeaderModel> headers = componentModel.getEndpointHeaders();
                    for (ComponentModel.EndpointHeaderModel e : headers) {
                        if (ObjectHelper.isEmpty(e.getLabel()) || e.getLabel().equalsIgnoreCase(camelType)) {
                            resultingHeaders.add(e);
                        }
                    }
                }
            }
        }
        return resultingHeaders;
    }

    /**
     * Headers the Kamelet declares under spec.dataTypes, which describe what this
     * template actually emits or consumes rather than what its component supports.
     */
    private List<ComponentModel.EndpointHeaderModel> getDeclaredHeaders(Kamelet kamelet) {
        if (kamelet.getSpec() == null || kamelet.getSpec().getDataTypes() == null) {
            return new ArrayList<>();
        }
        // Keyed by name, because the same header commonly repeats across the data types
        // of one side.
        Map<String, ComponentModel.EndpointHeaderModel> declared = new LinkedHashMap<>();
        for (DataTypes dataType : kamelet.getSpec().getDataTypes().values()) {
            if (dataType == null) {
                continue;
            }
            if (dataType.getHeaders() != null && !dataType.getHeaders().isEmpty()) {
                // The side-level block is the authoritative summary for that side: it is
                // what the Kamelet always emits or consumes, whichever data type is in
                // use. Where it exists it is the answer, and the per-type blocks below
                // are deliberately not merged into it -- those headers appear only when
                // their type is selected, so adding them would over-report exactly the
                // way the component list does.
                for (Map.Entry<String, Headers> entry : dataType.getHeaders().entrySet()) {
                    Headers header = entry.getValue();
                    declared.computeIfAbsent(entry.getKey(), n -> toHeaderModel(n,
                            header == null ? null : header.getTitle(),
                            header == null ? null : header.getDescription(),
                            header == null ? null : header.getType(),
                            header == null ? null : header.get_default(),
                            header == null ? null : header.getRequired()));
                }
                continue;
            }
            // No side-level block, so fall back to what its data types declare before
            // falling back to the component. A Kamelet that only transforms its payload
            // puts its headers there, and reading nothing made the catalog report that
            // such a Kamelet supports no headers at all.
            if (dataType.getTypes() != null) {
                for (Types type : dataType.getTypes().values()) {
                    if (type == null || type.getHeaders() == null) {
                        continue;
                    }
                    for (Map.Entry<String, org.apache.camel.v1.kameletspec.datatypes.types.Headers> entry
                            : type.getHeaders().entrySet()) {
                        org.apache.camel.v1.kameletspec.datatypes.types.Headers header = entry.getValue();
                        declared.computeIfAbsent(entry.getKey(), n -> toHeaderModel(n,
                                header == null ? null : header.getTitle(),
                                header == null ? null : header.getDescription(),
                                header == null ? null : header.getType(),
                                header == null ? null : header.get_default(),
                                header == null ? null : header.getRequired()));
                    }
                }
            }
        }
        return new ArrayList<>(declared.values());
    }

    /**
     * The side-level and type-level header POJOs are generated separately and share no
     * supertype, so the fields are passed in rather than the object.
     */
    private ComponentModel.EndpointHeaderModel toHeaderModel(
            String name, String title, String description, String type, String defaultValue, Boolean required) {
        ComponentModel.EndpointHeaderModel model = new ComponentModel.EndpointHeaderModel();
        model.setName(name);
        model.setDisplayName(title);
        model.setDescription(description);
        model.setType(type);
        model.setJavaType(type);
        model.setDefaultValue(defaultValue);
        model.setRequired(Boolean.TRUE.equals(required));
        return model;
    }

    public String getKameletScheme(String prefix) {
        return enumValue(prefix);
    }

    private String enumValue(String prefix) {
        for (KameletPrefixSchemeEnum c : KameletPrefixSchemeEnum.values()) {
            if (c.name.equals(prefix)) return c.scheme;
        }
        return null;
    }

    private String determineCamelType(Kamelet local) {
        String camelType;
        String kameletType = local.getMetadata().getLabels().get(KameletLabelNames.KAMELET_LABEL_TYPE);
        if (kameletType.equalsIgnoreCase(KameletTypeEnum.SINK.type())) {
            camelType = "producer";
        } else {
            camelType = "consumer";
        }
        return camelType;
    }
}
