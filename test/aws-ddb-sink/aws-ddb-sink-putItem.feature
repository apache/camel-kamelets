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
    Given Kamelet aws-ddb-sink is available
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given variables
      | timer.source.period  | 10000 |
      | aws.ddb.operation    | PutItem |
      | aws.ddb.tableName    | movies |
      | aws.ddb.item.id      | 1 |
      | aws.ddb.item.year    | 1977 |
      | aws.ddb.item.title   | Star Wars IV |
      | aws.ddb.json.data    | { "id":${aws.ddb.item.id}, "year":${aws.ddb.item.year}, "title":"${aws.ddb.item.title}" } |
      | aws.ddb.items        | [{year=AttributeValue(N=${aws.ddb.item.year}), id=AttributeValue(N=${aws.ddb.item.id}), title=AttributeValue(S=${aws.ddb.item.title})}] |

  Scenario: Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create AWS-DDB client
    Given New global Camel context
    Given load to Camel registry amazonDDBClient.groovy

  Scenario: Create AWS-DDB Kamelet sink binding
    When load KameletBinding aws-ddb-sink-binding.yaml
    And KameletBinding aws-ddb-sink-binding is available
    And Camel K integration aws-ddb-sink-binding is running
    And Camel K integration aws-ddb-sink-binding should print Routes startup
    Then sleep 10sec

  Scenario: Verify Kamelet sink
    Then run script verifyItems.groovy

  Scenario: Remove Camel K resources
    Given delete KameletBinding aws-ddb-sink-binding

  Scenario: Stop container
    Given stop LocalStack container
