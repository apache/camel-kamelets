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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverterResolver;
import org.apache.camel.kamelets.utils.format.spi.DataTypeLoader;
import org.apache.camel.kamelets.utils.format.spi.DataTypeRegistry;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default data type registry able to resolve data types converters in the project. Data types may be defined at the component level
 * via {@link org.apache.camel.kamelets.utils.format.spi.annotations.DataType} annotations. Also, users can add data types directly
 * to the Camel context or manually to the registry.
 *
 * The registry is able to retrieve converters for a given data type based on the component scheme and the given data type name.
 */
public class DefaultDataTypeRegistry extends ServiceSupport implements DataTypeRegistry, CamelContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataTypeRegistry.class);

    private CamelContext camelContext;

    protected final List<DataTypeLoader> dataTypeLoaders = new ArrayList<>();

    private DataTypeConverterResolver dataTypeConverterResolver;

    private final Map<String, List<DataTypeConverter>> dataTypeConverters = new HashMap<>();

    @Override
    public void addDataTypeConverter(String scheme, DataTypeConverter converter) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding data type for scheme {} and name {}", scheme, converter.getName());
        }

        this.getComponentDataTypeConverters(scheme).add(converter);
    }

    @Override
    public Optional<DataTypeConverter> lookup(String scheme, String name) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Searching for data type with scheme {} and name {}", scheme, name);
        }

        if (dataTypeLoaders.isEmpty()) {
            LOG.trace("Lazy initializing data type registry");
            try {
                doInit();
            } catch (Exception e) {
                throw new RuntimeCamelException("Failed to initialize data type registry", e);
            }
        }

        if (name == null) {
            return Optional.empty();
        }

        Optional<DataTypeConverter> dataTypeConverter = getDataTypeConverter(scheme, name);
        if (!dataTypeConverter.isPresent()) {
            dataTypeConverter = getDataTypeConverter("camel", name);
        }

        return dataTypeConverter;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        dataTypeLoaders.add(new AnnotationDataTypeLoader());

        addDataTypeConverter(new DefaultDataTypeConverter("string", String.class));
        addDataTypeConverter(new DefaultDataTypeConverter("binary", byte[].class));

        for (DataTypeLoader loader : dataTypeLoaders) {
            CamelContextAware.trySetCamelContext(loader, getCamelContext());
            loader.load(this);
        }

        LOG.debug("Loaded {} initial data type converters", dataTypeConverters.size());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        this.dataTypeConverters.clear();
    }

    /**
     * Retrieve data type converter for given scheme and format name. First checks for matching bean in Camel registry then
     * tries to get from local cache or perform lazy lookup.
     * @param scheme
     * @param name
     * @return
     */
    private Optional<DataTypeConverter> getDataTypeConverter(String scheme, String name) {
        if (dataTypeConverterResolver == null) {
             dataTypeConverterResolver = Optional.ofNullable(camelContext.getRegistry().findSingleByType(DataTypeConverterResolver.class))
                     .orElseGet(DefaultDataTypeConverterResolver::new);
        }

        // Looking for matching beans in Camel registry first
        Optional<DataTypeConverter> dataTypeConverter = Optional.ofNullable(camelContext.getRegistry()
                .lookupByNameAndType(String.format("%s-%s", scheme, name), DataTypeConverter.class));

        if (dataTypeConverter.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found data type {} for scheme {} and name {} in Camel registry", ObjectHelper.name(dataTypeConverter.get().getClass()), scheme, name);
            }
            return dataTypeConverter;
        }

        // Try to retrieve converter from preloaded converters in local cache
        dataTypeConverter = getComponentDataTypeConverters(scheme).stream()
                .filter(dtc -> name.equals(dtc.getName()))
                .findFirst();

        if (dataTypeConverter.isPresent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found data type {} for scheme {} and name {}", ObjectHelper.name(dataTypeConverter.get().getClass()), scheme, name);
            }
            return dataTypeConverter;
        }

        // Try to lazy load converter via resource path lookup
        dataTypeConverter = dataTypeConverterResolver.resolve(scheme, name, camelContext);
        dataTypeConverter.ifPresent(converter -> getComponentDataTypeConverters(scheme).add(converter));

        if (LOG.isDebugEnabled() && dataTypeConverter.isPresent()) {
            LOG.debug("Resolved data type {} for scheme {} and name {} via resource path", ObjectHelper.name(dataTypeConverter.get().getClass()), scheme, name);
        }

        return dataTypeConverter;
    }

    /**
     * Retrieve list of data types defined on the component level for given scheme.
     * @param scheme
     * @return
     */
    private List<DataTypeConverter> getComponentDataTypeConverters(String scheme) {
        return dataTypeConverters.computeIfAbsent(scheme, (s) -> new ArrayList<>());
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }
}
