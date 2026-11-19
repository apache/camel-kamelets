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
// Makes one change to the collection "orders" of the database "testdb", as
// the test variable mongodb.change says: create the collection, insert the
// document "order-1", update it, or delete it.

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import org.bson.Document

def client = MongoClients.create('mongodb://${CITRUS_TESTCONTAINERS_MONGODB_HOST}:${CITRUS_TESTCONTAINERS_MONGODB_PORT}')
try {
    def db = client.getDatabase('testdb')
    switch ('${mongodb.change}') {
        case 'create':
            db.createCollection('orders')
            break
        case 'insert':
            db.getCollection('orders').insertOne(new Document('_id', 'order-1').append('status', 'new'))
            break
        case 'update':
            db.getCollection('orders').updateOne(Filters.eq('_id', 'order-1'), Updates.set('status', 'shipped'))
            break
        case 'delete':
            db.getCollection('orders').deleteOne(Filters.eq('_id', 'order-1'))
            break
        default:
            throw new IllegalArgumentException('Unknown change: ${mongodb.change}')
    }
} finally {
    client.close()
}
