Feature: AWS S3 Kamelet - URI based config

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

  Scenario: Create S3 client
    Given New global Camel context
    Given load to Camel registry amazonS3Client.groovy

  Scenario: Create AWS-S3 Kamelet to log binding
    Given load Camel K integration aws-s3-to-log-uri-based.groovy
    Then Camel K integration aws-s3-to-log-uri-based should be running

  Scenario: Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration aws-s3-to-log-uri-based should print ${aws.s3.message}

  Scenario: Remove Camel K resources
    Given delete Camel K integration aws-s3-to-log-uri-based

  Scenario: Stop container
    Given stop LocalStack container
