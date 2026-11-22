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

// The queue that aws-eventbridge-source keeps (deleteQueueOnShutdown false): there is exactly one queue for the
// rule, it is still a target of the rule, and, when the variable aws.eventbridge.keptQueue is set, it is that
// queue. Sets aws.eventbridge.keptQueue to its URL. With putEvent set to true, it then puts an event that the
// rule matches on the bus.

import org.citrusframework.api.actions.testcontainers.aws2.AwsService
import org.citrusframework.testcontainers.aws2.LocalStackContainer
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

LocalStackContainer container = context.getReferenceResolver().resolve(LocalStackContainer.class)
EventBridgeClient eventBridgeClient = container.getClient(AwsService.EVENT_BRIDGE)
SqsClient sqsClient = container.getClient(AwsService.SQS)

def queueUrls = sqsClient.listQueues(b -> b.queueNamePrefix('camel-eventbridge-${aws.eventbridge.ruleName}-')).queueUrls()
if (queueUrls.size() != 1) {
    throw new IllegalStateException('Expected one queue for the rule but got: ' + queueUrls)
}
def queueUrl = queueUrls[0]
if (context.getVariables().containsKey('aws.eventbridge.keptQueue') && context.getVariable('aws.eventbridge.keptQueue') != queueUrl) {
    throw new IllegalStateException('Expected the queue ' + context.getVariable('aws.eventbridge.keptQueue') + ' but got: ' + queueUrl)
}
context.setVariable('aws.eventbridge.keptQueue', queueUrl)

def queueArn = sqsClient.getQueueAttributes(b -> b.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
        .attributes().get(QueueAttributeName.QUEUE_ARN)
def targets = eventBridgeClient.listTargetsByRule(b -> b.rule('${aws.eventbridge.ruleName}')
        .eventBusName('${aws.eventbridge.eventbusName}')).targets().collect { it.arn() }
if (targets != [queueArn]) {
    throw new IllegalStateException('Expected the target ' + queueArn + ' on the rule but got: ' + targets)
}

if (context.getVariables().containsKey('putEvent') && context.getVariable('putEvent') == 'true') {
    def response = eventBridgeClient.putEvents(b -> b.entries(PutEventsRequestEntry.builder()
            .eventBusName('${aws.eventbridge.eventbusName}')
            .source('${aws.eventbridge.eventSource}')
            .detailType('Order Shipped')
            .detail('{"orderId": "A-2"}')
            .build()))
    if (response.failedEntryCount() != 0) {
        throw new IllegalStateException('Failed to put the event: ' + response.entries())
    }
}
