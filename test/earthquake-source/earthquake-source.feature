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

Feature: Kamelet earthquake-source

  Background:
    Given HTTP server timeout is 15000 ms
    Given HTTP server "test-service"

  Scenario: Create Http server
    Given create Kubernetes service test-service with target port 8080

  Scenario: Create Kamelet binding
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000 |
    When bind Kamelet earthquake-source to uri http://test-service.${YAKS_NAMESPACE}/test
    And create KameletBinding earthquake-source-uri
    Then KameletBinding earthquake-source-uri should be available
    Then Camel K integration earthquake-source-uri should be running
    And Camel K integration earthquake-source-uri should print Routes startup

  Scenario: Verify binding
    Given expect HTTP request header: Content-Type="application/json;charset=UTF-8"
    When receive POST /test
    Then send HTTP 200 OK

  Scenario: Remove Camel K resources
    Given delete KameletBinding earthquake-source-uri
    And delete Kubernetes service test-service
