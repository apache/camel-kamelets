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
import org.apache.camel.Message;
import org.apache.camel.kamelets.utils.format.MimeType;
import org.apache.camel.kamelets.utils.format.SchemaType;
import org.apache.camel.kamelets.utils.format.converter.avro.Avro;
import org.apache.camel.kamelets.utils.format.converter.json.Json;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DataTypeTransformer;
import org.apache.camel.spi.Transformer;
import org.apache.camel.util.ObjectHelper;

/**
 * Data type able to unmarshal Exchange body to Java object. Supports both Avro and Json schema types and uses respective
 * Jackson implementation for the unmarshal operation.
 * Requires proper setting of content schema, class and schema type in Exchange properties
 * (usually resolved via Avro or Json schema resolver Kamelet action).
 */
@DataTypeTransformer(name = "application-x-java-object")
public class JavaObjectDataType extends Transformer implements CamelContextAware {

    private CamelContext camelContext;

    @Override
    public void transform(Message message, DataType fromType, DataType toType) {
        ObjectHelper.notNull(camelContext, "camelContext");

        FormatSchema schema = message.getExchange().getProperty(SchemaHelper.CONTENT_SCHEMA, FormatSchema.class);
        if (schema == null) {
            throw new CamelExecutionException("Missing proper schema for Java object data type processing", message.getExchange());
        }

        String contentClass = SchemaHelper.resolveContentClass(message.getExchange(), null);
        if (contentClass == null) {
            throw new CamelExecutionException("Missing content class information for Java object data type processing",
                    message.getExchange());
        }

        SchemaType schemaType = SchemaType.of(message.getExchange().getProperty(SchemaHelper.CONTENT_SCHEMA_TYPE, SchemaType.JSON.type(), String.class));

        try {
            Class<?> contentType = camelContext.getClassResolver().resolveMandatoryClass(contentClass);
            Object unmarshalled;

            if (schemaType == SchemaType.AVRO) {
                unmarshalled = Avro.MAPPER.reader().forType(contentType).with(schema).readValue(getBodyAsStream(message));
            } else if (schemaType == SchemaType.JSON) {
                unmarshalled = Json.MAPPER.reader().forType(contentType).with(schema).readValue(getBodyAsStream(message));
            } else {
                throw new CamelExecutionException(String.format("Unsupported schema type '%s'", schemaType), message.getExchange());
            }

            message.setBody(unmarshalled);

            message.setHeader(Exchange.CONTENT_TYPE, MimeType.STRUCT.type());
        } catch (InvalidPayloadException | IOException | ClassNotFoundException e) {
            throw new CamelExecutionException("Failed to apply Java object data type on exchange", message.getExchange(), e);
        }
    }

    private InputStream getBodyAsStream(Message message) throws InvalidPayloadException {
        InputStream bodyStream = message.getBody(InputStream.class);

        if (bodyStream == null) {
            bodyStream = new ByteArrayInputStream(message.getMandatoryBody(byte[].class));
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
