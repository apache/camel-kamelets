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

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.kamelets.utils.format.converter.utils.CloudEvents;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DataTypeTransformer;
import org.apache.camel.spi.Transformer;

/**
 * Output data type represents the CloudEvent V1 Http binding. The data type reads Camel specific
 * CloudEvent headers and transforms these to Http headers according to the CloudEvents Http binding specification.
 *
 * By default, sets the Http content type header to application/json when not set explicitly.
 */
@DataTypeTransformer(name = "http:application-cloudevents")
public class HttpCloudEventOutputType extends Transformer {

    @Override
    public void transform(Message message, DataType fromType, DataType toType) {
        final Map<String, Object> headers = message.getHeaders();

        headers.put("ce-id", message.getExchange().getExchangeId());
        headers.put("ce-specversion", headers.getOrDefault(CloudEvents.CAMEL_CLOUD_EVENT_VERSION, "1.0"));
        headers.put("ce-type", headers.getOrDefault(CloudEvents.CAMEL_CLOUD_EVENT_TYPE, "org.apache.camel.event"));
        headers.put("ce-source", headers.getOrDefault(CloudEvents.CAMEL_CLOUD_EVENT_SOURCE, "org.apache.camel"));

        if (headers.containsKey(CloudEvents.CAMEL_CLOUD_EVENT_SUBJECT)) {
            headers.put("ce-subject", headers.get(CloudEvents.CAMEL_CLOUD_EVENT_SUBJECT));
        }

        headers.put("ce-time", headers.getOrDefault(CloudEvents.CAMEL_CLOUD_EVENT_TIME, CloudEvents.getEventTime(message.getExchange())));
        headers.put(Exchange.CONTENT_TYPE, headers.getOrDefault(CloudEvents.CAMEL_CLOUD_EVENT_CONTENT_TYPE, "application/json"));
    }
}
