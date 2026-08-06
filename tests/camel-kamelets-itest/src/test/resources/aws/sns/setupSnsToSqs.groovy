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

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

SnsClient snsClient = context.getReferenceResolver().resolve("snsClient", SnsClient.class)
SqsClient sqsClient = context.getReferenceResolver().resolve("sqsClient", SqsClient.class)

def topicArn = snsClient.createTopic(b -> b.name('${aws.sns.topicName}')).topicArn()

def queueUrl = sqsClient.createQueue(b -> b.queueName('${aws.sqs.queueName}')).queueUrl()
def queueArn = sqsClient.getQueueAttributes(b -> b.queueUrl(queueUrl)
        .attributeNames(QueueAttributeName.QUEUE_ARN))
        .attributes().get(QueueAttributeName.QUEUE_ARN)

snsClient.subscribe(b -> b.topicArn(topicArn).protocol("sqs").endpoint(queueArn)
        .attributes(Collections.singletonMap("RawMessageDelivery", "true")))
