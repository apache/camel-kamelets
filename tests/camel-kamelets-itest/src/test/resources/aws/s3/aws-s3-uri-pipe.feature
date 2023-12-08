Feature: AWS S3 Source - binding to URI

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
    When load Pipe aws-s3-uri-pipe.yaml
    And Pipe aws-s3-uri-pipe is available
    And Camel K integration aws-s3-uri-pipe is running
    Then Camel K integration aws-s3-uri-pipe should print Started aws-s3-uri-pipe
    # Create AWS-S3 client
    Given New Camel context
    Given load to Camel registry amazonS3Client.groovy
    # Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration aws-s3-uri-pipe should print ${aws.s3.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Pipe aws-s3-uri-pipe
    # Stop LocalStack container
    Given stop LocalStack container
