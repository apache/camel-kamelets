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

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.kamelets.utils.format.converter.standard.JsonModelDataType;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultDataTypeConverterResolverTest {

    private DefaultCamelContext camelContext;

    private final DefaultDataTypeConverterResolver resolver = new DefaultDataTypeConverterResolver();

    @BeforeEach
    void setup() {
        this.camelContext = new DefaultCamelContext();
    }

    @Test
    public void shouldHandleUnresolvableDataTypeConverters() throws Exception {
        Optional<DataTypeConverter> converter = resolver.resolve("unknown", camelContext);
        Assertions.assertFalse(converter.isPresent());

        converter = resolver.resolve("foo", "unknown", camelContext);
        Assertions.assertFalse(converter.isPresent());
    }

    @Test
    public void shouldResolveDataTypeConverters() throws Exception {
        Optional<DataTypeConverter> converter = resolver.resolve("jsonObject", camelContext);
        Assertions.assertTrue(converter.isPresent());
        Assertions.assertEquals(JsonModelDataType.class, converter.get().getClass());

        converter = resolver.resolve("foo", "json", camelContext);
        Assertions.assertTrue(converter.isPresent());
        Assertions.assertEquals(FooConverter.class, converter.get().getClass());

        converter = resolver.resolve("camel", "lowercase", camelContext);
        Assertions.assertTrue(converter.isPresent());
    }

    public static class FooConverter implements DataTypeConverter {

        @Override
        public void convert(Exchange exchange) {
            exchange.getMessage().setBody("Foo");
        }

        @Override
        public String getName() {
            return "foo";
        }
    }
}