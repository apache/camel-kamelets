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

// Brings a freshly started Couchbase container to the point where the
// couchbase Kamelets can use it: initialise the cluster, create a bucket, add a
// primary index and seed one document.
//
// The container publishes its ports on random host ports, so that the test does
// not fail when another process uses 8091, 8093 or 11210. The Couchbase SDK
// reconnects to the addresses the cluster advertises, so the script renames the
// node to the IP address of the container and advertises the published host and ports as
// the "external" alternate addresses. A client whose seed host is that host then
// uses them. The script sets these test variables for the test and the routes:
// couchbase.host, couchbase.mgmt.port, couchbase.query.port and couchbase.kv.port.
//
// Two deliberate constraints:
//  - plain HTTP rather than the Couchbase SDK, so the test module does not need
//    the SDK on its classpath
//  - string concatenation rather than Groovy GStrings, because Citrus resolves
//    dollar-brace expressions in these scripts as its own test variables

def container = context.getReferenceResolver().resolve('couchbase')
def host = container.getHost()
def mgmtPort = container.getMappedPort(8091)
def queryPort = container.getMappedPort(8093)
def kvPort = container.getMappedPort(11210)
context.setVariable('couchbase.host', host)
context.setVariable('couchbase.mgmt.port', String.valueOf(mgmtPort))
context.setVariable('couchbase.query.port', String.valueOf(queryPort))
context.setVariable('couchbase.kv.port', String.valueOf(kvPort))

def admin = 'http://' + host + ':' + mgmtPort
def query = 'http://' + host + ':' + queryPort + '/query/service'
def user = 'Administrator'
def pass = 'password'
def bucket = 'kamelets'
def basic = 'Basic ' + (user + ':' + pass).bytes.encodeBase64().toString()

def call = { String url, String body, boolean auth, String method = 'POST' ->
    def conn = new URL(url).openConnection()
    conn.requestMethod = method
    conn.doOutput = true
    conn.connectTimeout = 10000
    conn.readTimeout = 60000
    conn.setRequestProperty('Content-Type', 'application/x-www-form-urlencoded')
    if (auth) {
        conn.setRequestProperty('Authorization', basic)
    }
    conn.outputStream.withWriter { it << body }
    def code = conn.responseCode
    if (code >= 400) {
        def error = conn.errorStream?.text
        throw new IllegalStateException(method + ' ' + url + ' failed with ' + code + ': ' + error)
    }
    return code
}

// The container logs "Starting Couchbase Server" well before the management
// port serves requests, so wait for readiness here rather than in the container
// wait strategy.
def ready = false
for (int i = 0; i < 60 && !ready; i++) {
    try {
        def conn = new URL(admin + '/pools').openConnection()
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        ready = conn.responseCode == 200
    } catch (Exception ignored) {
        // not up yet
    }
    if (!ready) {
        sleep(2000)
    }
}
if (!ready) {
    throw new IllegalStateException('Couchbase management port did not become available at ' + admin)
}

// the node name the cluster advertises on its default network, which must not be the seed host of the
// client: the IP address of the container (Couchbase does not accept a short host name)
def nodeName = container.getContainerInfo().getNetworkSettings().getNetworks().values()
        .collect { it.getIpAddress() }.find { it } ?: '127.0.0.1'
call(admin + '/node/controller/rename', 'hostname=' + nodeName, false)
call(admin + '/pools/default', 'memoryQuota=512&indexMemoryQuota=512', false)
call(admin + '/node/controller/setupServices', 'services=kv%2Cn1ql%2Cindex', false)
call(admin + '/settings/web', 'port=SAME&username=' + user + '&password=' + pass, false)
call(admin + '/settings/indexes', 'storageMode=forestdb', true)
call(admin + '/node/controller/setupAlternateAddresses/external',
        'hostname=' + host + '&mgmt=' + mgmtPort + '&kv=' + kvPort + '&n1ql=' + queryPort, true, 'PUT')
call(admin + '/pools/default/buckets', 'name=' + bucket + '&ramQuota=128&bucketType=couchbase', true)

// The bucket is created asynchronously and the query service only sees the
// keyspace once it has warmed up, so retry the index creation until it lands.
def indexed = false
for (int i = 0; i < 30 && !indexed; i++) {
    try {
        call(query, 'statement=' + URLEncoder.encode('CREATE PRIMARY INDEX ON `' + bucket + '`', 'UTF-8'), true)
        indexed = true
    } catch (Exception ignored) {
        sleep(2000)
    }
}
if (!indexed) {
    throw new IllegalStateException('Could not create the primary index on ' + bucket)
}

def insert = 'INSERT INTO `' + bucket + '` (KEY, VALUE) VALUES ("doc1", {"message":"hello-from-couchbase"})'
call(query, 'statement=' + URLEncoder.encode(insert, 'UTF-8'), true)
