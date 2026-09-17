# Avro serialization/deserialization

This test verifies the Avro serialization/deserialization actions

## Objectives

The test verifies the proper Avro serialization and deserialization of Avro.

The test uses two routes that interact with each other. The first route `avro-binary-source-route` periodically creates a test data event as Json and applies the `avro/binary` data type using the schema in [User.avsc](User.avsc). 

The binary Avro data is then sent to a Http webhook sink that references a Http endpoint that is provided by the 2nd route `avro-deserialize-route`. The `avro-deserialize-route` route provides the Http service and deserializes the binary Avro data using the same User schema. The deserialized data is printed to the log output.

The test starts both routes and is able to verify the proper log output as an expected outcome.

### Citrus Test

The test performs the following high level steps:

*Avro data type feature*
- Create test data based on the User.avsc Avro schema
- Load and run the `avro-deserialize-route` route
- Load and run the `avro-binary-source-route` route
- Verify that the routes do interact with each other and the proper test data is logged in the route output

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the Citrus CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install Citrus JBang App](https://citrusframework.org/citrus/reference/html/index.html#runtime-jbang-install)

## Run the tests with JBang

To run tests with URI based configuration: 

```shell script
$ citrus run src/test/resources/avro/avro-serdes-action.it.yaml
```

You will be provided with the test log output and the test results.
