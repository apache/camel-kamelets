Feature: Protobuf serialize/deserialize action

  Scenario: Create Kamelet Pipes
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create protobuf-to-log binding
    When load Pipe protobuf-deserialize-pipe.yaml
    Then Camel K integration protobuf-deserialize-pipe should be running

    # Create json-to-protobuf binding
    When load Pipe protobuf-serialize-pipe.yaml
    Then Camel K integration protobuf-serialize-pipe should be running

    # Verify output message sent
    Then Camel K integration protobuf-deserialize-pipe should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete Pipe protobuf-deserialize-pipe
    Given delete Pipe protobuf-serialize-pipe
