Feature: Avro data type

  Scenario: Create Kamelet Pipes
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create avro-to-log binding
    When load Pipe avro-to-log-pipe.yaml
    Then Camel K integration avro-to-log-pipe should be running

    # Create json-to-avro binding
    When load Pipe json-to-avro-pipe.yaml
    Then Camel K integration json-to-avro-pipe should be running

    # Verify output message sent
    Then Camel K integration avro-to-log-pipe should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete Pipe avro-to-log-pipe
    Given delete Pipe json-to-avro-pipe
