# Avro serialization/deserialization

This test verifies the Avro serialization/deserialization actions

## Objectives

The test verifies the proper Avro serialization and deserialization of Avro.

The test uses two Pipes that interact with each other. The first binding `avro-binary-source-pipe` periodically creates a test data event as Json and applies the `avro/binary` data type using the schema in [User.avsc](User.avsc). 

The binary Avro data is then sent to a Http webhook sink that references a Http endpoint that is provided by the 2nd binding `avro-deserialize-pipe`. The `avro-deserialize-pipe` binding provides the Http service and deserializes the binary Avro data using the same User schema. The deserialized data is printed to the log output.

The test starts both Pipes and is able to verify the proper log output as an expected outcome.

### YAKS Test

The test performs the following high level steps:

*Avro data type feature*
- Create test data based on the User.avsc Avro schema
- Load and run the `avro-deserialize-pipe` Pipe
- Load and run the `avro-binary-source-pipe` Pipe
- Verify that the bindings do interact with each other and the proper test data is logged in the binding output

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the YAKS CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install YAKS CLI](https://github.com/citrusframework/yaks#installation)

## Run the tests with JBang

To run tests with URI based configuration: 

```shell script
$ yaks run --local test/avro/avro-serdes-action.feature
```

You will be provided with the test log output and the test results.
