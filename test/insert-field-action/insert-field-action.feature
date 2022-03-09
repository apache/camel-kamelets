Feature: Timer Source Kamelet

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel K resource polling configuration
      | maxAttempts          | 60   |
      | delayBetweenAttempts | 3000 |

  Scenario: Wait for binding to start
    Given create Kubernetes service probe-service with target port 8080
    Then Camel K integration insert-field-action-binding should be running

  Scenario: Verify binding
    Given HTTP server "probe-service"
    And HTTP server timeout is 300000 ms
    Then expect HTTP request body
    """
    {
      "content": "thecontent",
      "thefield": "thevalue"
    }
    """
    And expect HTTP request header: Content-Type="application/json;charset=UTF-8"
    And receive POST /events
    And delete KameletBinding insert-field-action-binding
