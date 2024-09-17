Feature: AWS S3 Source - Http sink

  Background:
    Given Kubernetes timeout is 60000 ms
    Given HTTP server timeout is 60000 ms
    Given HTTP server "test-service"
    Given start HTTP server
    Given variables
      | aws.s3.bucketNameOrArn | mybucket |
      | aws.s3.message | Hello from S3 Kamelet |
      | aws.s3.key | hello.txt |

  Scenario: Create infrastructure
    # Start LocalStack container
    Given Enable service S3
    Given start LocalStack container
    Then verify actions waitForLocalStack.groovy

  Scenario: Verify AWS-S3 Kamelet to Http
    # Create Http server
    Given create Kubernetes service test-service
    Given purge endpoint test-service
    # Create AWS-S3 client
    Given New Camel context
    Given load to Camel registry amazonS3Client.groovy
    # Create binding
    When load Pipe aws-s3-to-http.yaml
    And Pipe aws-s3-to-http is available
    And Camel K integration aws-s3-to-http is running
    Then Camel K integration aws-s3-to-http should print (aws-s3-to-http) started
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
      | Content-Type   | text/plain; charset=UTF-8 |
    When receive POST /incoming
    Then send HTTP 201 CREATED

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Pipe aws-s3-to-http
    Given delete Kubernetes service test-service
    # Stop LocalStack container
    Given stop LocalStack container
    And stop HTTP server
