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

// Checks that the file upload/day 1.csv is on the server, in the home directory of the user citrus, owned by
// this user, with the body of the message as its content and the permissions of the variable ssh.chmod.

import org.testcontainers.containers.GenericContainer

def ssh = context.getReferenceResolver().resolve('ssh', GenericContainer.class)
def file = '/config/upload/day 1.csv'

def content = ssh.execInContainer('cat', file)
if (content.getExitCode() != 0) {
    throw new org.citrusframework.exceptions.CitrusRuntimeException('The server has no file ' + file + ': ' + content.getStderr())
}
if (content.getStdout() != 'id,item\n1,book') {
    throw new org.citrusframework.exceptions.CitrusRuntimeException('Unexpected content of ' + file + ': ' + content.getStdout().inspect())
}
def stat = ssh.execInContainer('stat', '-c', '%a %U', file).getStdout().trim()
def expected = context.getVariable('ssh.chmod') + ' citrus'
if (stat != expected) {
    throw new org.citrusframework.exceptions.CitrusRuntimeException('Expected the permissions and owner ' + expected + ' of ' + file + ' but got ' + stat)
}
