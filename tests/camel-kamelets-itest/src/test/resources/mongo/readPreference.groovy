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
// Checks that the open cursor of the Kamelet on the collection "testcol" (the tailable cursor of
// mongodb-source) was opened with the read preference secondaryPreferred, which the Kamelet's
// readPreference option sets. $currentOp with idleCursors shows the command that opened it, both
// for an idle cursor and for a getMore that is waiting for data (an operation on the cursor).

import com.mongodb.client.MongoClients
import org.bson.Document
import org.citrusframework.exceptions.ValidationException

def client = MongoClients.create('mongodb://${CITRUS_TESTCONTAINERS_MONGODB_HOST}:${CITRUS_TESTCONTAINERS_MONGODB_PORT}')
try {
    def pipeline = [
            new Document('$currentOp', new Document('idleCursors', true).append('allUsers', true)),
            new Document('$match', new Document('ns', 'testdb.testcol').append('cursor', new Document('$exists', true)))]
    def cursors = client.getDatabase('admin').aggregate(pipeline).into([])
    def modes = cursors.collect { it.get('cursor', Document)?.get('originatingCommand', Document)?.get('$readPreference', Document)?.getString('mode') }
    if (!modes.contains('secondaryPreferred')) {
        throw new ValidationException('Expected a cursor on testdb.testcol opened with the read preference secondaryPreferred, but the cursors have ' + modes)
    }
} finally {
    client.close()
}
