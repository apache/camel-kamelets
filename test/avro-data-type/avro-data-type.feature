Feature: Avro data type

  Scenario: Create Kamelet bindings
    Given variable uuid is "citrus:randomUUID()"
    Given variable user is
    """
    { "id": "${uuid}", "firstname": "Sheldon", "lastname": "Cooper", "age": 28 }
    """
    # Create avro-to-log binding
    When load KameletBinding avro-to-log-binding.yaml
    Then Camel K integration avro-to-log-binding should be running

    # Create json-to-avro binding
    When load KameletBinding json-to-avro-binding.yaml
    Then Camel K integration json-to-avro-binding should be running

    # Verify output message sent
    Then Camel K integration avro-to-log-binding should print Body: {  "id" : "${uuid}",  "firstname" : "Sheldon",  "lastname" : "Cooper",  "age" : 28}

  Scenario: Remove resources
    Given delete KameletBinding avro-to-log-binding
    Given delete KameletBinding json-to-avro-binding
