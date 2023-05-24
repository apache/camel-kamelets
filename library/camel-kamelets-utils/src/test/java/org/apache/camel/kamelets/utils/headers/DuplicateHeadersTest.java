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
package org.apache.camel.kamelets.utils.headers;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuplicateHeadersTest {

    private DefaultCamelContext camelContext;

    private DuplicateNamingHeaders processor;

    @BeforeEach
    void setup() {
        camelContext = new DefaultCamelContext();
    }

    @Test
    void shouldDuplicateHeaders() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader("CamelAwsS3Key", "test.txt");
        exchange.getMessage().setHeader("CamelAwsS3BucketName", "kamelets-demo");
        exchange.getMessage().setHeader("my-header", "header");

        processor = new DuplicateNamingHeaders();
        processor.setPrefix("CamelAwsS3");
        processor.setRenamingPrefix("aws.s3.");
        processor.process(exchange);

        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("aws.s3.key"));
        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("aws.s3.bucket.name"));
        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("my-header"));
    }

    @Test
    void shouldDuplicateSelectedHeaders() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setHeader("CamelAwsS3Key", "test.txt");
        exchange.getMessage().setHeader("CamelAwsS3BucketName", "kamelets-demo");
        exchange.getMessage().setHeader("my-header", "header");

        processor = new DuplicateNamingHeaders();
        processor.setPrefix("CamelAwsS3");
        processor.setRenamingPrefix("aws.s3.");
        processor.setMode("filtering");
        processor.setSelectedHeaders("CamelAwsS3Key,CamelAwsS3BucketName");
        processor.process(exchange);

        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("aws.s3.key"));
        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("aws.s3.bucket.name"));
        Assertions.assertTrue(exchange.getMessage().getHeaders().containsKey("my-header"));
    }

}
