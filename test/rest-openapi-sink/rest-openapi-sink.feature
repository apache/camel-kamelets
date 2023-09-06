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

Feature: REST OpenAPI Kamelet sink

  Background:
    Given HTTP server timeout is 60000 ms
    Given HTTP server "test-service"
    Given variable petId is "1000"
    Given load variable pet.json

  Scenario: Create Http server
    Given create Kubernetes service test-service

  Scenario: Create Kamelet binding for addPet
    Given variable operation is "addPet"
    Then load Pipe rest-openapi-sink-pipe.yaml

  Scenario: Provide OpenAPI specification to Camel K integration
    Given load variable openapi from openapi.json
    When receive GET /petstore/openapi.json
    Then HTTP request header Content-Type is "application/json"
    Then HTTP response body: ${openapi}
    Then send HTTP 200 OK

  Scenario: Verify proper addPet request message sent
    Given expect HTTP request body: ${pet}
    When receive POST /petstore/pet
    And send HTTP 201 CREATED

  Scenario: Remove resources
    Given delete Pipe rest-openapi-sink-pipe

  Scenario: Create Kamelet binding for deletePet
    Given variable operation is "deletePet"
    When load Pipe rest-openapi-sink-pipe.yaml

  Scenario: Provide OpenAPI specification to Camel K integration
    Given load variable openapi from openapi.json
    When receive GET /petstore/openapi.json
    Then HTTP request header Content-Type is "application/json"
    Then HTTP response body: ${openapi}
    Then send HTTP 200 OK

  Scenario: Verify proper deletePet request message sent
    When receive DELETE /petstore/pet/${petId}
    And send HTTP 204 NO_CONTENT

  Scenario: Remove resources
    Given delete Pipe rest-openapi-sink-pipe
    And delete Kubernetes service test-service
