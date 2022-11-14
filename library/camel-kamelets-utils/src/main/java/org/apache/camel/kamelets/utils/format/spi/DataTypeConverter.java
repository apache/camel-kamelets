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

@FunctionalInterface
public interface DataTypeConverter {

    void convert(Exchange exchange);

    /**
     * Gets the data type converter name. Automatically derives the name from given type annotation.
     * @return
     */
    default String getName() {
        if (this.getClass().isAnnotationPresent(DataType.class)) {
            return this.getClass().getAnnotation(DataType.class).name();
        }

        throw new UnsupportedOperationException("Missing data type converter name");
    }
}
