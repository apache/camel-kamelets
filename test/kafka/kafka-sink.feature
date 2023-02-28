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
      | securityProtocol | PLAINTEXT |
      | topic            | my-topic |
      | message          | Camel K rocks! |
    Given Kafka topic: ${topic}
    Given Kafka topic partition: 0

  Scenario: Create infrastructure
    Given start Redpanda container

  Scenario: Create Kamelet binding
    When load KameletBinding kafka-sink-binding.yaml
    Then Camel K integration kafka-sink-binding should be running

  Scenario: Verify Kafka sink output
    Given Kafka connection
      | url | ${YAKS_TESTCONTAINERS_REDPANDA_LOCAL_BOOTSTRAP_SERVERS} |
    Then receive Kafka message with body: ${message}

  Scenario: Remove resources
    Given delete KameletBinding kafka-sink-binding
    And stop Redpanda container
