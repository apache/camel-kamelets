Feature: Insert field Kamelet action

  Background:
    Given HTTP server timeout is 15000 ms
    Given HTTP server "test-insert-service"
    Given variables
      | field | subject |
      | value | Camel K rocks! |

  Scenario: Create Http server
    Given create Kubernetes service test-insert-service with target port 8080

  Scenario: Create Kamelet binding
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given variable input is
    """
    { "id": "citrus:randomUUID()" }
    """
    When load KameletBinding insert-field-action-binding.yaml
    Then Camel K integration insert-field-action-binding should be running
    And Camel K integration insert-field-action-binding should print Routes startup

  Scenario: Verify output message sent
    Given expect HTTP request body
    """
    { "id": "@ignore@", "${field}": "${value}" }
    """
    And HTTP request header Content-Type="application/json"
    When receive POST /result
    Then send HTTP 200 OK

  Scenario: Remove resources
    Given delete KameletBinding insert-field-action-binding
    And delete Kubernetes service test-insert-service
