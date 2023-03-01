Feature: AWS S3 Kamelet - Http sink

  Background:
    Given Kubernetes timeout is 60000 ms
    Given HTTP server timeout is 60000 ms
    Given HTTP server "test-service"
    Given variables
      | aws.s3.bucketNameOrArn | mybucket |
      | aws.s3.message | Hello from S3 Kamelet |
      | aws.s3.key | hello.txt |

  Scenario: Create infrastructure
    # Create Http server
    Given create Kubernetes service test-service
    # Start LocalStack container
    Given Enable service S3
    Given start LocalStack container
    # Create AWS-S3 client
    Given New global Camel context
    Given load to Camel registry amazonS3Client.groovy

  Scenario: Verify AWS-S3 Kamelet to Http
    # Create binding
    When load KameletBinding aws-s3-to-http.yaml
    And KameletBinding aws-s3-to-http is available
    And Camel K integration aws-s3-to-http is running
    Then Camel K integration aws-s3-to-http should print Started aws-s3-to-http
    # Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    # Verify Http request
    Given expect HTTP request body: ${aws.s3.message}
    And expect HTTP request headers
      | ce-specversion | 1.0 |
      | ce-id          | @notEmpty()@ |
      | ce-subject     | ${aws.s3.key} |
      | ce-source      | aws.s3.bucket.${aws.s3.bucketNameOrArn} |
      | ce-type        | org.apache.camel.event.aws.s3.getObject |
      | ce-time        | @notEmpty()@ |
      | Content-Type   | application/json;charset=UTF-8 |
    When receive POST /incoming
    Then send HTTP 201 CREATED

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete KameletBinding aws-s3-to-http
    Given delete Kubernetes service test-service
    # Stop LocalStack container
    Given stop LocalStack container
