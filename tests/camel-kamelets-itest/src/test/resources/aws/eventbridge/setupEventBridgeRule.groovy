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

// The event bus and the rule that aws-eventbridge-source receives the events of. The Kamelet adds its own queue
// to the rule; the event pattern of the rule decides which events arrive.

import org.citrusframework.api.actions.testcontainers.aws2.AwsService
import org.citrusframework.testcontainers.aws2.LocalStackContainer
import software.amazon.awssdk.services.eventbridge.EventBridgeClient

LocalStackContainer container = context.getReferenceResolver().resolve(LocalStackContainer.class)
EventBridgeClient eventBridgeClient = container.getClient(AwsService.EVENT_BRIDGE)

eventBridgeClient.createEventBus(b -> b.name('${aws.eventbridge.eventbusName}'))

eventBridgeClient.putRule(b -> b.name('${aws.eventbridge.ruleName}')
        .eventBusName('${aws.eventbridge.eventbusName}')
        .eventPattern('''
                    {
                        "source": ["${aws.eventbridge.eventSource}"],
                        "detail-type": ["Order Shipped"]
                    }
                    '''))
