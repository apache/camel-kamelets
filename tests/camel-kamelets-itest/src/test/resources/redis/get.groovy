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
// Reads the key "test-key" with GET, in the Redis protocol over a plain socket,
// so the test module needs no Redis client. It fails unless the value is the
// text that redis-sink stored.

def args = ['GET', 'test-key']
def command = new StringBuilder('*' + args.size() + '\r\n')
args.each { command.append('$').append(it.getBytes('UTF-8').length).append('\r\n').append(it).append('\r\n') }

def socket = new Socket('${CITRUS_TESTCONTAINERS_REDIS_HOST}', '${CITRUS_TESTCONTAINERS_REDIS_PORT}' as int)
try {
    socket.soTimeout = 10000
    socket.outputStream.write(command.toString().getBytes('UTF-8'))
    socket.outputStream.flush()
    def reader = new BufferedReader(new InputStreamReader(socket.inputStream, 'UTF-8'))
    def length = reader.readLine()
    def value = reader.readLine()
    if (length != '$11' || value != 'hello-redis') {
        throw new IllegalStateException('Unexpected value of test-key: ' + length + ' ' + value)
    }
} finally {
    socket.close()
}
