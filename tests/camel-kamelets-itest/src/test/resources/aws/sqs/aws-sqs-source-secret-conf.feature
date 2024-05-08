@ignored
Feature: AWS SQS Kamelet - secret based config

  Background:
    Given variables
      | aws.sqs.queueName | myqueue |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service SQS
    Given start LocalStack container
    # Create Kubernetes secret
    Given create Kubernetes secret aws-sqs-source-credentials
      | aws-sqs-credentials.properties | citrus:encodeBase64(citrus:readFile(aws-sqs-credentials.properties)) |
    Given create labels on Kubernetes secret aws-sqs-source-credentials
      | camel.apache.org/kamelet               | aws-sqs-source |
      | camel.apache.org/kamelet.configuration | aws-sqs-credentials |

  Scenario: Verify AWS-SQS events
    # Create AWS-SQS client
    Given load to Camel registry amazonSQSClient.groovy
    # Create binding
    Given load Camel K integration aws-sqs-to-log-secret-based.groovy
    Then Camel K integration aws-sqs-to-log-secret-based should be running
    # Verify Kamelet source
    Given variable aws.sqs.message is "Hello from SQS Kamelet"
    And send Camel exchange to("aws2-sqs:${aws.sqs.queueName}?amazonSQSClient=#amazonSQSClient") with body: ${aws.sqs.message}
    Then Camel K integration aws-sqs-to-log-secret-based should print ${aws.sqs.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Camel K integration aws-sqs-to-log-secret-based
    # Remove Kubernetes secret
    Given delete Kubernetes secret aws-sqs-source-credentials
    # Stop LocalStack container
    Given stop LocalStack container
