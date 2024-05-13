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

Feature: Slack Kamelet - Source

  Background:
    Given variables
      | slack.channel | announcements |
      | slack.message | Camel rocks! |
      | slack.user.id | W12345678    |
      | slack.team.id | T12345678    |
    Given HTTP server timeout is 15000 ms
    Given HTTP server "slack-service"

  Scenario: Create Http server
    Given create Kubernetes service slack-service

  Scenario: Verify Slack events
    Given variables
      | slack.token   | xoxb-yaks:randomNumber(10)-yaks:randomNumber(13)-yaks:randomString(34) |
    # Create binding
    Given load Pipe slack-source-pipe.yaml
    # Verify authentication test
    Given expect HTTP request header: Authorization="Bearer ${slack.token}"
    Given expect HTTP request header: Content-Type="application/x-www-form-urlencoded"
    When receive POST /api/auth.test
    Then HTTP response body
    """
    {
        "ok": true,
        "url": "yaks:resolveURL('slack-service')",
        "team": "Camel Workspace",
        "user": "yaks",
        "team_id": "${slack.team.id}",
        "user_id": "${slack.user.id}"
    }
    """
    Then send HTTP 200 OK
    # Verify conversations list
    Given expect HTTP request header: Authorization="Bearer ${slack.token}"
    When receive POST /api/conversations.list
    Then HTTP response body
    """
    {
        "ok": true,
        "channels": [
            {
                "id": "yaks:randomString(9, UPPERCASE)",
                "name": "${slack.channel}",
                "is_channel": true,
                "is_group": false,
                "is_im": false,
                "created": 1449252889,
                "creator": "U012A3CDE",
                "is_archived": false,
                "is_general": true,
                "unlinked": 0,
                "name_normalized": "${slack.channel}",
                "is_shared": false,
                "is_ext_shared": false,
                "is_org_shared": false,
                "pending_shared": [],
                "is_pending_ext_shared": false,
                "is_member": true,
                "is_private": false,
                "is_mpim": false,
                "updated": 1678229664302,
                "topic": {
                    "value": "Company-wide announcements and work-based matters",
                    "creator": "",
                    "last_set": 0
                },
                "purpose": {
                    "value": "This channel is for team-wide communication and announcements. All team members are in this channel.",
                    "creator": "",
                    "last_set": 0
                },
                "previous_names": [],
                "num_members": 2
            }
        ],
        "response_metadata": {
            "next_cursor": "yaks:randomString(32)"
        }
    }
    """
    Then send HTTP 200 OK
    # Verify conversations history
    Given expect HTTP request header: Authorization="Bearer ${slack.token}"
    When receive POST /api/conversations.history
    Then HTTP response body
    """
    {
        "ok": true,
        "messages": [
            {
                "type": "message",
                "user": "${slack.user.id}",
                "text": "${slack.message}",
                "ts": "1512085950.000216"
            }
        ],
        "has_more": true,
        "pin_count": 0,
        "response_metadata": {
            "next_cursor": "yaks:randomString(32)"
        }
    }
    """
    Then send HTTP 200 OK
    # Verify event
    And Camel K integration slack-source-pipe should print ${slack.message}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe slack-source-pipe
    Given delete Kubernetes service slack-service
    And stop server component slack-service
