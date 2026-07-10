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

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import com.mongodb.client.model.CreateCollectionOptions
import org.bson.Document

MongoClient mongoClient = MongoClients.create('mongodb://${CITRUS_TESTCONTAINERS_MONGODB_HOST}:${CITRUS_TESTCONTAINERS_MONGODB_PORT}')

def db = mongoClient.getDatabase('testdb')

db.createCollection('testcol', new CreateCollectionOptions().capped(true).sizeInBytes(1048576))

def col = db.getCollection('testcol')
col.insertOne(new Document('name', 'source-test').append('value', 'hello-from-mongo'))
