Feature: AWS S3 Source - URI based config

  Background:
    Given variables
      | aws.s3.bucketNameOrArn | mybucket |
      | aws.s3.message | Hello from S3 Kamelet |
      | aws.s3.key | hello.txt |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service S3
    Given start LocalStack container

  Scenario: Verify AWS-S3 Kamelet to log binding
    # Create binding
    Given load Camel K integration aws-s3-to-log-uri-based.groovy
    Then Camel K integration aws-s3-to-log-uri-based should be running
    # Create AWS-S3 client
    Given New Camel context
    Given load to Camel registry amazonS3Client.groovy
    # Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration aws-s3-to-log-uri-based should print ${aws.s3.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Camel K integration aws-s3-to-log-uri-based
    # Stop LocalStack container
    Given stop LocalStack container
