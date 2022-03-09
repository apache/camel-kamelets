Feature: Mail Sink

  Background:
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given variables
      | host      | mail-server |
      | username  | test |
      | password  | secret |
      | from      | user@demo.yaks |
      | to        | announcements@demo.yaks |
      | subject   | Kamelet workshop |
      | message   | Camel K rocks |

  Scenario: Create mail server
    Given load endpoint mail-server.groovy
    Given create Kubernetes service mail-server with port mapping 25:22222

  Scenario: Create Camel K resources
    Given Kamelet mail-sink is available
    Given Kamelet timer-source is available
    Given load KameletBinding timer-to-mail.yaml
    And Camel K integration timer-to-mail should be running

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
