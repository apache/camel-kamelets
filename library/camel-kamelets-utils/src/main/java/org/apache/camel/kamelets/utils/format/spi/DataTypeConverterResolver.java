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

package org.apache.camel.kamelets.utils.format.spi;

import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Resolves data type converters from URI to be able to lazy load converters using factory finder discovery mechanism.
 */
@FunctionalInterface
public interface DataTypeConverterResolver {

    /**
     * Attempts to resolve the converter for the given scheme and name. Usually uses the factory finder URI to resolve the converter.
     * Scheme and name may be combined in order to resolve component specific converters. Usually implements a fallback
     * resolving mechanism when no matching converter for scheme and name is found (e.g. search for generic Camel converters just using the name).
     *
     * @param scheme the data type scheme.
     * @param name the data type name.
     * @param camelContext the current Camel context.
     * @return optional data type resolved via URI factory finder.
     */
    Optional<DataTypeConverter> resolve(String scheme, String name, CamelContext camelContext);

    /**
     * Attempts to resolve default converter for the given name. Uses default Camel scheme to resolve the converter via factory finder mechanism.
     *
     * @param name the data type name.
     * @param camelContext the current Camel context.
     * @return optional data type resolved via URI factory finder.
     */
    default Optional<DataTypeConverter> resolve(String name, CamelContext camelContext) {
        return resolve(DataType.DEFAULT_SCHEME, name, camelContext);
    }
}
