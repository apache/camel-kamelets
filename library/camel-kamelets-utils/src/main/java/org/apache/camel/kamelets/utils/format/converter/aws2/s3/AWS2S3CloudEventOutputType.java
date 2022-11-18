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

package org.apache.camel.kamelets.utils.format.converter.aws2.s3;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.component.cloudevents.CloudEvent;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Output data type represents AWS S3 get object response as CloudEvent V1. The data type sets Camel specific
 * CloudEvent headers on the exchange.
 */
@DataType(scheme = "aws2-s3", name = "cloudevents")
public class AWS2S3CloudEventOutputType implements DataTypeConverter {

    @Override
    public void convert(Exchange exchange) {
        final Map<String, Object> headers = exchange.getMessage().getHeaders();

        headers.put(CloudEvent.CAMEL_CLOUD_EVENT_TYPE, "kamelet:aws-s3-source");
        headers.put(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE, exchange.getMessage().getHeader(AWS2S3Constants.BUCKET_NAME, String.class));
        headers.put(CloudEvent.CAMEL_CLOUD_EVENT_SUBJECT, exchange.getMessage().getHeader(AWS2S3Constants.KEY, String.class));
        headers.put(CloudEvent.CAMEL_CLOUD_EVENT_TIME, getEventTime(exchange));
        headers.put(CloudEvent.CAMEL_CLOUD_EVENT_DATA_CONTENT_TYPE, exchange.getMessage().getHeader(AWS2S3Constants.CONTENT_TYPE, String.class));

        String encoding = exchange.getMessage().getHeader(AWS2S3Constants.CONTENT_ENCODING, String.class);
        if (encoding != null) {
            headers.put(CloudEvent.CAMEL_CLOUD_EVENT_DATA_CONTENT_ENCODING, encoding);
        }
    }

    private String getEventTime(Exchange exchange) {
        final ZonedDateTime created
                = ZonedDateTime.ofInstant(Instant.ofEpochMilli(exchange.getCreated()), ZoneId.systemDefault());
        return DateTimeFormatter.ISO_INSTANT.format(created);
    }
}
