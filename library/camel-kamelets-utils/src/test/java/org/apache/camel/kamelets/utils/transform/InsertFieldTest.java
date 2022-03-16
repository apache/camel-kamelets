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
package org.apache.camel.kamelets.utils.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.Map;

class InsertFieldTest {

    private DefaultCamelContext camelContext;

    private final ObjectMapper mapper = new ObjectMapper();

    private final InsertField processor = new InsertField();

    private final String baseJson = "{" +
                "\"name\":\"Rajesh Koothrappali\"" +
            "}";

    private final String arrayJson = "[\"batman\",\"spiderman\",\"wonderwoman\"]";

    @BeforeEach
    void setup() {
        this.camelContext = new DefaultCamelContext();
    }

    @Test
    void shouldAddFieldToPlainJson() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree(baseJson));

        processor.process("age", "29", exchange);

        Assertions.assertEquals(exchange.getMessage().getBody(String.class), "{" +
                        "\"name\":\"Rajesh Koothrappali\"," +
                        "\"age\":\"29\"" +
                        "}");
    }

    @Test
    void shouldAddFieldToArrayJson() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree(arrayJson));

        processor.process(null, "green lantern", exchange);

        Assertions.assertEquals(exchange.getMessage().getBody(String.class),
                "[\"batman\",\"spiderman\",\"wonderwoman\",\"green lantern\"]");
    }
}
