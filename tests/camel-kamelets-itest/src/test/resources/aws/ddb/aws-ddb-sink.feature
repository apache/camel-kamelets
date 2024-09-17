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

Feature: AWS DDB Sink

  Background:
    Given variables
      | maxRetryAttempts   | 20 |
      | timer.source.period  | 10000 |
      | aws.ddb.streams      | false |
      | aws.ddb.tableName    | movies |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container
    Then verify actions waitForLocalStack.groovy

  Scenario: Create AWS DDB client
    # Create AWS-DDB client
    Given load to Camel registry amazonDDBClient.groovy
    # Verify empty AWS-DDB
    Given variables
      | aws.ddb.items      | [] |
    Then apply actions verifyItems.groovy

  Scenario: DeleteItem
    Given variables
      | aws.ddb.operation  | DeleteItem |
      | aws.ddb.item.id    | yaks:randomNumber(4) |
      | aws.ddb.item.year  | 1985 |
      | aws.ddb.item.title | Back to the future |
      | aws.ddb.json.data  | {"id": ${aws.ddb.item.id}} |
    Given run script putItem.groovy
    Given variables
      | aws.ddb.items    | [[year:AttributeValue(N=${aws.ddb.item.year}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title})]] |
    # Create item on AWS-DDB
    Then apply actions verifyItems.groovy
    # Create binding
    When load Pipe aws-ddb-sink-pipe.yaml
    And Pipe aws-ddb-sink-pipe is available
    And Camel K integration aws-ddb-sink-pipe is running
    And Camel K integration aws-ddb-sink-pipe should print Started aws-ddb-sink-pipe
    # Verify Kamelet sink
    Given variables
      | aws.ddb.items    | [] |
    Then apply actions verifyItems.groovy
    # Remove Camel K resources
    Given delete Pipe aws-ddb-sink-pipe

  Scenario: PutItem
    Given variables
      | aws.ddb.operation  | PutItem |
      | aws.ddb.item.id    | yaks:randomNumber(4) |
      | aws.ddb.item.year  | 1977 |
      | aws.ddb.item.title | Star Wars IV |
      | aws.ddb.json.data  | { "id":${aws.ddb.item.id}, "year":${aws.ddb.item.year}, "title":"${aws.ddb.item.title}" } |
    When load Pipe aws-ddb-sink-pipe.yaml
    And Pipe aws-ddb-sink-pipe is available
    And Camel K integration aws-ddb-sink-pipe is running
    And Camel K integration aws-ddb-sink-pipe should print Started aws-ddb-sink-pipe
    # Verify Kamelet sink
    Given variables
      | aws.ddb.item | [year:AttributeValue(N=${aws.ddb.item.year}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title})] |
    Then apply actions getItem.groovy
    # Remove Camel K binding
    Given delete Pipe aws-ddb-sink-pipe

  Scenario: UpdateItem
    Given variables
      | aws.ddb.operation      | UpdateItem |
      | aws.ddb.item.id        | yaks:randomNumber(4) |
      | aws.ddb.item.year      | 1933 |
      | aws.ddb.item.title     | King Kong |
      | aws.ddb.item.title.new | King Kong - Historical |
      | aws.ddb.item.directors | ["Merian C. Cooper", "Ernest B. Schoedsack"] |
      | aws.ddb.json.data      | { "key": {"id": ${aws.ddb.item.id}}, "item": {"title": "${aws.ddb.item.title.new}", "year": ${aws.ddb.item.year}, "directors": ${aws.ddb.item.directors}} } |
    Given run script putItem.groovy
    Given variables
      | aws.ddb.item | [year:AttributeValue(N=${aws.ddb.item.year}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title})] |
    Then apply actions getItem.groovy
    # Create binding
    When load Pipe aws-ddb-sink-pipe.yaml
    And Pipe aws-ddb-sink-pipe is available
    And Camel K integration aws-ddb-sink-pipe is running
    And Camel K integration aws-ddb-sink-pipe should print Started aws-ddb-sink-pipe
    # Verify Kamelet sink
    Given variables
      | maxRetryAttempts       | 200 |
      | aws.ddb.item.directors | [Ernest B. Schoedsack, Merian C. Cooper] |
      | aws.ddb.item           | [year:AttributeValue(N=${aws.ddb.item.year}), directors:AttributeValue(SS=${aws.ddb.item.directors}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title.new})] |
    Then apply actions getItem.groovy
    # Remove Camel K resources
    Given delete Pipe aws-ddb-sink-pipe

  Scenario: Remove resources
    # Stop LocalStack container
    Given stop LocalStack container
