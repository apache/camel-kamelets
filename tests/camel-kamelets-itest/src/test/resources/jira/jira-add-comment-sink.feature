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
      | jira.issue.comment  | New comment, citrus:randomString(10) |
      | jira.issue.comment.id | 10001 |
      | jira.username       | yaks   |
      | jira.password       | secr3t |
    Given HTTP server timeout is 15000 ms
    Given HTTP server "jira-service"

  Scenario: Create Http server
    Given create Kubernetes service jira-service
    Given purge endpoint jira-service

  Scenario: Verify Jira events
    # Create binding
    Given load Pipe jira-add-comment-sink-pipe.yaml
    # Verify get issue request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    When receive GET /rest/api/latest/issue/${jira.issue.key}
    Then HTTP response header: Content-Type="application/json"
    And HTTP response body
    """
    {
        "expand": "schema,names",
        "id": "${jira.issue.id}",
        "key": "${jira.issue.key}",
        "self": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.key}",
        "transitions": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.key}/transitions",
        "names": {},
        "schema": {},
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
    """
    Then send HTTP 200 OK
    # Verify add comment request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    When receive GET /rest/api/latest/serverInfo
    Then HTTP response body
    """
    {
        "baseUrl": "yaks:resolveURL('jira-service')",
        "version": "5.0.0",
        "versionNumbers": [
            5,
            0,
            0
        ],
        "buildNumber": 581,
        "buildDate": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
        "serverTime": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
        "scmInfo": "yaks:randomString(40, LOWERCASE)",
        "buildPartnerName": "YAKS Co.",
        "serverTitle": "Test Jira Server"
    }
    """
    Then send HTTP 200 OK
    # Verify add comment request
    Given expect HTTP request header: Authorization="Basic citrus:encodeBase64(${jira.username}:${jira.password})"
    Then expect HTTP request body
    """
    {
        "body": "${jira.issue.comment}"
    }
    """
    When receive POST /rest/api/latest/issue/${jira.issue.key}/comment
    Then HTTP response body
    """
    {
        "self": "yaks:resolveURL('jira-service')/rest/api/latest/issue/${jira.issue.id}/comment/${jira.issue.comment.id}",
        "id": "${jira.issue.comment.id}",
        "author": {
            "self": "yaks:resolveURL('jira-service')/rest/api/latest/user?username=yaks",
            "name": "yaks",
            "displayName": "YAKS User",
            "active": false
        },
        "body": "${jira.issue.comment}",
        "updateAuthor": {
            "self": "yaks:resolveURL('jira-service')/rest/api/latest/user?username=yaks",
            "name": "yaks",
            "displayName": "YAKS User",
            "active": false
        },
        "created": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
        "updated": "yaks:currentDate(yyyy-MM-dd'T'HH:mm:ss.SZ)",
        "visibility": {
            "type": "role",
            "value": "Users"
        }
    }
    """
    Then send HTTP 200 OK
    # Verify event
    And Camel K integration jira-add-comment-sink-pipe should print ${jira.issue.comment}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe jira-add-comment-sink-pipe
    Given delete Kubernetes service jira-service
    And stop server component jira-service
