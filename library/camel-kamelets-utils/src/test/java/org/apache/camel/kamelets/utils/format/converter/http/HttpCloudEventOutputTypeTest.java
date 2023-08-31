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

package org.apache.camel.kamelets.utils.format.converter.http;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.TransformerKey;
import org.apache.camel.kamelets.utils.format.converter.utils.CloudEvents;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Transformer;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCloudEventOutputTypeTest {

    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    private final HttpCloudEventOutputType outputType = new HttpCloudEventOutputType();

    @Test
    void shouldMapToHttpCloudEvent() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader(CloudEvents.CAMEL_CLOUD_EVENT_SUBJECT, "test1.txt");
        exchange.getMessage().setHeader(CloudEvents.CAMEL_CLOUD_EVENT_TYPE, "org.apache.camel.event");
        exchange.getMessage().setHeader(CloudEvents.CAMEL_CLOUD_EVENT_SOURCE, "org.apache.camel.test");
        exchange.getMessage().setHeader(CloudEvents.CAMEL_CLOUD_EVENT_CONTENT_TYPE, "text/plain");
        exchange.getMessage().setBody(new ByteArrayInputStream("Test1".getBytes(StandardCharsets.UTF_8)));

        outputType.transform(exchange.getMessage(), DataType.ANY, new DataType("http:application-cloudevents"));

        assertTrue(exchange.getMessage().hasHeaders());
        assertEquals(exchange.getExchangeId(), exchange.getMessage().getHeader("ce-id"));
        assertEquals("1.0", exchange.getMessage().getHeader("ce-specversion"));
        assertEquals("org.apache.camel.event", exchange.getMessage().getHeader("ce-type"));
        assertEquals("test1.txt", exchange.getMessage().getHeader("ce-subject"));
        assertEquals("org.apache.camel.test", exchange.getMessage().getHeader("ce-source"));
        assertTrue(exchange.getMessage().getHeaders().containsKey("ce-time"));
        assertEquals("text/plain", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
        assertEquals("Test1", exchange.getMessage().getBody(String.class));
    }

    @Test
    public void shouldLookupDataType() throws Exception {
        Transformer transformer = camelContext.getTransformerRegistry().resolveTransformer(new TransformerKey(DataType.ANY, new DataType("http:application-cloudevents")));
        Assertions.assertNotNull(transformer);
    }
}
