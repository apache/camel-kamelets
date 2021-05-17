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
package org.apache.camel.kamelets.utils.transform.kafka;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.util.ObjectHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampRouter {

    public void process(@ExchangeProperty("topicFormat") String topicFormat, @ExchangeProperty("timestampFormat") String timestampFormat, Exchange ex) {
        final Pattern TOPIC = Pattern.compile("[topic]", Pattern.LITERAL);

        final Pattern TIMESTAMP = Pattern.compile("[timestamp]", Pattern.LITERAL);

        final SimpleDateFormat fmt = new SimpleDateFormat(timestampFormat);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        String topicName = ex.getMessage().getHeader(KafkaConstants.TOPIC, String.class);
        long timestamp = ex.getMessage().getHeader(KafkaConstants.TIMESTAMP, Long.class);
        if (ObjectHelper.isNotEmpty(topicName) && ObjectHelper.isNotEmpty(timestamp)) {
            final String formattedTimestamp = fmt.format(new Date(timestamp));
            final String replace1 = TOPIC.matcher(topicFormat).replaceAll(Matcher.quoteReplacement(topicName));
            final String updatedTopic = TIMESTAMP.matcher(replace1).replaceAll(Matcher.quoteReplacement(formattedTimestamp));
            ex.getMessage().setHeader(KafkaConstants.OVERRIDE_TOPIC, updatedTopic);
        }
    }

}
