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

import com.datastax.oss.driver.api.core.CqlSession

def session = CqlSession.builder()
        .addContactPoint(new InetSocketAddress('${CITRUS_TESTCONTAINERS_CASSANDRA_HOST}', Integer.parseInt('${CITRUS_TESTCONTAINERS_CASSANDRA_SERVICE_PORT}')))
        .withLocalDatacenter('datacenter1')
        .build()

session.execute("CREATE KEYSPACE IF NOT EXISTS test_ks WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
session.execute("CREATE TABLE IF NOT EXISTS test_ks.test_data (id int PRIMARY KEY, name text, value text)")
session.execute("INSERT INTO test_ks.test_data (id, name, value) VALUES (1, 'test-entry', 'hello-cassandra')")

session.close()
