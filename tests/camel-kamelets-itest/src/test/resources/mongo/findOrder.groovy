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

// Checks the document "order-1" in the collection "testcol" of the database "testdb": it must be
// equal to the JSON object in the test variable mongodb.expected.

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import org.bson.Document
import org.citrusframework.exceptions.ValidationException

def expected = Document.parse(context.getVariable('mongodb.expected'))
def client = MongoClients.create('mongodb://${CITRUS_TESTCONTAINERS_MONGODB_HOST}:${CITRUS_TESTCONTAINERS_MONGODB_PORT}')
try {
    def stored = client.getDatabase('testdb').getCollection('testcol').find(Filters.eq('_id', 'order-1')).first()
    if (stored != expected) {
        throw new ValidationException('Expected the stored document ' + expected.toJson() + ', but it is ' + stored?.toJson())
    }
} finally {
    client.close()
}
