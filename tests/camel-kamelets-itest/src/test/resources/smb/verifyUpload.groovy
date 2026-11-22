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

// Checks that the share has the file upload/day 1.csv, with the body of the message as its content, and no file
// written outside the directory upload.

import org.testcontainers.containers.GenericContainer

def smb = context.getReferenceResolver().resolve('smb', GenericContainer.class)
def file = '/shared/upload/day 1.csv'

def content = smb.execInContainer('cat', file)
if (content.getExitCode() != 0) {
    throw new IllegalStateException('The share has no file ' + file + ': ' + content.getStderr())
}
if (content.getStdout() != 'id,item\n1,book') {
    throw new IllegalStateException('Unexpected content of ' + file + ': ' + content.getStdout().inspect())
}
def files = smb.execInContainer('find', '/shared', '-type', 'f').getStdout().readLines().sort()
if (files != ['/shared/orders/order 1.csv', file]) {
    throw new IllegalStateException('Unexpected files on the share: ' + files)
}
