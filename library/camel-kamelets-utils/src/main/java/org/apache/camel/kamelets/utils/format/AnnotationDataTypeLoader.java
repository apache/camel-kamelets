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

package org.apache.camel.kamelets.utils.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.TypeConverterLoaderException;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.DataTypeLoader;
import org.apache.camel.kamelets.utils.format.spi.DataTypeRegistry;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;
import org.apache.camel.spi.Injector;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data type loader scans packages for {@link DataTypeConverter} classes annotated with {@link DataType} annotation.
 */
public class AnnotationDataTypeLoader implements DataTypeLoader {

    public static final String META_INF_SERVICES = "META-INF/services/org/apache/camel/DataType";

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationDataTypeLoader.class);

    protected final PackageScanClassResolver resolver;
    protected final Injector injector;

    protected Set<Class<?>> visitedClasses = new HashSet<>();
    protected Set<String> visitedURIs = new HashSet<>();

    public AnnotationDataTypeLoader(Injector injector, PackageScanClassResolver resolver) {
        this.injector = injector;
        this.resolver = resolver;
    }

    @Override
    public void load(DataTypeRegistry registry) {
        Set<String> packages = new HashSet<>();

        LOG.trace("Searching for {} services", META_INF_SERVICES);
        try {
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            if (ccl != null) {
                findPackages(packages, ccl);
            }
            findPackages(packages, getClass().getClassLoader());
            if (packages.isEmpty()) {
                LOG.debug("No package names found to be used for classpath scanning for annotated data types.");
                return;
            }
        } catch (Exception e) {
            throw new TypeConverterLoaderException(
                    "Cannot find package names to be used for classpath scanning for annotated data types.", e);
        }

        // if there is any packages to scan and load @DataType classes, then do it
        if (LOG.isTraceEnabled()) {
            LOG.trace("Found data type packages to scan: {}", String.join(", ", packages));
        }
        Set<Class<?>> scannedClasses = resolver.findAnnotated(DataType.class, packages.toArray(new String[]{}));
        if (!scannedClasses.isEmpty()) {
            LOG.debug("Found {} packages with {} @DataType classes to load", packages.size(), scannedClasses.size());

            // load all the found classes into the type data type registry
            for (Class<?> type : scannedClasses) {
                if (acceptClass(type)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Loading data type annotation: {}", ObjectHelper.name(type));
                    }
                    loadDataType(registry, type);
                }
            }
        }

        // now clear the maps so we do not hold references
        visitedClasses.clear();
        visitedURIs.clear();
    }

    private void loadDataType(DataTypeRegistry registry, Class<?> type) {
        if (visitedClasses.contains(type)) {
            return;
        }
        visitedClasses.add(type);

        try {
            if (DataTypeConverter.class.isAssignableFrom(type) && type.isAnnotationPresent(DataType.class)) {
                DataType dt = type.getAnnotation(DataType.class);
                DataTypeConverter converter = (DataTypeConverter) injector.newInstance(type);
                registry.addDataTypeConverter(dt.scheme(), converter);
            }
        } catch (NoClassDefFoundError e) {
            LOG.debug("Ignoring converter type: {} as a dependent class could not be found: {}",
                    type.getCanonicalName(), e, e);
        }
    }

    protected boolean acceptClass(Class<?> type) {
        return true;
    }

    protected void findPackages(Set<String> packages, ClassLoader classLoader) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(META_INF_SERVICES);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String path = url.getPath();
            if (!visitedURIs.contains(path)) {
                // remember we have visited this uri so we wont read it twice
                visitedURIs.add(path);
                LOG.debug("Loading file {} to retrieve list of packages, from url: {}", META_INF_SERVICES, url);
                try (BufferedReader reader = IOHelper.buffered(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        line = line.trim();
                        if (line.startsWith("#") || line.length() == 0) {
                            continue;
                        }
                        packages.add(line);
                    }
                }
            }
        }
    }
}
