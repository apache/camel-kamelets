@knative
@experimental
Feature: AWS S3 Kamelet - Knative binding

  Background:
    Given Kamelet aws-s3-experimental-source is available
    Given Knative event consumer timeout is 20000 ms
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 4000  |
    Given variables
      | aws.s3.output | string |
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

  Scenario: Create AWS-S3 Kamelet to Knative binding
    Given variable loginfo is "Installed features"
    When load KameletBinding aws-s3-to-knative.yaml
    And KameletBinding aws-s3-to-knative-binding is available
    And Camel K integration aws-s3-to-knative-binding is running
    Then Camel K integration aws-s3-to-knative-binding should print ${loginfo}

  Scenario: Verify Kamelet source
    Given create Knative event consumer service event-consumer-service
    Given create Knative trigger event-service-trigger on service event-consumer-service with filter on attributes
      | type   | org.apache.camel.event |
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then expect Knative event data: ${aws.s3.message}
    And verify Knative event
      | type            | org.apache.camel.event |
      | source          | @ignore@ |
      | id              | @ignore@ |

  Scenario: Remove Camel K resources
    Given delete KameletBinding aws-s3-to-knative-binding
    Given delete Kubernetes service event-consumer-service
    Given delete Knative broker default

  Scenario: Stop container
    Given stop LocalStack container
