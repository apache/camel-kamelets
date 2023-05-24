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

package org.apache.camel.kamelets.utils.format.converter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.kamelets.utils.format.MimeType;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Data type uses Jackson data format to marshal given Exchange payload to a Json (binary byte array representation).
 * Requires Exchange payload as JsonNode representation.
 */
@DataType(name = "application-json", mediaType = "application/json")
public class JsonDataType implements DataTypeConverter {

    @Override
    public void convert(Exchange exchange) {
        try {
            byte[] marshalled = Json.MAPPER.writer().forType(JsonNode.class).writeValueAsBytes(exchange.getMessage().getBody());
            exchange.getMessage().setBody(marshalled);

            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MimeType.JSON.type());
            exchange.getMessage().setHeader(SchemaHelper.CONTENT_SCHEMA,
                    exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, "", String.class));
        } catch (JsonProcessingException e) {
            throw new CamelExecutionException("Failed to apply Json output data type on exchange", exchange, e);
        }
    }
}
