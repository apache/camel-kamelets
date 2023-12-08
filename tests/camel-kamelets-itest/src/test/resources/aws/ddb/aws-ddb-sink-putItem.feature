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

Feature: AWS DDB Sink - PutItem

  Background:
    Given variables
      | timer.source.period  | 10000 |
      | aws.ddb.operation    | PutItem |
      | aws.ddb.tableName    | movies |
      | aws.ddb.item.id      | 1 |
      | aws.ddb.item.year    | 1977 |
      | aws.ddb.item.title   | Star Wars IV |
      | aws.ddb.json.data    | { "id":${aws.ddb.item.id}, "year":${aws.ddb.item.year}, "title":"${aws.ddb.item.title}" } |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container

  Scenario: Verify AWS-DDB Kamelet sink binding
    Given variables
      | maxRetryAttempts  | 20 |
      | aws.ddb.items | [] |
    # Create AWS-DDB client
    Given load to Camel registry amazonDDBClient.groovy
    # Verify empty items on AWS-DDB
    Then apply actions verifyItems.groovy

    # Create binding
    When load Pipe aws-ddb-sink-pipe.yaml
    And Pipe aws-ddb-sink-pipe is available
    And Camel K integration aws-ddb-sink-pipe is running
    And Camel K integration aws-ddb-sink-pipe should print Started aws-ddb-sink-pipe
    # Verify Kamelet sink
    Given variables
      | maxRetryAttempts  | 20 |
      | aws.ddb.items     | [[year:AttributeValue(N=${aws.ddb.item.year}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title})]] |
    Then apply actions verifyItems.groovy

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe aws-ddb-sink-pipe
    # Stop LocalStack container
    Given stop LocalStack container
