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

// Checks that the sink uploaded upload/day 1.csv into the home directory of the user, with the body of the
// message as its content.

import java.nio.file.Files
import java.nio.file.Paths

def file = Paths.get(context.getVariable('ftp.home'), 'upload', 'day 1.csv')
if (!Files.exists(file)) {
    throw new org.citrusframework.exceptions.CitrusRuntimeException('The FTP server has no file ' + file)
}
def content = Files.readString(file)
if (content != 'id,item\n1,book') {
    throw new org.citrusframework.exceptions.CitrusRuntimeException('Unexpected content of ' + file + ': ' + content.inspect())
}
