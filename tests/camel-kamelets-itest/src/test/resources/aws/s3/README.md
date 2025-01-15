# AWS S3 Kamelet test

This test verifies the AWS S3 Kamelet source defined in [aws-s3-source.kamelet.yaml](aws-s3-source.kamelet.yaml)

## Objectives

The test verifies the AWS S3 Kamelet source by creating a Camel K integration that uses the Kamelet and listens for messages on the
AWS S3 bucket.

The test uses a [LocalStack Testcontainers](https://www.testcontainers.org/modules/localstack/) instance to start a local AWS S3 service for mocking reasons.
The Kamelet and the test interact with the local AWS S3 service for validation of functionality.

### Test Kamelet source

The test performs the following high level steps for configs - URI, secret and property based:

*Preparation*
- Start the AWS S3 service as LocalStack container
- Overwrite the Kamelet with the latest source
- Prepare the Camel AWS S3 client

*Scenario* 
- Create the Kamelet in the current namespace in the cluster
- Create the Camel K integration that uses the Kamelet source to consume data from AWS S3 service
- Wait for the Camel K integration to start and listen for AWS S3 messages
- Create a new message in the AWS S3 bucket
- Verify that the integration has received the message event

*Cleanup*
- Stop the LocalStack container
- Delete the Camel K integration
- Delete the secret from the current namespacce

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the Citrus CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install Citrus JBang App](https://citrusframework.org/citrus/reference/html/index.html#runtime-jbang-install)

## Run the tests

To run tests with URI based configuration: 

```shell script
$ citrus run src/test/resources/aws-s3-source-uri-conf.it.yaml
```

To run tests with secret based configuration:

```shell script
$ citrus run src/test/resources/aws/s3/aws-s3-source-secret-conf.it.yaml
```

To run tests with property based configuration:

```shell script
$ citrus run src/test/resources/aws/s3/aws-s3-source-property-conf.it.yaml
```

To run tests with URI binding:

```shell script
$ citrus run src/test/resources/aws/s3/aws-s3-uri-pipe.it.yaml
```

You will be provided with the test log output and the test results.
