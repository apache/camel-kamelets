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

Feature: Jira Kamelet - Sink

  Background:
    Given variables
      | timer.source.period | 10000 |
      | jira.project.key    | CAMEL |
      | jira.project.id     | 10001 |
      | jira.issue.id       | 10001 |
      | jira.issue.key      | CAMEL-10001 |
      | jira.issue.summary  | New bug, citrus:randomString(10) |
      | jira.issue.assignee | Superman |
      | jira.issue.type     | Bug |
      | jira.issue.description | Sample bug |
      | jira.username       | yaks   |
      | jira.password       | secr3t |
    Given HTTP server timeout is 15000 ms
    Given HTTP server "jira-service"

  Scenario: Create Http server
    Given create Kubernetes service jira-service
    Given purge endpoint jira-service

  Scenario: Verify Jira events
    # Create binding
    Given load Pipe jira-add-issue-sink-pipe.yaml
    # Verify issue type request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    When receive GET /rest/api/latest/issuetype
    Then HTTP response header: Content-Type="application/json"
    And HTTP response body
    """
    [
        {
            "self": "yaks:resolveURL('jira-service')/rest/api/latest/issueType/1",
            "id": "1",
            "description": "A problem with the software.",
            "iconUrl": "yaks:resolveURL('jira-service')/images/icons/issuetypes/bug.png",
            "name": "Bug",
            "subtask": false,
            "avatarId": 2
        },
        {
            "self": "yaks:resolveURL('jira-service')/rest/api/latest/issueType/3",
            "id": "3",
            "description": "A task that needs to be done.",
            "iconUrl": "yaks:resolveURL('jira-service')/images/icons/issuetypes/task.png",
            "name": "Task",
            "subtask": false,
            "avatarId": 1
        }
    ]
    """
    Then send HTTP 200 OK
    # Verify add issue request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    Then expect HTTP request body
    """
    {
        "fields": {
            "project": {
                "key": "${jira.project.key}"
            },
            "summary": "${jira.issue.summary}",
            "description": "${jira.issue.description}",
            "issuetype": {
                "id": "1"
            },
            "assignee": {
                "name": "${jira.issue.assignee}"
            }
        },
        "properties": []
    }
    """
    When receive POST /rest/api/latest/issue
    Then HTTP response body
    """
    {
        "id": "${jira.issue.id}",
        "key": "${jira.issue.key}",
        "self": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.id}"
    }
    """
    Then send HTTP 200 OK
    # Verify event
    And Camel K integration jira-add-issue-sink-pipe should print ${jira.issue.summary}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe jira-add-issue-sink-pipe
    Given delete Kubernetes service jira-service
    And stop server component jira-service
