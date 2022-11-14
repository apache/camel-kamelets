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

/**
 * Registry for data types. Data type loaders should be used to add types to the registry.
 * <p/>
 * The registry is able to perform a lookup of a specific data type.
 */
public interface DataTypeRegistry {

    /**
     * Registers a new default data type converter.
     * @param scheme
     * @param converter
     */
    void addDataTypeConverter(String scheme, DataTypeConverter converter);

    /**
     * Registers a new default data type converter.
     * @param converter
     */
    default void addDataTypeConverter(DataTypeConverter converter) {
        addDataTypeConverter("camel", converter);
    }

    /**
     * Find data type for given component scheme and data type name.
     * @param scheme
     * @param name
     * @return
     */
    Optional<DataTypeConverter> lookup(String scheme, String name);

    /**
     * Find data type for given data type name.
     * @param name
     * @return
     */
    default Optional<DataTypeConverter> lookup(String name) {
        return lookup("camel", name);
    }
}
