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

Feature: Kafka Kamelet source

  Background:
    Given variables
      | securityProtocol   | PLAINTEXT |
      | deserializeHeaders | true |
      | topic              | my-topic |
      | source             | Kafka Kamelet source |
      | message            | Camel K rocks! |
      | user               | redpanda |
      | password           | admin |
    Given Kafka topic: ${topic}
    Given Kafka topic partition: 0
    Given HTTP server timeout is 15000 ms
    Given HTTP server "test-service"

  Scenario: Create infrastructure
    Given start Redpanda container
    Given create Kubernetes service test-service with target port 8080
    Given purge endpoint test-service

  Scenario: Create Kamelet binding
    When load Pipe kafka-source-pipe.yaml
    Then Camel K integration kafka-source-pipe should be running
    And Camel K integration kafka-source-pipe should print Subscribed to topic(s): ${topic}
    And sleep 10sec

  Scenario: Send message to Kafka topic and verify sink output
    Given variable key is "citrus:randomNumber(4)"
    Given Kafka connection
      | url | ${YAKS_TESTCONTAINERS_REDPANDA_LOCAL_BOOTSTRAP_SERVERS} |
    Given Kafka message key: ${key}
    When send Kafka message with body and headers: ${message}
      | event-source | ${source} |
    Then expect HTTP request body: ${message}
    Then expect HTTP request headers
      | event-source    | ${source} |
      | kafka.TOPIC     | ${topic}  |
      | kafka.KEY       | ${key}    |
      | kafka.PARTITION | 0         |
    And receive POST /result
    And send HTTP 200 OK

  Scenario: Remove resources
    Given delete Pipe kafka-source-pipe
    And delete Kubernetes service test-service
    And stop Redpanda container
    And stop HTTP server
