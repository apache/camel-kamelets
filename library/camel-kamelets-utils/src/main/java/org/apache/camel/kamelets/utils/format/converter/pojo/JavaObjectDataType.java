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

package org.apache.camel.kamelets.utils.format.converter.pojo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.FormatSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.kamelets.utils.format.MimeType;
import org.apache.camel.kamelets.utils.format.SchemaType;
import org.apache.camel.kamelets.utils.format.converter.avro.Avro;
import org.apache.camel.kamelets.utils.format.converter.json.Json;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;
import org.apache.camel.util.ObjectHelper;

/**
 * Data type able to unmarshal Exchange body to Java object. Supports both Avro and Json schema types and uses respective
 * Jackson implementation for the unmarshal operation.
 * Requires proper setting of content schema, class and schema type in Exchange properties
 * (usually resolved via Avro or Json schema resolver Kamelet action).
 */
@DataType(name = "application-x-java-object", mediaType = "application/x-java-object")
public class JavaObjectDataType implements DataTypeConverter, CamelContextAware {

    private CamelContext camelContext;

    @Override
    public void convert(Exchange exchange) {
        ObjectHelper.notNull(camelContext, "camelContext");

        FormatSchema schema = exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, FormatSchema.class);
        if (schema == null) {
            throw new CamelExecutionException("Missing proper schema for Java object data type processing", exchange);
        }

        String contentClass = SchemaHelper.resolveContentClass(exchange, null);
        if (contentClass == null) {
            throw new CamelExecutionException("Missing content class information for Java object data type processing",
                    exchange);
        }

        SchemaType schemaType = SchemaType.of(exchange.getProperty(SchemaHelper.CONTENT_SCHEMA_TYPE, SchemaType.JSON.type(), String.class));

        try {
            Class<?> contentType = camelContext.getClassResolver().resolveMandatoryClass(contentClass);
            Object unmarshalled;

            if (schemaType == SchemaType.AVRO) {
                unmarshalled = Avro.MAPPER.reader().forType(contentType).with(schema).readValue(getBodyAsStream(exchange));
            } else if (schemaType == SchemaType.JSON) {
                unmarshalled = Json.MAPPER.reader().forType(contentType).with(schema).readValue(getBodyAsStream(exchange));
            } else {
                throw new CamelExecutionException(String.format("Unsupported schema type '%s'", schemaType), exchange);
            }

            exchange.getMessage().setBody(unmarshalled);

            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MimeType.STRUCT.type());
        } catch (InvalidPayloadException | IOException | ClassNotFoundException e) {
            throw new CamelExecutionException("Failed to apply Java object data type on exchange", exchange, e);
        }
    }

    private InputStream getBodyAsStream(Exchange exchange) throws InvalidPayloadException {
        InputStream bodyStream = exchange.getMessage().getBody(InputStream.class);

        if (bodyStream == null) {
            bodyStream = new ByteArrayInputStream(exchange.getMessage().getMandatoryBody(byte[].class));
        }

        return bodyStream;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }
}
