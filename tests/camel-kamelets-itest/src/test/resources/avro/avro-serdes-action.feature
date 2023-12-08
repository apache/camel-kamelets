Feature: Avro serialize/deserialize action

  Scenario: Create Kamelet Pipes
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create avro-to-log binding
    When load Pipe avro-deserialize-pipe.yaml
    Then Camel K integration avro-deserialize-pipe should be running

    # Create json-to-avro binding
    When load Pipe avro-serialize-pipe.yaml
    Then Camel K integration avro-serialize-pipe should be running

    # Verify output message sent
    Then Camel K integration avro-deserialize-pipe should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete Pipe avro-deserialize-pipe
    Given delete Pipe avro-serialize-pipe
