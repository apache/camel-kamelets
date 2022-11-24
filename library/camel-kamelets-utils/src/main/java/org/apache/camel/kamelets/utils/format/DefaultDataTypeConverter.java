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

import org.apache.camel.Exchange;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Default data type converter receives a name and a target type in order to use traditional exchange body conversion
 * mechanisms in order to transform the message body to a given type.
 */
public class DefaultDataTypeConverter implements DataTypeConverter {

    private final String scheme;
    private final String name;
    private final String mediaType;
    private final Class<?> type;

    public DefaultDataTypeConverter(String scheme, String name, String mediaType, Class<?> type) {
        this.scheme = scheme;
        this.name = name;
        this.mediaType = mediaType;
        this.type = type;
    }

    public DefaultDataTypeConverter(String scheme, String name, Class<?> type) {
        this(scheme, name, "", type);
    }

    public DefaultDataTypeConverter(String name, Class<?> type) {
        this(DataType.DEFAULT_SCHEME, name, type);
    }

    @Override
    public void convert(Exchange exchange) {
        if (type.isInstance(exchange.getMessage().getBody())) {
            return;
        }

        exchange.getMessage().setBody(exchange.getMessage().getBody(type));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getMediaType() {
        return mediaType;
    }

    public Class<?> getType() {
        return type;
    }
}
