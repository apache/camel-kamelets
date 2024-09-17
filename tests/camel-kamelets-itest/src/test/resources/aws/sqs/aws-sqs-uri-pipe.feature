Feature: AWS SQS Kamelet - binding to URI

  Background:
    Given variables
      | aws.sqs.queueName | myqueue |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service SQS
    Given start LocalStack container
    Then verify actions waitForLocalStack.groovy

  Scenario: Verify AWS-SQS events
    # Create AWS-SQS client
    Given load to Camel registry amazonSQSClient.groovy
    # Create binding
    Given load Pipe aws-sqs-uri-pipe.yaml
    Given Pipe aws-sqs-uri-pipe is available
    And Camel K integration aws-sqs-uri-pipe is running
    Then Camel K integration aws-sqs-uri-pipe should print Started aws-sqs-uri-pipe
    # Verify Kamelet source
    Given variable aws.sqs.message is "Hello from SQS Kamelet"
    And send Camel exchange to("aws2-sqs:${aws.sqs.queueName}?amazonSQSClient=#amazonSQSClient") with body: ${aws.sqs.message}
    Then Camel K integration aws-sqs-uri-pipe should print ${aws.sqs.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Pipe aws-sqs-uri-pipe
    # Stop LocalStack container
    Given stop LocalStack container
