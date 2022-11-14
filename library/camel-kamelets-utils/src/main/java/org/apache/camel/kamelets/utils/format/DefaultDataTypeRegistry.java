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
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.engine.DefaultInjector;
import org.apache.camel.impl.engine.DefaultPackageScanClassResolver;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.DataTypeLoader;
import org.apache.camel.kamelets.utils.format.spi.DataTypeRegistry;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.support.service.ServiceSupport;

/**
 * Default data type registry able to resolve data types converters in the project. Data types may be defined at the component level
 * via {@link org.apache.camel.kamelets.utils.format.spi.annotations.DataType} annotations. Also, users can add data types directly
 * to the Camel context or manually to the registry.
 *
 * The registry is able to retrieve converters for a given data type based on the component scheme and the given data type name.
 */
public class DefaultDataTypeRegistry extends ServiceSupport implements DataTypeRegistry, CamelContextAware {

    private CamelContext camelContext;

    private PackageScanClassResolver resolver;

    protected final List<DataTypeLoader> dataTypeLoaders = new ArrayList<>();

    private final Map<String, List<DataTypeConverter>> dataTypeConverters = new HashMap<>();

    @Override
    public void addDataTypeConverter(String scheme, DataTypeConverter converter) {
        this.getComponentDataTypeConverters(scheme).add(converter);
    }

    @Override
    public Optional<DataTypeConverter> lookup(String scheme, String name) {
        if (dataTypeLoaders.isEmpty()) {
            try {
                doInit();
            } catch (Exception e) {
                throw new RuntimeCamelException("Failed to initialize data type registry", e);
            }
        }

        if (name == null) {
            return Optional.empty();
        }

        Optional<DataTypeConverter> componentDataTypeConverter = getComponentDataTypeConverters(scheme).stream()
                .filter(dtc -> name.equals(dtc.getName()))
                .findFirst();

        if (componentDataTypeConverter.isPresent()) {
            return componentDataTypeConverter;
        }

        return getDefaultDataTypeConverter(name);
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();

        if (resolver == null) {
            if (camelContext != null) {
                resolver = camelContext.adapt(ExtendedCamelContext.class).getPackageScanClassResolver();
            } else {
                resolver = new DefaultPackageScanClassResolver();
            }
        }

        dataTypeLoaders.add(new AnnotationDataTypeLoader(new DefaultInjector(camelContext), resolver));

        addDataTypeConverter(new DefaultDataTypeConverter("string", String.class));
        addDataTypeConverter(new DefaultDataTypeConverter("binary", byte[].class));

        for (DataTypeLoader loader : dataTypeLoaders) {
            CamelContextAware.trySetCamelContext(loader, getCamelContext());
            loader.load(this);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        this.dataTypeConverters.clear();
    }

    /**
     * Retrieve default data output type from Camel context for given format name.
     * @param name
     * @return
     */
    private Optional<DataTypeConverter> getDefaultDataTypeConverter(String name) {
        Optional<DataTypeConverter> dataTypeConverter = getComponentDataTypeConverters("camel").stream()
                .filter(dtc -> name.equals(dtc.getName()))
                .findFirst();

        if (dataTypeConverter.isPresent()) {
            return dataTypeConverter;
        }

        return Optional.ofNullable(camelContext.getRegistry().lookupByNameAndType(name, DataTypeConverter.class));
    }

    /**
     * Retrieve list of data types defined on the component level for given scheme.
     * @param scheme
     * @return
     */
    private List<DataTypeConverter> getComponentDataTypeConverters(String scheme) {
        if (!dataTypeConverters.containsKey(scheme)) {
            dataTypeConverters.put(scheme, new ArrayList<>());
        }

        return dataTypeConverters.get(scheme);
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
