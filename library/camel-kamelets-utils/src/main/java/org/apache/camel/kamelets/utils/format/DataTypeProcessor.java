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

package org.apache.camel.kamelets.utils.format;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Processor applies data type conversion based on given format name. Searches for matching data type converter
 * with given component scheme and format name.
 */
public class DataTypeProcessor implements Processor, CamelContextAware {

    private CamelContext camelContext;

    @BeanInject
    private DefaultDataTypeRegistry dataTypeRegistry;

    private String scheme;
    private String format;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (format == null || format.isEmpty()) {
            return;
        }

        dataTypeRegistry.lookup(scheme, format)
                        .ifPresent(converter -> converter.convert(exchange));
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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
