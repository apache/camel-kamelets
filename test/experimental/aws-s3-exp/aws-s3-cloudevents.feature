@knative
@experimental
Feature: AWS S3 Kamelet - cloud events data type

  Background:
    Given Knative event consumer timeout is 20000 ms
    Given variables
      | aws.s3.output | cloudevents |
      | aws.s3.bucketNameOrArn | mybucket |
      | aws.s3.message | Hello from S3 Kamelet |
      | aws.s3.key | hello.txt |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service S3
    Given start LocalStack container
    # Create AWS-S3 client
    Given New global Camel context
    Given load to Camel registry amazonS3Client.groovy
    # Create Knative broker
    Given create Knative broker default
    And Knative broker default is running

  Scenario: Verify AWS-S3 Kamelet to Knative binding
    # Create binding
    When load KameletBinding aws-s3-to-knative.yaml
    And KameletBinding aws-s3-to-knative-binding is available
    And Camel K integration aws-s3-to-knative-binding is running
    Then Camel K integration aws-s3-to-knative-binding should print Started aws-s3-to-knative-binding
    # Verify Kamelet source
    Given create Knative event consumer service event-consumer-service
    Given create Knative trigger event-service-trigger on service event-consumer-service with filter on attributes
      | type   | org.apache.camel.event.aws.s3.getObject |
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then expect Knative event data: ${aws.s3.message}
    And verify Knative event
      | type            | org.apache.camel.event.aws.s3.getObject |
      | source          | aws.s3.bucket.${aws.s3.bucketNameOrArn} |
      | subject         | ${aws.s3.key} |
      | id              | @ignore@ |

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete KameletBinding aws-s3-to-knative-binding
    Given delete Kubernetes service event-consumer-service
    # Remove Knative resources
    Given delete Knative broker default
    # Stop LocalStack container
    Given stop LocalStack container
