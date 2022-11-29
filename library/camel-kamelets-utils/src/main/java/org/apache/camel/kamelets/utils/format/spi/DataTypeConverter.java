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

import org.apache.camel.Exchange;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Converter applies custom logic to a given exchange in order to update the message content in that exchange according to
 * the data type.
 */
@FunctionalInterface
public interface DataTypeConverter {

    /**
     * Changes the exchange message content (body and/or header) to represent the data type.
     * @param exchange the exchange that should have its message content applied to the data type.
     */
    void convert(Exchange exchange);

    /**
     * Gets the data type converter name. Automatically derives the name from given data type annotation if any.
     * Subclasses may add a fallback logic to determine the data type name in case the annotation is missing.
     * @return the name of the data type.
     */
    default String getName() {
        if (this.getClass().isAnnotationPresent(DataType.class)) {
            return this.getClass().getAnnotation(DataType.class).name();
        }

        throw new UnsupportedOperationException("Missing data type converter name");
    }

    /**
     * Gets the data type component scheme. Automatically derived from given data type annotation.
     * Subclasses may add custom logic to determine the data type scheme. By default, the generic Camel scheme is used.
     * @return the component scheme of the data type.
     */
    default String getScheme() {
        if (this.getClass().isAnnotationPresent(DataType.class)) {
            return this.getClass().getAnnotation(DataType.class).scheme();
        }

        return DataType.DEFAULT_SCHEME;
    }

    /**
     * Gets the data type media type. Automatically derived from given data type annotation.
     * Subclasses may add additional logic to determine the media type when annotation is missing.
     * By default, returns empty String as a media type.
     * @return the media type of the data type.
     */
    default String getMediaType() {
        if (this.getClass().isAnnotationPresent(DataType.class)) {
            return this.getClass().getAnnotation(DataType.class).mediaType();
        }

        return "";
    }
}
