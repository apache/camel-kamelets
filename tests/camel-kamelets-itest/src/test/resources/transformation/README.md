# Transformations

The tests verify different transformations performed as part of a Pipe.

## Objectives

The test verifies the proper data transformations provided by action Kamelets and data types.

The test starts both Pipes and is able to verify the proper log output as an expected outcome.

### Citrus Test

The test performs the following high level steps:

*Avro data type feature*
- Create test data based on the User.avsc Avro schema
- Load and run the `avro-to-log` Pipe
- Load and run the `json-to-avro` Pipe
- Verify that the bindings do interact with each other and the proper test data is logged in the binding output

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the Citrus CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install Citrus JBang App](https://citrusframework.org/citrus/reference/html/index.html#runtime-jbang-install)

## Run the tests with JBang

To run tests with URI based configuration: 

```shell script
$ citrus run src/test/resources/transformation/data-type-action.it.yaml
```

You will be provided with the test log output and the test results.
