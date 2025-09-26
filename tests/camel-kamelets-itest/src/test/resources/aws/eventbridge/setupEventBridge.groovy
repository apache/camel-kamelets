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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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

import org.citrusframework.actions.testcontainers.aws2.AwsService
import org.citrusframework.testcontainers.aws2.LocalStackContainer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.PutRuleResponse
import software.amazon.awssdk.services.eventbridge.model.Target
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

LocalStackContainer container = context.getReferenceResolver().resolve(LocalStackContainer.class)
container.withServices(AwsService.EVENT_BRIDGE, AwsService.S3, AwsService.SQS)

EventBridgeClient eventBridgeClient = container.getClient(AwsService.EVENT_BRIDGE)

// Add an EventBridge rule on the event
PutRuleResponse putRuleResponse = eventBridgeClient.putRule(b -> b.name("events-cdc")
        .eventBusName('${aws.eventbridge.eventbusName}')
        .eventPattern('''
                    {
                        "source": ["${aws.eventbridge.eventSourcePrefix}${aws.eventbridge.eventSource}"],
                        "detail-type": ["${aws.eventbridge.detailType}"]
                    }
                    '''))

SqsClient sqsClient = SqsClient
        .builder()
        .endpointOverride(URI.create('${CITRUS_TESTCONTAINERS_LOCALSTACK_SERVICE_URL}'))
        .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        '${CITRUS_TESTCONTAINERS_LOCALSTACK_ACCESS_KEY}',
                        '${CITRUS_TESTCONTAINERS_LOCALSTACK_SECRET_KEY}')
        ))
        .region(Region.of('${CITRUS_TESTCONTAINERS_LOCALSTACK_REGION}'))
        .build()

// Create SQS queue acting as an EventBridge notification endpoint
CreateQueueResponse createQueueResponse = sqsClient.createQueue(b -> b.queueName('${aws.sqs.queueName}'))

// Modify access policy for the queue just created, so EventBridge rule is allowed to send messages
String queueUrl = createQueueResponse.queueUrl();
String queueArn = 'arn:aws:sqs:%s:000000000000:${aws.sqs.queueName}'.formatted(container.getRegion())

sqsClient.setQueueAttributes(b -> b.queueUrl(queueUrl).attributes(Collections.singletonMap(QueueAttributeName.POLICY, """
                {
                    "Version": "2012-10-17",
                    "Id": "%s/SQSDefaultPolicy",
                    "Statement":
                    [
                        {
                            "Sid": "EventsToMyQueue",
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "events.amazonaws.com"
                            },
                            "Action": "sqs:SendMessage",
                            "Resource": "%s",
                            "Condition": {
                                "ArnEquals": {
                                    "aws:SourceArn": "%s"
                                }
                            }
                        }
                    ]
                }
                """.formatted(queueArn, queueArn, putRuleResponse.ruleArn()))))

// Add a target for EventBridge rule which will be the SQS Queue just created
eventBridgeClient.putTargets(b -> b.rule("events-cdc")
        .eventBusName('${aws.eventbridge.eventbusName}')
        .targets(Target.builder().id("sqs-sub").arn(queueArn).build()))
