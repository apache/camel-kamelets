# Protobuf serialization/deserialization

This test verifies the Protobuf serialization/deserialization actions

## Objectives

The test verifies the proper Protobuf serialization and deserialization of Protobuf.

The test uses two Pipes that interact with each other. The first binding `protobuf-binary-source-pipe` periodically creates a test data event as Json and applies the `protobuf/binary` data type using the schema in [User.proto](User.proto). 

The binary Protobuf data is then sent to a Http webhook sink that references a Http endpoint that is provided by the 2nd binding `protobuf-deserialize-pipe`. The `protobuf-deserialize-pipe` binding provides the Http service and deserializes the binary Protobuf data using the same User schema. The deserialized data is printed to the log output.

The test starts both Pipes and is able to verify the proper log output as an expected outcome.

### YAKS Test

The test performs the following high level steps:

*Protobuf data type feature*
- Create test data based on the User.proto Protobuf schema
- Load and run the `protobuf-deserialize-pipe` Pipe
- Load and run the `protobuf-binary-source-pipe` Pipe
- Verify that the bindings do interact with each other and the proper test data is logged in the binding output

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the YAKS CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install YAKS CLI](https://github.com/citrusframework/yaks#installation)

## Run the tests with JBang

To run tests with URI based configuration: 

```shell script
$ yaks run --local test/protobuf/protobuf-serdes-action.feature
```

You will be provided with the test log output and the test results.
