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

import org.apache.camel.CamelContextAware;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.kamelets.utils.format.converter.bytes.ByteArrayDataType;
import org.apache.camel.kamelets.utils.format.converter.text.StringDataType;
import org.apache.camel.kamelets.utils.format.converter.test.UppercaseDataType;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultDataTypeRegistryTest {

    private final DefaultDataTypeRegistry dataTypeRegistry = new DefaultDataTypeRegistry();

    @BeforeEach
    void setup() {
        CamelContextAware.trySetCamelContext(dataTypeRegistry, new DefaultCamelContext());
    }

    @Test
    public void shouldLookupDefaultDataTypeConverters() throws Exception {
        Optional<DataTypeConverter> converter = dataTypeRegistry.lookup( "plain-text");
        Assertions.assertTrue(converter.isPresent());
        Assertions.assertEquals(StringDataType.class, converter.get().getClass());
        converter = dataTypeRegistry.lookup( "application-octet-stream");
        Assertions.assertTrue(converter.isPresent());
        Assertions.assertEquals(ByteArrayDataType.class, converter.get().getClass());
        converter = dataTypeRegistry.lookup( "lowercase");
        Assertions.assertTrue(converter.isPresent());
        converter = dataTypeRegistry.lookup( "uppercase");
        Assertions.assertTrue(converter.isPresent());
        Assertions.assertEquals(UppercaseDataType.class, converter.get().getClass());
    }

}
