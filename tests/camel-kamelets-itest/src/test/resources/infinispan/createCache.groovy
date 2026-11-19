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
// Creates the cache "kamelets" on the Infinispan server, as the Infinispan
// Kamelets need a cache that already exists.
//
//  - the REST API of Infinispan asks for a Digest login, which HttpURLConnection
//    answers through an Authenticator
//  - a local cache, so the server sends the client no cluster topology with the
//    container address, which the client could not reach
//  - string concatenation rather than Groovy GStrings, because Citrus resolves
//    dollar-brace expressions in these scripts as its own test variables

def url = 'http://${CITRUS_TESTCONTAINERS_INFINISPAN_HOST}:${CITRUS_TESTCONTAINERS_INFINISPAN_PORT}/rest/v2/caches/kamelets'

def conn = (HttpURLConnection) new URL(url).openConnection()
conn.authenticator = new Authenticator() {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication('admin', 'password'.toCharArray())
    }
}
conn.requestMethod = 'POST'
conn.doOutput = true
conn.connectTimeout = 10000
conn.readTimeout = 30000
conn.setRequestProperty('Content-Type', 'application/json')
conn.outputStream.withWriter { it << '{"local-cache":{}}' }
def code = conn.responseCode
if (code >= 300) {
    throw new IllegalStateException('POST ' + url + ' failed with ' + code)
}
