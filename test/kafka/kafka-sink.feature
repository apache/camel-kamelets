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

Feature: Kafka Kamelet sink

  Background:
    Given variable user is ""
    Given variable password is ""
    Given variables
      | bootstrap.server.host     | my-cluster-kafka-bootstrap |
      | bootstrap.server.port     | 9092 |
      | securityProtocol          | PLAINTEXT |
      | topic                     | my-topic |
      | message                   | Camel K rocks! |
    Given Kafka topic: ${topic}
    Given Kafka topic partition: 0

  Scenario: Create Kamelet binding
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    When load KameletBinding kafka-sink-test.yaml
    Then Camel K integration kafka-sink-test should be running

  Scenario: Receive message on Kafka topic and verify sink output
    Given Kafka connection
      | url         | ${bootstrap.server.host}.${YAKS_NAMESPACE}:${bootstrap.server.port} |
    Then receive Kafka message with body: ${message}

  Scenario: Remove resources
    Given delete KameletBinding kafka-sink-test
