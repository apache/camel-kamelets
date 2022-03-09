Feature: Timer Source Kamelet

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

  Scenario: Bind Kamelet to service
    Given create Kubernetes service probe-service with target port 8080
    And KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to uri http://probe-service.${YAKS_NAMESPACE}/events
    When create KameletBinding timer-source-binding
    Then KameletBinding timer-source-binding should be available

  Scenario: Verify binding
    Given HTTP server "probe-service"
    And HTTP server timeout is 300000 ms
    Then expect HTTP request body: Hello World
    And receive POST /events
    And delete KameletBinding timer-source-binding
