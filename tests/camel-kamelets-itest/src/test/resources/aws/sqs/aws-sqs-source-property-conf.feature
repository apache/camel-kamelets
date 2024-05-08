Feature: AWS SQS Kamelet - property based config

  Background:
    Given variables
      | aws.sqs.queueName | myqueue |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service SQS
    Given start LocalStack container

  Scenario: Verify AWS-SQS events
    # Create AWS-SQS client
    Given load to Camel registry amazonSQSClient.groovy
    # Create binding
    Given Camel K integration property file aws-sqs-credentials.properties
    Given create Camel K integration aws-sqs-to-log-prop-based.groovy
    """
    from("kamelet:aws-sqs-source/aws-sqs-credentials")
      .to("log:info")
    """
    Then Camel K integration aws-sqs-to-log-prop-based should be running
    # Verify Kamelet source
    Given variable aws.sqs.message is "Hello from SQS Kamelet"
    And send Camel exchange to("aws2-sqs:${aws.sqs.queueName}?amazonSQSClient=#amazonSQSClient") with body: ${aws.sqs.message}
    Then Camel K integration aws-sqs-to-log-prop-based should print ${aws.sqs.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Camel K integration aws-sqs-to-log-prop-based
    # Stop LocalStack container
    Given stop LocalStack container
