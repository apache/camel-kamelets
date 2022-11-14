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
package org.apache.camel.kamelets.utils.format.converter.aws2.s3;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.kamelets.utils.format.DefaultDataTypeRegistry;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AWS2S3JsonOutputTypeTest {

    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    private final AWS2S3JsonOutputType outputType = new AWS2S3JsonOutputType();

    @Test
    void shouldMapFromStringToJsonModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader(AWS2S3Constants.KEY, "test1.txt");
        exchange.getMessage().setBody("Test1");
        outputType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertEquals("test1.txt", exchange.getMessage().getHeader(AWS2S3Constants.KEY));

        assertJsonModelBody(exchange, "test1.txt", "Test1");
    }

    @Test
    void shouldMapFromBytesToJsonModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader(AWS2S3Constants.KEY, "test2.txt");
        exchange.getMessage().setBody("Test2".getBytes(StandardCharsets.UTF_8));
        outputType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertEquals("test2.txt", exchange.getMessage().getHeader(AWS2S3Constants.KEY));

        assertJsonModelBody(exchange, "test2.txt", "Test2");
    }

    @Test
    void shouldMapFromInputStreamToJsonModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader(AWS2S3Constants.KEY, "test3.txt");
        exchange.getMessage().setBody(new ResponseInputStream<>(GetObjectRequest.builder().bucket("myBucket").key("test3.txt").build(),
                AbortableInputStream.create(new ByteArrayInputStream("Test3".getBytes(StandardCharsets.UTF_8)))));
        outputType.convert(exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        assertEquals("test3.txt", exchange.getMessage().getHeader(AWS2S3Constants.KEY));

        assertJsonModelBody(exchange, "test3.txt", "Test3");
    }

    @Test
    public void shouldLookupDataType() throws Exception {
        DefaultDataTypeRegistry dataTypeRegistry = new DefaultDataTypeRegistry();
        CamelContextAware.trySetCamelContext(dataTypeRegistry, camelContext);
        Optional<DataTypeConverter> converter = dataTypeRegistry.lookup("aws2-s3", "json");
        Assertions.assertTrue(converter.isPresent());
    }

    private static void assertJsonModelBody(Exchange exchange, String key, String content) {
        assertEquals(String.format("{\"key\": \"%s\", \"content\": \"%s\"}", key, content), exchange.getMessage().getBody());
    }
}
