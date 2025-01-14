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

Feature: Kafka Timestamp Router

  Background:
    Given variables
      | securityProtocol | PLAINTEXT |
      | topicName        | my-topic |
      | timestamp        | yaks:unixTimestamp()000 |
      | topic            | ${topicName}_yaks:currentDate('YYYY-MM-dd') |
      | message          | Camel K rocks! |
      | user             | redpanda |
      | password         | admin |
    Given Kafka topic: ${topic}
    Given Kafka topic partition: 0

  Scenario: Create infrastructure
    Given start Redpanda container

  Scenario: Create Pipe
    When load Pipe timestamp-router-pipe.yaml
    Then Camel K integration timestamp-router-pipe should be running
    Then Camel K integration timestamp-router-pipe should print Routes startup

  Scenario: Receive message on Kafka topic and verify sink output
    Given new Kafka connection
      | url           | ${CITRUS_TESTCONTAINERS_REDPANDA_LOCAL_BOOTSTRAP_SERVERS} |
      | consumerGroup | consumer-1                                              |
    Given URL: yaks:resolveURL('timestamp-router-pipe',8080)
    Given HTTP request query parameter kafka.TOPIC="${topicName}"
    Given HTTP request query parameter kafka.TIMESTAMP="${timestamp}"
    Given HTTP request query parameter message="yaks:urlEncode(${message})"
    Given HTTP request fork mode is enabled
    When send GET /messages
    Then receive Kafka message with body: ${message}
    And receive HTTP 200 OK

  Scenario: Remove resources
    Given delete Pipe timestamp-router-pipe
    And stop Redpanda container
