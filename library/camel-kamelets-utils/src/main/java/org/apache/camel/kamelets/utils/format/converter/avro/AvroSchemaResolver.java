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

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import org.apache.avro.Schema;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.jackson.SchemaResolver;
import org.apache.camel.kamelets.utils.format.SchemaType;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.util.ObjectHelper;

public class AvroSchemaResolver implements SchemaResolver, Processor {
    private final ConcurrentMap<String, AvroSchema> schemes;

    private AvroSchema schema;
    private String contentClass;

    private boolean validate = true;

    public AvroSchemaResolver() {
        this.schemes = new ConcurrentHashMap<>();
    }

    public String getSchema() {
        if (this.schema != null) {
            return this.schema.getAvroSchema().toString();
        }

        return null;
    }

    public void setSchema(String schema) {
        if (ObjectHelper.isNotEmpty(schema)) {
            this.schema = new AvroSchema(new Schema.Parser().setValidate(validate).parse(schema));
        } else {
            this.schema = null;
        }
    }

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public String getContentClass() {
        return contentClass;
    }

    public void setContentClass(String contentClass) {
        if (ObjectHelper.isNotEmpty(contentClass)) {
            this.contentClass = contentClass;
        } else {
            this.contentClass = null;
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object payload = exchange.getMessage().getBody();
        if (payload == null) {
            return;
        }

        AvroSchema answer = computeIfAbsent(exchange);

        if (answer != null) {
            exchange.setProperty(SchemaHelper.CONTENT_SCHEMA, answer);
            exchange.setProperty(SchemaHelper.CONTENT_SCHEMA_TYPE, SchemaType.AVRO.type());
            exchange.setProperty(SchemaHelper.CONTENT_CLASS, SchemaHelper.resolveContentClass(exchange, this.contentClass));
        }
    }

    @Override
    public FormatSchema resolve(Exchange exchange) {
        AvroSchema answer = exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, AvroSchema.class);
        if (answer == null) {
            answer = computeIfAbsent(exchange);
        }

        return answer;
    }

    private AvroSchema computeIfAbsent(Exchange exchange) {
         if (this.schema != null) {
            return this.schema;
         }

        AvroSchema answer = exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, AvroSchema.class);

        if (answer == null && exchange.getProperties().containsKey(SchemaHelper.SCHEMA)) {
            String schemaJson = exchange.getProperty(SchemaHelper.SCHEMA, String.class);
            Schema raw = new Schema.Parser().setValidate(validate).parse(schemaJson);
            answer = new AvroSchema(raw);
        }

        if (answer == null) {
            String contentClass = SchemaHelper.resolveContentClass(exchange, this.contentClass);
            if (contentClass != null) {
                answer = this.schemes.computeIfAbsent(contentClass, t -> {
                    Resource res = PluginHelper.getResourceLoader(exchange.getContext())
                            .resolveResource("classpath:schemas/" + SchemaType.AVRO.type() + "/" + t + "." + SchemaType.AVRO.type());

                    try {
                        if (res.exists()) {
                            try (InputStream is = res.getInputStream()) {
                                if (is != null) {
                                    return Avro.MAPPER.schemaFrom(is);
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Unable to load Avro schema for type: " + t + ", resource: " + res.getLocation(), e);
                    }

                    try {
                        return Avro.MAPPER.schemaFor(Class.forName(contentClass));
                    } catch (JsonMappingException | ClassNotFoundException e) {
                        throw new RuntimeException(
                                "Unable to compute Avro schema for type: " + t, e);
                    }
                });
            }
        }

        if (answer != null) {
            this.schema = answer;
        }

        return answer;
    }
}
