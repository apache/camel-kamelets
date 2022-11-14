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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.kamelets.utils.format.DefaultDataTypeRegistry;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonModelDataTypeTest {

    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    private final JsonModelDataType dataType = new JsonModelDataType();

    @Test
    void shouldMapFromStringToJsonModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.setProperty(JsonModelDataType.JSON_DATA_TYPE_KEY, Person.class.getName());
        exchange.getMessage().setBody("{ \"name\": \"Sheldon\", \"age\": 29}");
        dataType.convert(exchange);

        assertEquals(Person.class, exchange.getMessage().getBody().getClass());
        assertEquals("Sheldon", exchange.getMessage().getBody(Person.class).getName());
    }

    @Test
    public void shouldLookupDataType() throws Exception {
        DefaultDataTypeRegistry dataTypeRegistry = new DefaultDataTypeRegistry();
        CamelContextAware.trySetCamelContext(dataTypeRegistry, camelContext);
        Optional<DataTypeConverter> converter = dataTypeRegistry.lookup("jsonObject");
        Assertions.assertTrue(converter.isPresent());
    }

    public static class Person {
        @JsonProperty
        private String name;

        @JsonProperty
        private Long age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getAge() {
            return age;
        }

        public void setAge(Long age) {
            this.age = age;
        }
    }

}