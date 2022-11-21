@knative
Feature: AWS S3 Kamelet - binding to Knative

  Background:
    Given Kamelet aws-s3-source is available
    Given variables
      | aws.s3.bucketNameOrArn | mybucket |
      | aws.s3.message | Hello from S3 Kamelet |
      | aws.s3.key | hello.txt |

  Scenario: Start LocalStack container
    Given Enable service S3
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create AWS-S3 client
    Given New global Camel context
    Given load to Camel registry amazonS3Client.groovy

  Scenario: Create Knative broker
    Given create Knative broker default
    And Knative broker default is running

  Scenario: Create AWS-S3 Kamelet to InMemoryChannel binding
    Given variable loginfo is "Installed features"
    Given load KameletBinding aws-s3-to-knative.yaml
    Given load KameletBinding knative-to-log.yaml
    Then KameletBinding aws-s3-to-knative should be available
    And KameletBinding knative-to-log should be available
    And Camel K integration aws-s3-to-knative is running
    And Camel K integration knative-to-log is running
    And Camel K integration aws-s3-to-knative should print ${loginfo}
    And Camel K integration knative-to-log should print ${loginfo}
    Then sleep 10000 ms

  Scenario: Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration knative-to-log should print ${aws.s3.message}

  Scenario: Remove resources
    Given delete KameletBinding aws-s3-to-knative
    Given delete KameletBinding knative-to-log
    Given delete Knative broker default

  Scenario: Stop container
    Given stop LocalStack container
