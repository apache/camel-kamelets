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

import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Registry for data types and its converters. Data type loaders should be used to add members to the registry.
 * <p/>
 * The registry is able to perform a lookup of a specific data type by its given scheme and name. Usually data types are grouped
 * by their component scheme so users may use component specific converters and default Camel converters.
 */
public interface DataTypeRegistry {

    /**
     * Registers a new default data type converter. Usually used to add default Camel converter implementations.
     *
     * @param scheme the data type scheme.
     * @param converter the converter implementation.
     */
    void addDataTypeConverter(String scheme, DataTypeConverter converter);

    /**
     * Registers a new default data type converter. Uses the default Camel scheme to mark this converter as generic one.
     *
     * @param converter the data type converter implementation.
     */
    default void addDataTypeConverter(DataTypeConverter converter) {
        addDataTypeConverter(DataType.DEFAULT_SCHEME, converter);
    }

    /**
     * Find data type for given component scheme and data type name. Searches for the component scheme specific converter first.
     * As a fallback may also try to resolve the converter with only the name in the given set of default Camel converters registered in this registry.
     *
     * @param scheme the data type converter scheme (usually a component scheme).
     * @param name the data type converter name.
     * @return optional data type converter implementation matching the given scheme and name.
     */
    Optional<DataTypeConverter> lookup(String scheme, String name);

    /**
     * Find data type for given data type name. Just searches the set of default Camel converter implementations registered in this registry.
     *
     * @param name the data type converter name.
     * @return
     */
    default Optional<DataTypeConverter> lookup(String name) {
        return lookup(DataType.DEFAULT_SCHEME, name);
    }
}
