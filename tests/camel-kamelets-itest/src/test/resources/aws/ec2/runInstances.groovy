/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// Floci emulates EC2 instances as metadata: they are running at once
def client = HttpClient.newHttpClient()
def runInstance = {
    def request = HttpRequest.newBuilder(URI.create(context.getVariable('CITRUS_TESTCONTAINERS_LOCALSTACK_SERVICE_URL') + '/'))
            .header('Content-Type', 'application/x-www-form-urlencoded')
            .POST(HttpRequest.BodyPublishers.ofString('Action=RunInstances&Version=2016-11-15&ImageId=ami-12345678&InstanceType=t2.micro&MinCount=1&MaxCount=1'))
            .build()
    def answer = client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    def matcher = answer =~ /<instanceId>([^<]+)<\/instanceId>/
    if (!matcher.find()) {
        throw new IllegalStateException('No instance in ' + answer)
    }
    return matcher.group(1)
}
context.setVariable('ec2.instanceId', runInstance())
context.setVariable('ec2.otherInstanceId', runInstance())
