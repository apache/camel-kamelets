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
      | timer.source.period      | 10000 |
      | aws.kinesis.streamName   | mystream |
      | aws.kinesis.partitionKey | partition-1 |
      | aws.kinesis.message      | Camel rocks! |
      | aws.kinesis.json.data    | { "message":"${aws.kinesis.message}" } |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service KINESIS
    Given start LocalStack container
    Then verify actions waitForLocalStack.groovy

  Scenario: Verify Kinesis events
    # Create AWS-KINESIS client
    Given load to Camel registry amazonKinesisClient.groovy
    # Create binding
    When load Pipe aws-kinesis-sink-pipe.yaml
    And Pipe aws-kinesis-sink-pipe is available
    And Camel K integration aws-kinesis-sink-pipe is running
    And Camel K integration aws-kinesis-sink-pipe should print Started aws-kinesis-sink-pipe
    # Create vent listener
    Given Camel route kinesisEventListener.groovy
    """
    from("aws2-kinesis://${aws.kinesis.streamName}?amazonKinesisClient=#amazonKinesisClient")
       .convertBodyTo(String.class)
       .to("seda:result")
    """
    # Verify event
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${aws.kinesis.partitionKey}"
    Then receive Camel exchange from("seda:result") with body: ${aws.kinesis.json.data}
    # Stop event listener
    Then stop Camel route kinesisEventListener

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe aws-kinesis-sink-pipe
    # Stop LocalStack container
    Given stop LocalStack container
