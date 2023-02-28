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
@experimental
Feature: AWS DDB Sink - UpdateItem

  Background:
    Given variables
      | timer.source.period    | 10000 |
      | aws.ddb.operation      | UpdateItem |
      | aws.ddb.tableName      | movies |
      | aws.ddb.item.id        | 1 |
      | aws.ddb.item.year      | 1933 |
      | aws.ddb.item.title     | King Kong |
      | aws.ddb.item.title.new | King Kong - Historical |
      | aws.ddb.item.directors | ["Merian C. Cooper", "Ernest B. Schoedsack"] |
      | aws.ddb.json.data      | { "key": {"id": ${aws.ddb.item.id}}, "item": {"title": "${aws.ddb.item.title.new}", "year": ${aws.ddb.item.year}, "directors": ${aws.ddb.item.directors}} } |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container
    # Create AWS-DDB client
    Given New global Camel context
    Given load to Camel registry amazonDDBClient.groovy

  Scenario: Create item on AWS-DDB
    Given run script putItem.groovy
    Given variables
      | maxRetryAttempts  | 20 |
      | aws.ddb.items | [[year:AttributeValue(N=${aws.ddb.item.year}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title})]] |
    Then apply actions verifyItems.groovy

  Scenario: Verify AWS-DDB Kamelet sink binding
    # Create binding
    When load KameletBinding aws-ddb-sink-binding.yaml
    And KameletBinding aws-ddb-sink-binding is available
    And Camel K integration aws-ddb-sink-binding is running
    And Camel K integration aws-ddb-sink-binding should print Started aws-ddb-sink-binding
    # Verify Kamelet sink
    Given variables
      | maxRetryAttempts  | 200 |
      | aws.ddb.item.directors | [Ernest B. Schoedsack, Merian C. Cooper] |
      | aws.ddb.items | [[year:AttributeValue(N=${aws.ddb.item.year}), directors:AttributeValue(SS=${aws.ddb.item.directors}), id:AttributeValue(N=${aws.ddb.item.id}), title:AttributeValue(S=${aws.ddb.item.title.new})]] |
    Then apply actions verifyItems.groovy

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete KameletBinding aws-ddb-sink-binding
    # Stop LocalStack container
    Given stop LocalStack container
