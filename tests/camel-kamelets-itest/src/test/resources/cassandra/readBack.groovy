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

// Reads the row that cassandra-sink inserted, and fails unless it holds the values of the message.

import com.datastax.oss.driver.api.core.CqlSession

def session = CqlSession.builder()
        .addContactPoint(new InetSocketAddress('${CITRUS_TESTCONTAINERS_CASSANDRA_HOST}', Integer.parseInt('${CITRUS_TESTCONTAINERS_CASSANDRA_PORT}')))
        .withLocalDatacenter('datacenter1')
        .build()
try {
    def row = session.execute("SELECT name, value FROM test_ks.test_data WHERE id = 42").one()
    if (row == null || row.getString('name') != 'sink-test' || row.getString('value') != 'from-cassandra-sink') {
        throw new IllegalStateException('Unexpected row with id 42: ' + row?.getFormattedContents())
    }
} finally {
    session.close()
}
