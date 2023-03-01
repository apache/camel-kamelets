Feature: Data type action

  Background:
    Given HTTP server timeout is 15000 ms
    Given HTTP server "test-service"

  Scenario: Create Http server
    Given create Kubernetes service test-service with target port 8080

  Scenario: Create Kamelet binding
    Given variable uuid is "citrus:randomUUID()"
    Given variable input is
    """
    { "id": "${uuid}" }
    """
    When load KameletBinding data-type-action-binding.yaml
    Then Camel K integration data-type-action-binding should be running

    # Verify output message sent
    Given expect HTTP request body: ${input}
    And expect HTTP request headers
      | ce-specversion | 1.0 |
      | ce-id          | @notEmpty()@ |
      | ce-source      | org.apache.camel |
      | ce-type        | org.apache.camel.event |
      | ce-time        | @notEmpty()@ |
      | Content-Type   | application/json;charset=UTF-8 |
    When receive POST /result
    Then send HTTP 200 OK

  Scenario: Remove resources
    Given delete KameletBinding data-type-action-binding
    And delete Kubernetes service test-service
