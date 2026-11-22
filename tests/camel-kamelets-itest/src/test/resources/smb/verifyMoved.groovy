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

// Checks that the file orders/order 1.csv was moved to orders/.done once it was read.

import org.testcontainers.containers.GenericContainer

def smb = context.getReferenceResolver().resolve('smb', GenericContainer.class)

def files = smb.execInContainer('find', '/shared/orders', '-type', 'f').getStdout().readLines()
if (files != ['/shared/orders/.done/order 1.csv']) {
    throw new IllegalStateException('Expected only /shared/orders/.done/order 1.csv but got: ' + files)
}
