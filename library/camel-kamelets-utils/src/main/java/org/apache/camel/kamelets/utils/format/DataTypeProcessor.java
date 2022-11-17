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
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;

/**
 * Processor applies data type conversion based on given format name. Searches for matching data type converter
 * with given component scheme and format name.
 */
public class DataTypeProcessor implements Processor, CamelContextAware {

    public static final String DATA_TYPE_FORMAT_PROPERTY = "CamelDataTypeFormat";

    private CamelContext camelContext;

    private DefaultDataTypeRegistry registry;

    private String scheme;
    private String format;

    private DataTypeConverter converter;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (exchange.hasProperties() && exchange.getProperties().containsKey(DATA_TYPE_FORMAT_PROPERTY)) {
            format = exchange.getProperty(DATA_TYPE_FORMAT_PROPERTY, String.class);
        }

        if (format == null || format.isEmpty()) {
            return;
        }

        doConverterLookup().ifPresent(converter -> converter.convert(exchange));
    }

    private Optional<DataTypeConverter> doConverterLookup() {
        if (converter != null) {
            return Optional.of(converter);
        }

        Optional<DataTypeConverter> maybeConverter = registry.lookup(scheme, format);
        maybeConverter.ifPresent(dataTypeConverter -> this.converter = dataTypeConverter);

        return maybeConverter;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setRegistry(DefaultDataTypeRegistry dataTypeRegistry) {
        this.registry = dataTypeRegistry;
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
