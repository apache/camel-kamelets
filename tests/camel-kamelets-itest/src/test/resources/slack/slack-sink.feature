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

Feature: Slack Kamelet - Sink

  Background:
    Given variables
      | timer.source.period | 10000 |
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
    Given load Pipe slack-sink-pipe.yaml
    # Verify authentication test
    Given expect HTTP request header: Content-Type="application/json; charset=UTF-8"
    Then expect HTTP request body
    """
    {
        "channel": "${slack.channel}",
        "text": "${slack.message}",
    }
    """
    When receive POST /
    Then send HTTP 200 OK
    # Verify event
    And Camel K integration slack-sink-pipe should print ${slack.message}

  Scenario: Remove resources
    # Remove Camel K binding
    Given delete Pipe slack-sink-pipe
    Given delete Kubernetes service slack-service
    And stop server component slack-service
