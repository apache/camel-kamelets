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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.kamelets.utils.format.DefaultDataTypeRegistry;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringDataTypeTest {

    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    private final StringDataType dataType = new StringDataType();

    @Test
    void shouldRetainStringModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader("file", "test.txt");
        exchange.getMessage().setBody("Test");
        dataType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertStringBody(exchange, "test.txt", "Test");
    }

    @Test
    void shouldMapFromBinaryToStringModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader("file", "test1.txt");
        exchange.getMessage().setBody("Test1".getBytes(StandardCharsets.UTF_8));
        dataType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertStringBody(exchange, "test1.txt", "Test1");
    }

    @Test
    void shouldMapFromInputStreamToStringModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader("file", "test3.txt");
        exchange.getMessage().setBody(new ByteArrayInputStream("Test3".getBytes(StandardCharsets.UTF_8)));
        dataType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertStringBody(exchange, "test3.txt", "Test3");
    }

    @Test
    public void shouldLookupDataType() throws Exception {
        DefaultDataTypeRegistry dataTypeRegistry = new DefaultDataTypeRegistry();
        CamelContextAware.trySetCamelContext(dataTypeRegistry, camelContext);
        Optional<DataTypeConverter> converter = dataTypeRegistry.lookup( "string");
        Assertions.assertTrue(converter.isPresent());
    }

    private static void assertStringBody(Exchange exchange, String key, String content) {
        assertEquals(key, exchange.getMessage().getHeader("file"));

        assertEquals(String.class, exchange.getMessage().getBody().getClass());
        assertEquals(content, exchange.getMessage().getBody(String.class));
    }
}
