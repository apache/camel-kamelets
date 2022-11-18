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

import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverterResolver;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link DataTypeConverterResolver} which tries to find components by using the URI scheme prefix
 * and searching for a file of the URI scheme name in the <b>META-INF/services/org/apache/camel/datatype/converter/</b> directory
 * on the classpath.
 */
public class DefaultDataTypeConverterResolver implements DataTypeConverterResolver {

    public static final String DATA_TYPE_CONVERTER_RESOURCE_PATH = "META-INF/services/org/apache/camel/datatype/converter/";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataTypeConverterResolver.class);

    @Override
    public Optional<DataTypeConverter> resolve(String scheme, String name, CamelContext context) {
        String converterName = String.format("%s-%s", scheme, name);

        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolving data type converter {} via: {}{}", converterName, DATA_TYPE_CONVERTER_RESOURCE_PATH, converterName);
        }

        Optional<DataTypeConverter> converter = findConverter(converterName, context);
        if (getLog().isDebugEnabled() && converter.isPresent()) {
            getLog().debug("Found data type converter: {} via type: {} via: {}{}", converterName,
                    ObjectHelper.name(converter.getClass()), DATA_TYPE_CONVERTER_RESOURCE_PATH, converterName);
        }

        return converter;
    }

    private Optional<DataTypeConverter> findConverter(String name, CamelContext context) {
        return context.adapt(ExtendedCamelContext.class)
                .getBootstrapFactoryFinder(DATA_TYPE_CONVERTER_RESOURCE_PATH)
                .newInstance(name, DataTypeConverter.class);
    }

    protected Logger getLog() {
        return LOG;
    }

}
