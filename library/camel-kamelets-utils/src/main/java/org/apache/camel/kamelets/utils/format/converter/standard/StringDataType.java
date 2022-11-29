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

package org.apache.camel.kamelets.utils.format.converter.standard;

import org.apache.camel.Exchange;
import org.apache.camel.kamelets.utils.format.DefaultDataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * String data type.
 */
@DataType(name = "string", mediaType = "text/plain")
public class StringDataType implements DataTypeConverter {

    private static final DataTypeConverter DELEGATE =
            new DefaultDataTypeConverter(DataType.DEFAULT_SCHEME, "string", "text/plain", String.class);

    @Override
    public void convert(Exchange exchange) {
        DELEGATE.convert(exchange);
    }
}
