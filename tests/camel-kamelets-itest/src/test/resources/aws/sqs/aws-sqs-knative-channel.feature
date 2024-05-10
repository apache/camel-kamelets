@knative
Feature: AWS SQS Kamelet - binding to InMemoryChannel

  Background:
    Given variables
      | aws.sqs.queueName | myqueue |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service SQS
    Given start LocalStack container
    # Create Knative broker and channel
    Given create Knative broker default
    And Knative broker default is running
    And create Knative channel messages

  Scenario: Verify AWS-SQS Kamelet to InMemoryChannel binding
    # Create AWS-SQS client
    Given load to Camel registry amazonSQSClient.groovy
    # Create binding
    Given load Pipe aws-sqs-to-knative-channel.yaml
    Given load Pipe knative-channel-to-log.yaml
    Then Pipe aws-sqs-to-knative-channel is available
    And Pipe knative-channel-to-log should be available
    And Camel K integration aws-sqs-to-knative-channel is running
    And Camel K integration knative-channel-to-log is running
    And Camel K integration aws-sqs-to-knative-channel should print Started aws-sqs-to-knative-channel
    And Camel K integration knative-channel-to-log should print Installed features
    # Verify Kamelet source
    Given variable aws.sqs.message is "Hello from SQS Kamelet"
    And send Camel exchange to("aws2-sqs:${aws.sqs.queueName}?amazonSQSClient=#amazonSQSClient") with body: ${aws.sqs.message}
    Then Camel K integration knative-channel-to-log should print ${aws.sqs.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Pipe aws-sqs-to-knative-channel
    Given delete Pipe knative-channel-to-log
    # Remove Knative resources
    Given delete Knative broker default
    Given delete Knative channel messages
    # Stop LocalStack container
    Given stop LocalStack container
