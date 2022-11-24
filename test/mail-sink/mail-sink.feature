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

Feature: Mail Sink

  Background:
    Given variables
      | host      | mail-server |
      | username  | test |
      | password  | secret |
      | from      | user@demo.yaks |
      | to        | announcements@demo.yaks |
      | subject   | Kamelet workshop |
      | message   | Camel K rocks |

  Scenario: Create mail server
    Given HTTP server "mail-server"
    Given HTTP server listening on port 22222
    Given create Kubernetes service mail-server with port mapping 25:22222
    And stop HTTP server
    Given load endpoint mail-server.groovy

  Scenario: Create Camel K resources
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given load KameletBinding timer-to-mail.yaml
    And Camel K integration timer-to-mail should be running
    And Camel K integration timer-to-mail should print Routes startup

  Scenario: Verify mail message sent
    Then endpoint mail-server should receive body
    """
    {
      "from": "${from}",
      "to": "${to}",
      "cc": "",
      "bcc": "",
      "replyTo": "@ignore@",
      "subject": "${subject}",
      "body": {
        "contentType": "text/plain",
        "content": "${message}",
        "attachments": null
      }
    }
    """

  Scenario: Remove Camel K resources
    Given delete KameletBinding timer-to-mail
    And delete Kubernetes service mail-server
