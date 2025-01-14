/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aws.ddb

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification
import software.amazon.awssdk.services.dynamodb.model.StreamViewType

DynamoDbClient amazonDDBClient = DynamoDbClient
        .builder()
        .endpointOverride(URI.create("${CITRUS_TESTCONTAINERS_LOCALSTACK_SERVICE_URL}"))
        .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        "${CITRUS_TESTCONTAINERS_LOCALSTACK_ACCESS_KEY}",
                        "${CITRUS_TESTCONTAINERS_LOCALSTACK_SECRET_KEY}")
        ))
        .region(Region.of("${CITRUS_TESTCONTAINERS_LOCALSTACK_REGION}"))
        .build()

amazonDDBClient.createTable(b -> {
        b.tableName("${aws.ddb.tableName}")
        b.keySchema(
                KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build(),
        )
        b.attributeDefinitions(
                AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.N).build(),
        )

        if (${aws.ddb.streams}) {
            b.streamSpecification(StreamSpecification.builder()
                .streamEnabled(true)
                .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES).build())
        }

        b.provisionedThroughput(
                ProvisionedThroughput.builder()
                        .readCapacityUnits(1L)
                        .writeCapacityUnits(1L).build())
})

return amazonDDBClient
