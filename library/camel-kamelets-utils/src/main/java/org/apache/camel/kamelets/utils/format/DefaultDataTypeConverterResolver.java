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
import org.apache.camel.spi.FactoryFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link DataTypeConverterResolver} which tries to find components by using the URI scheme prefix
 * and searching for a file of the URI scheme name in the <b>META-INF/services/org/apache/camel/datatype/converter/</b> directory
 * on the classpath.
 */
public class DefaultDataTypeConverterResolver implements DataTypeConverterResolver {

    public static final String RESOURCE_PATH = "META-INF/services/org/apache/camel/datatype/converter/";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataTypeConverterResolver.class);

    private FactoryFinder factoryFinder;

    @Override
    public Optional<DataTypeConverter> resolve(String scheme, String name, CamelContext context) {
        String converterName = String.format("%s-%s", scheme, name);
        Class<?> type = findConverter(converterName, context);
        if (type == null) {
            // not found
            return Optional.empty();
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Found data type converter: {} via type: {} via: {}{}", converterName,
                    type.getName(), factoryFinder.getResourcePath(), converterName);
        }

        // create the converter instance
        if (DataTypeConverter.class.isAssignableFrom(type)) {
            try {
                return Optional.of((DataTypeConverter) context.getInjector().newInstance(type));
            } catch (NoClassDefFoundError e) {
                LOG.debug("Ignoring converter type: {} as a dependent class could not be found: {}",
                        type.getCanonicalName(), e, e);
            }
        } else {
            throw new IllegalArgumentException("Type is not a DataTypeConverter implementation. Found: " + type.getName());
        }

        return Optional.empty();
    }

    private Class<?> findConverter(String name, CamelContext context) {
        if (factoryFinder == null) {
            factoryFinder = context.adapt(ExtendedCamelContext.class).getFactoryFinder(RESOURCE_PATH);
        }
        return factoryFinder.findClass(name).orElse(null);
    }

    protected Logger getLog() {
        return LOG;
    }

}
