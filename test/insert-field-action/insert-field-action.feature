Feature: Timer Source Kamelet

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel-K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

  Scenario: Wait for binding to start
    Given create Kubernetes service probe-service with target port 8080
    Then KameletBinding insert-field-action-binding should be available

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
    And receive POST /events
    And delete KameletBinding insert-field-action-binding
