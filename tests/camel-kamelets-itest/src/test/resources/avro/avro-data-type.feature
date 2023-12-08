Feature: Avro data type

  Scenario: Create Kamelet Pipes
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create avro-to-log binding
    When load Pipe avro-x-struct-sink-pipe.yaml
    Then Camel K integration avro-x-struct-sink-pipe should be running

    # Create json-to-avro binding
    When load Pipe avro-binary-source-pipe.yaml
    Then Camel K integration avro-binary-source-pipe should be running

    # Verify output message sent
    Then Camel K integration avro-x-struct-sink-pipe should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete Pipe avro-x-struct-sink-pipe
    Given delete Pipe avro-binary-source-pipe
