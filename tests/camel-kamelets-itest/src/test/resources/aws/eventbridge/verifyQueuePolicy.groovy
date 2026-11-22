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

// The queue that aws-eventbridge-source created for the rule has a policy that lets only that rule send to it:
// an aws:SourceArn condition with the ARN of the rule.

import org.citrusframework.api.actions.testcontainers.aws2.AwsService
import org.citrusframework.testcontainers.aws2.LocalStackContainer
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

LocalStackContainer container = context.getReferenceResolver().resolve(LocalStackContainer.class)
EventBridgeClient eventBridgeClient = container.getClient(AwsService.EVENT_BRIDGE)
SqsClient sqsClient = container.getClient(AwsService.SQS)

def ruleArn = eventBridgeClient.describeRule(b -> b.name('${aws.eventbridge.ruleName}')
        .eventBusName('${aws.eventbridge.eventbusName}')).arn()

def queueUrls = sqsClient.listQueues(b -> b.queueNamePrefix('camel-eventbridge-${aws.eventbridge.ruleName}-')).queueUrls()
if (queueUrls.size() != 1) {
    throw new IllegalStateException('Expected one queue for the rule but got: ' + queueUrls)
}
def policy = sqsClient.getQueueAttributes(b -> b.queueUrl(queueUrls[0]).attributeNames(QueueAttributeName.POLICY))
        .attributes().get(QueueAttributeName.POLICY)
if (policy == null || !policy.contains('"aws:SourceArn":"' + ruleArn + '"')) {
    throw new IllegalStateException('The queue policy does not limit the senders to the rule ' + ruleArn + ': ' + policy)
}
