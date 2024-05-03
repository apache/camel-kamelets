Feature: Protobuf data type

  Scenario: Create Kamelet Pipes
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create protobuf-to-log binding
    When load Pipe protobuf-x-struct-sink-pipe.yaml
    Then Camel K integration protobuf-x-struct-sink-pipe should be running

    # Create json-to-protobuf binding
    When load Pipe protobuf-binary-source-pipe.yaml
    Then Camel K integration protobuf-binary-source-pipe should be running

    # Verify output message sent
    Then Camel K integration protobuf-x-struct-sink-pipe should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete Pipe protobuf-x-struct-sink-pipe
    Given delete Pipe protobuf-binary-source-pipe
