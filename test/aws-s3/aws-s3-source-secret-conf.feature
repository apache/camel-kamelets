@ignored
Feature: AWS S3 Kamelet - secret based config

  Background:
    Given variables
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
    # Create Kubernetes secret
    Given create Kubernetes secret aws-s3-source-credentials
      | aws-s3-credentials.properties | citrus:encodeBase64(citrus:readFile(aws-s3-credentials.properties)) |

  Scenario: Verify AWS-S3 Kamelet to log binding
    # Create binding
    Given create labels on Kubernetes secret aws-s3-source-credentials
      | camel.apache.org/kamelet               | aws-s3-source |
      | camel.apache.org/kamelet.configuration | aws-s3-credentials |
    Given load Camel K integration aws-s3-to-log-secret-based.groovy
    Then Camel K integration aws-s3-to-log-secret-based should be running
    # Verify Kamelet source
    Given Camel exchange message header CamelAwsS3Key="${aws.s3.key}"
    Given send Camel exchange to("aws2-s3://${aws.s3.bucketNameOrArn}?amazonS3Client=#amazonS3Client") with body: ${aws.s3.message}
    Then Camel K integration aws-s3-to-log-secret-based should print ${aws.s3.message}

  Scenario: Remove resources
    # Remove Camel K resources
    Given delete Camel K integration aws-s3-to-log-secret-based
    # Remove Kubernetes secret
    Given delete Kubernetes secret aws-s3-source-credentials
    # Stop LocalStack container
    Given stop LocalStack container
