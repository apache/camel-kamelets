Feature: Kamelet earthquake-source works

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel K resource polling configuration
      | maxAttempts          | 60   |
      | delayBetweenAttempts | 3000 |

  Scenario: Bind Kamelet to service
    Given create Kubernetes service test-service with target port 8080
    And bind Kamelet earthquake-source to uri http://test-service.${YAKS_NAMESPACE}.svc.cluster.local/test
    When create KameletBinding earthquake-source-uri
    Then KameletBinding earthquake-source-uri should be available

  Scenario: Verify binding
    Given HTTP server "test-service"
    And HTTP server timeout is 120000 ms
    Then expect HTTP request header: Content-Type="application/json;charset=UTF-8"
    And receive POST /test
    And delete KameletBinding earthquake-source-uri
