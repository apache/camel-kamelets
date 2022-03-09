Feature: AWS DDB Sink - UpdateItem

  Background:
    Given Kamelet aws-ddb-sink is available
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given variables
      | timer.source.period    | 10000 |
      | aws.ddb.operation      | UpdateItem |
      | aws.ddb.tableName      | movies |
      | aws.ddb.item.id        | 1 |
      | aws.ddb.item.year      | 1933 |
      | aws.ddb.item.title     | King Kong |
      | aws.ddb.item.title.new | King Kong - Historical |
      | aws.ddb.item.directors | ["Merian C. Cooper", "Ernest B. Schoedsack"] |
      | aws.ddb.json.data      | { "key": {"id": ${aws.ddb.item.id}}, "item": {"title": "${aws.ddb.item.title.new}", "year": ${aws.ddb.item.year}, "directors": ${aws.ddb.item.directors}} } |

  Scenario: Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create AWS-DDB client
    Given New global Camel context
    Given load to Camel registry amazonDDBClient.groovy

  Scenario: Create item on AWS-DDB
    Given run script putItem.groovy
    Given variables
      | aws.ddb.items | [{year=AttributeValue(N=${aws.ddb.item.year}), id=AttributeValue(N=${aws.ddb.item.id}), title=AttributeValue(S=${aws.ddb.item.title})}] |
    Then run script verifyItems.groovy

  Scenario: Create AWS-DDB Kamelet sink binding
    When load KameletBinding aws-ddb-sink-binding.yaml
    And KameletBinding aws-ddb-sink-binding is available
    And Camel K integration aws-ddb-sink-binding is running
    And Camel K integration aws-ddb-sink-binding should print Routes startup
    Then sleep 10sec

  Scenario: Verify Kamelet sink
    Given variables
      | aws.ddb.item.directors | [Ernest B. Schoedsack, Merian C. Cooper] |
      | aws.ddb.items | [{year=AttributeValue(N=${aws.ddb.item.year}), directors=AttributeValue(SS=${aws.ddb.item.directors}), id=AttributeValue(N=${aws.ddb.item.id}), title=AttributeValue(S=${aws.ddb.item.title.new})}] |
    Then run script verifyItems.groovy

  Scenario: Remove Camel K resources
    Given delete KameletBinding aws-ddb-sink-binding

  Scenario: Stop container
    Given stop LocalStack container
