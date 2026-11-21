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
// Checks the request that the stand-in for the function received: azure-functions-sink must not send on the
// Authorization and Proxy-Authorization headers of the message. Citrus keeps each received message in its message
// store; the request is the one for the path /api/orders.

def store = context.getMessageStore()
def requests = store.values().findAll { message ->
    message.getHeaders().values().any { it?.toString()?.endsWith('/api/orders') }
}
if (requests.isEmpty()) {
    throw new IllegalStateException('No received request for /api/orders in the message store: ' + store.keySet())
}
requests.each { request ->
    def sent = request.getHeaders().keySet().findAll {
        it.equalsIgnoreCase('Authorization') || it.equalsIgnoreCase('Proxy-Authorization')
    }
    if (!sent.isEmpty()) {
        throw new IllegalStateException('The function got the credential headers of the message: ' + sent)
    }
}
