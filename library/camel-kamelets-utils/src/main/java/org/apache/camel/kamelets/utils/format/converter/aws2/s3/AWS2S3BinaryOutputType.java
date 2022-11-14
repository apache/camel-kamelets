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

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Binary output type.
 */
@DataType(scheme = "aws2-s3", name = "binary")
public class AWS2S3BinaryOutputType implements DataTypeConverter {

    @Override
    public void convert(Exchange exchange) {
        if (exchange.getMessage().getBody() instanceof byte[]) {
            return;
        }

        try {
            InputStream is = exchange.getMessage().getBody(InputStream.class);
            if (is != null) {
                exchange.getMessage().setBody(IoUtils.toByteArray(is));
                return;
            }

            // Use default Camel converter utils to convert body to byte[]
            exchange.getMessage().setBody(exchange.getMessage().getMandatoryBody(byte[].class));
        } catch (IOException | InvalidPayloadException e) {
            throw new CamelExecutionException("Failed to convert AWS S3 body to byte[]", exchange, e);
        }
    }
}
