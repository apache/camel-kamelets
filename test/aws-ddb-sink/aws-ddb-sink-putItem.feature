Feature: AWS DDB Sink - PutItem

  Background:
    Given Kamelet aws-ddb-sink is available
    Given Camel K resource polling configuration
      | maxAttempts          | 200   |
      | delayBetweenAttempts | 2000  |
    Given variables
      | timer.source.period  | 10000 |
      | aws.ddb.operation    | PutItem |
      | aws.ddb.tableName    | movies |
      | aws.ddb.item.id      | 1 |
      | aws.ddb.item.year    | 1977 |
      | aws.ddb.item.title   | Star Wars IV |
      | aws.ddb.json.data    | { "id":${aws.ddb.item.id}, "year":${aws.ddb.item.year}, "title":"${aws.ddb.item.title}" } |
      | aws.ddb.items        | [{year=AttributeValue(N=${aws.ddb.item.year}), id=AttributeValue(N=${aws.ddb.item.id}), title=AttributeValue(S=${aws.ddb.item.title})}] |

  Scenario: Start LocalStack container
    Given Enable service DYNAMODB
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create AWS-DDB client
    Given New global Camel context
    Given load to Camel registry amazonDDBClient.groovy

  Scenario: Create AWS-DDB Kamelet sink binding
    When load KameletBinding aws-ddb-sink-binding.yaml
    And KameletBinding aws-ddb-sink-binding is available
    And Camel K integration aws-ddb-sink-binding is running
    And Camel K integration aws-ddb-sink-binding should print Routes startup
    Then sleep 10sec

  Scenario: Verify Kamelet sink
    Then run script verifyItems.groovy

  Scenario: Remove Camel K resources
    Given delete KameletBinding aws-ddb-sink-binding

  Scenario: Stop container
    Given stop LocalStack container
