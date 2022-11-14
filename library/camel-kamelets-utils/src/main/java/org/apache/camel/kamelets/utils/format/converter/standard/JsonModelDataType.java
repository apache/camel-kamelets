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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Data type converter able to unmarshal to given unmarshalType using jackson data format.
 * <p/>
 * Unmarshal type should be given as a fully qualified class name in the exchange properties.
 */
@DataType(name = "jsonObject")
public class JsonModelDataType implements DataTypeConverter {

    public static final String JSON_DATA_TYPE_KEY = "CamelJsonModelDataType";

    @Override
    public void convert(Exchange exchange) {
        if (!exchange.hasProperties() || !exchange.getProperties().containsKey(JSON_DATA_TYPE_KEY)) {
            return;
        }

        String type = exchange.getProperty(JSON_DATA_TYPE_KEY, String.class);
        try (JacksonDataFormat dataFormat = new JacksonDataFormat(new ObjectMapper(), Class.forName(type))) {
            Object unmarshalled = dataFormat.unmarshal(exchange, getBodyAsStream(exchange));
            exchange.getMessage().setBody(unmarshalled);
        } catch (Exception e) {
            throw new CamelExecutionException(
                    String.format("Failed to load Json unmarshalling type '%s'", type), exchange, e);
        }
    }

    private InputStream getBodyAsStream(Exchange exchange) throws InvalidPayloadException {
        InputStream bodyStream = exchange.getMessage().getBody(InputStream.class);

        if (bodyStream == null) {
            bodyStream = new ByteArrayInputStream(exchange.getMessage().getMandatoryBody(byte[].class));
        }

        return bodyStream;
    }
}
