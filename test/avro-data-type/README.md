# Avro data type

This test verifies the Avro data type serialization/deserialization

## Objectives

The test verifies the proper serialization and deserialization of Avro data types `avro/binary` and `avro/x-struct`.

The test uses two KameletBindings that interact with each other. The first binding `json-to-avro` periodically creates a test data event as Json and applies the `avro/binary` data type using the schema in [User.avsc](User.avsc). 

The binary Avro data is then sent to a Http webhook sink that references an Http endpoint that is provided by the 2nd binding `avro-to-log`. The `avro-to-log` binding provides the Http service and deserializes the binary Avro data using the same User schema. The deserialized data is printed to the log output.

The test starts both KameletBindings and is able to verify the proper log output as an expected outcome.

### YAKS Test

The test performs the following high level steps:

*Avro data type feature*
- Create test data based on the User.avsc Avro schema
- Load and run the `avro-to-log` KameletBinding
- Load and run the `json-to-avro` KameletBinding
- Verify that the bindings do interact with each other and the proper test data is logged in the binding output

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the YAKS CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html)
- [Install YAKS CLI](https://github.com/citrusframework/yaks#installation)

## Run the tests with JBang

To run tests with URI based configuration: 

```shell script
$ yaks run --local test/avro-data-type/avro-serdes-action.feature
```

You will be provided with the test log output and the test results.
