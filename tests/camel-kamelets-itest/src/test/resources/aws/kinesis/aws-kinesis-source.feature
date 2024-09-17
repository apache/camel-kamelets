# ---------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ---------------------------------------------------------------------------

Feature: AWS Kinesis - Sink

  Background:
    Given variables
      | aws.kinesis.streamName   | mystream |
      | aws.kinesis.partitionKey | partition-1 |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service KINESIS
    Given start LocalStack container
    Then verify actions waitForLocalStack.groovy

  Scenario: Verify Kinesis events
    # Create AWS-KINESIS client
    Given load to Camel registry amazonKinesisClient.groovy
    # Create binding
    When load Pipe aws-kinesis-source-pipe.yaml
    And Pipe aws-kinesis-source-pipe is available
    And Camel K integration aws-kinesis-source-pipe is running
    And Camel K integration aws-kinesis-source-pipe should print Started aws-kinesis-source-pipe
    # Publish event
    Given variable aws.kinesis.streamData is "Camel rocks!"
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${aws.kinesis.partitionKey}"
    Given send Camel exchange to("aws2-kinesis://${aws.kinesis.streamName}?amazonKinesisClient=#amazonKinesisClient") with body: ${aws.kinesis.streamData}
    # Verify event
    Then Camel K integration aws-kinesis-source-pipe should print ${aws.kinesis.streamData}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe aws-kinesis-source-pipe
    # Stop LocalStack container
    Given stop LocalStack container
