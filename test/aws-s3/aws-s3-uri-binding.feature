Feature: AWS S3 Kamelet - binding to URI

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

  Scenario: Create AWS-S3 Kamelet to log binding
    Given variable loginfo is "Installed features"
    When load KameletBinding aws-s3-uri-binding.yaml
    And KameletBinding aws-s3-uri-binding is available
    And Camel K integration aws-s3-uri-binding is running
    Then Camel K integration aws-s3-uri-binding should print ${loginfo}

  Scenario: Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration aws-s3-uri-binding should print ${aws.s3.message}

  Scenario: Remove Camel K resources
    Given delete KameletBinding aws-s3-uri-binding

  Scenario: Stop container
    Given stop LocalStack container
