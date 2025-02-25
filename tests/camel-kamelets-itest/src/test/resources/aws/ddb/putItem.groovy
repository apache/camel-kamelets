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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ReturnValue

var amazonDDBClient = context.getReferenceResolver().resolve("amazonDDBClient")

Map<String, AttributeValue> item = new HashMap<>()
item.put("id", AttributeValue.builder().n('${aws.ddb.item.id}').build())
item.put("year", AttributeValue.builder().n('${aws.ddb.item.year}').build())
item.put("title", AttributeValue.builder().s('${aws.ddb.item.title}').build())

amazonDDBClient.putItem(b -> {
    b.tableName('${aws.ddb.tableName}')
    b.item(item)
    b.returnValues(ReturnValue.ALL_OLD)
})
