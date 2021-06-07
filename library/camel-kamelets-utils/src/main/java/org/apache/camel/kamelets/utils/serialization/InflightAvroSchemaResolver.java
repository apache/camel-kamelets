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
package org.apache.camel.kamelets.utils.serialization;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;

import org.apache.avro.Schema;
import org.apache.camel.Exchange;
import org.apache.camel.component.jackson.SchemaResolver;

public class InflightAvroSchemaResolver implements SchemaResolver {

    @Override
    public FormatSchema resolve(Exchange exchange) {
        String schemaJson = (String) exchange.getProperty("schema");
        Boolean validate = Boolean.valueOf((String) exchange.getProperty("validate"));
        Schema raw = new Schema.Parser().setValidate(validate).parse(schemaJson);
        AvroSchema schema = new AvroSchema(raw);
        return schema;
    }

}
