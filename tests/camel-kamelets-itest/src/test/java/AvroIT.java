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

import java.util.stream.Stream;

import org.citrusframework.common.TestLoader;
import org.citrusframework.container.SequenceBeforeTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.junit.jupiter.CitrusTestFactory;
import org.citrusframework.junit.jupiter.CitrusTestFactorySupport;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.DynamicTest;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;

@CitrusSupport
public class AvroIT {

    private final int avroWebhookPort = SocketUtils.findAvailableTcpPort();

    @BindToRegistry
    public SequenceBeforeTest beforeAvro() {
        return new SequenceBeforeTest.Builder().onTests("avro-*").actions(
                createVariables().variable("avro.webhook.server.port", String.valueOf(avroWebhookPort))
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> avro() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("avro");
    }

}
