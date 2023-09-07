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
@ignored
Feature: Salesforce Kamelet

  Background:
    Given variable client_id is "${clientId}"
    Given variable userName is "${userName}"
    Given variable password is "${password}"
    Given variable client_secret is "${clientSecret}"

    Given variable token_request is "grant_type=password&client_id=${client_id}&client_secret=${client_secret}&username=${userName}&password=${password}"
    Given URL: https://login.salesforce.com
    And HTTP request header Content-Type="application/x-www-form-urlencoded"
    And HTTP request body: ${token_request}
    When send POST /services/oauth2/token
    Then verify HTTP response expression: $.instance_url="@variable(instance_url)@"
    And verify HTTP response expression: $.access_token="@variable(access_token)@"
    And receive HTTP 200 OK

  Scenario: Interact with SalesForce
    Given load KameletBinding timer-to-salesforce-binding.yaml
    Then Camel K integration timer-to-salesforce-binding should be running
    And Camel K integration timer-to-salesforce-binding should print Routes startup

    Given URL: ${instance_url}
    Then HTTP request header Authorization="Bearer ${access_token}"
    And HTTP request header Content-Type="application/json"
    And HTTP request query parameter q="citrus:urlEncode('SELECT Id FROM Contact LIMIT 1')"
    When send GET /services/data/v50.0/query/
    Then verify HTTP response expression: $.records[0].Id="@variable(id)@"
    And receive HTTP 200 OK

    And load KameletBinding direct-to-salesforce-update-binding.yaml
    Then Camel K integration direct-to-salesforce-update-binding should be running
    And Camel K integration direct-to-salesforce-update-binding should print Routes startup
    Then sleep 5000 ms
    Given URL: ${instance_url}
    Then HTTP request header Authorization="Bearer ${access_token}"
    And HTTP request header Content-Type="application/json"
    Given variable query is "SELECT Phone FROM Contact WHERE Id = '${id}'"
    And HTTP request query parameter q="citrus:urlEncode('${query}')"
    When send GET /services/data/v50.0/query/
    Then verify HTTP response expression: $.records[0].Phone="1234567890"
    And receive HTTP 200 OK

    When load KameletBinding direct-to-salesforce-delete-binding.yaml
    Then Camel K integration direct-to-salesforce-delete-binding should be running
    And Camel K integration direct-to-salesforce-delete-binding should print Routes startup
    Then sleep 5000 ms

    Given URL: ${instance_url}
    Given HTTP request header Authorization="Bearer ${access_token}"
    And HTTP request header Content-Type="application/json"
    And HTTP request query parameter q="citrus:urlEncode('${query}')"
    When send GET /services/data/v50.0/query/
    Then verify HTTP response expression: $.totalSize="0"
    And receive HTTP 200 OK

  Scenario: Remove Camel-K resources
    Given delete KameletBinding timer-to-salesforce-binding
    And delete KameletBinding direct-to-salesforce-update-binding
    And delete KameletBinding direct-to-salesforce-delete-binding
