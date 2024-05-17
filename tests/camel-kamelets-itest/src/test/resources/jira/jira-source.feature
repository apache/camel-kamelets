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

Feature: Jira Kamelet - Source

  Background:
    Given variables
      | jira.project.id    | 10001 |
      | jira.issue.id      | 10001 |
      | jira.issue.key     | CAMEL-1 |
      | jira.issue.summary | New bug, citrus:randomString(10) |
      | jira.username      | yaks   |
      | jira.password      | secr3t |
      | jira.jql           | assignee=yaks |
    Given HTTP server timeout is 120000 ms
    Given HTTP server "jira-service"

  Scenario: Create Http server
    Given create Kubernetes service jira-service
    Given purge endpoint jira-service

  Scenario: Verify Jira events
    # Create binding
    Given load Pipe jira-source-pipe.yaml
    # Verify latest issue request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    And expect HTTP request query parameter jql="yaks:urlEncode(${jira.jql})+ORDER+BY+key+desc"
    And expect HTTP request query parameter maxResults="1"
    When receive GET /rest/api/latest/search
    Then HTTP response header: Content-Type="application/json"
    And HTTP response body
    """
    {
        "expand": "",
        "startAt": 0,
        "maxResults": 1,
        "total": 1,
        "issues": [
            {
                "expand": "",
                "id": "${jira.issue.id}",
                "key": "${jira.issue.key}",
                "self": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.id}",
                "transitions": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.key}/transitions"
            }
        ]
    }
    """
    Then send HTTP 200 OK
    # Verify search request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    And expect HTTP request query parameter jql="yaks:urlEncode(${jira.jql})+ORDER+BY+key+desc"
    And expect HTTP request query parameter maxResults="50"
    And expect HTTP request query parameter expand="schema%2Cnames"
    When receive GET /rest/api/latest/search
    Then HTTP response header: Content-Type="application/json"
    And HTTP response body
    """
    {
        "expand": "schema,names",
        "startAt": 0,
        "maxResults": 50,
        "total": 1,
        "names": {
            "custom": "${jira.issue.key}"
        },
        "schema": {
            "custom": {
                "type": "string"
            }
        },
        "issues": [
            {
                "expand": "",
                "id": "${jira.issue.id}",
                "key": "${jira.issue.key}",
                "self": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.id}",
                "transitions": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.key}/transitions",
                "fields": {
                    "project": {
                        "self": "yaks:resolveURL('jira-service')/rest/api/latest/project/${jira.project.id}",
                        "id": "${jira.project.id}",
                        "key": "CAMEL"
                    },
                    "summary": "${jira.issue.summary}",
                    "custom": "${jira.issue.key}",
                    "issuetype": {
                        "self": "yaks:resolveURL('jira-service')/rest/api/latest/issuetype/3",
                        "id": "3",
                        "description": "A task that needs to be done.",
                        "iconUrl": "yaks:resolveURL('jira-service')/images/icons/task.gif",
                        "name": "Task",
                        "subtask": false
                    },
                    "created": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
                    "updated": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
                    "status": {
                        "self": "yaks:resolveURL('jira-service')/rest/api/latest/status/1",
                        "id": 1,
                        "description": "Open task.",
                        "iconUrl": "yaks:resolveURL('jira-service')/images/icons/status_1.gif",
                        "name": "Open"
                    }
                }
            }
        ]
    }
    """
    Then send HTTP 200 OK
    And Camel K integration jira-source-pipe should print ${jira.issue.summary}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe jira-source-pipe
    Given delete Kubernetes service jira-service
    And stop server component jira-service
