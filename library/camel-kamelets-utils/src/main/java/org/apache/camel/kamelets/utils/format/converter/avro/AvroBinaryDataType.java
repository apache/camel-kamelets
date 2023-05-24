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

package org.apache.camel.kamelets.utils.format.converter.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.kamelets.utils.format.MimeType;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Data type uses Jackson Avro data format to marshal given JsonNode in Exchange body to a binary (byte array) representation.
 * Uses given Avro schema from the Exchange properties when marshalling the payload (usually already resolved via schema
 * resolver Kamelet action).
 */
@DataType(name = "avro-binary", mediaType = "avro/binary")
public class AvroBinaryDataType implements DataTypeConverter {

    @Override
    public void convert(Exchange exchange) {
        AvroSchema schema = exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, AvroSchema.class);

        if (schema == null) {
            throw new CamelExecutionException("Missing proper avro schema for data type processing", exchange);
        }

        try {
            byte[] marshalled = Avro.MAPPER.writer().forType(JsonNode.class).with(schema)
                    .writeValueAsBytes(getBodyAsJsonNode(exchange, schema));
            exchange.getMessage().setBody(marshalled);

            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MimeType.AVRO_BINARY.type());
            exchange.getMessage().setHeader(SchemaHelper.CONTENT_SCHEMA,
                    exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, "", String.class));
        } catch (InvalidPayloadException | IOException e) {
            throw new CamelExecutionException("Failed to apply Avro binary data type on exchange", exchange, e);
        }
    }

    private JsonNode getBodyAsJsonNode(Exchange exchange, AvroSchema schema) throws InvalidPayloadException, IOException {
        if (exchange.getMessage().getBody() instanceof  JsonNode) {
            return (JsonNode) exchange.getMessage().getBody();
        }

        return Avro.MAPPER.reader().forType(JsonNode.class).with(schema)
                .readValue(getBodyAsStream(exchange));
    }

    private InputStream getBodyAsStream(Exchange exchange) throws InvalidPayloadException {
        InputStream bodyStream = exchange.getMessage().getBody(InputStream.class);

        if (bodyStream == null) {
            bodyStream = new ByteArrayInputStream(exchange.getMessage().getMandatoryBody(byte[].class));
        }

        return bodyStream;
    }
}
