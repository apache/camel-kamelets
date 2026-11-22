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

// Checks that the SMPPSim SMS center got the SMS that smpp-sink sent: SMPPSim
// logs each submit_sm it receives, with its fields.

def smppsim = context.getReferenceResolver().resolve('smppsim', org.testcontainers.containers.GenericContainer.class)
def logs = smppsim.getLogs()
['source_addr=1616', 'dest_addr=4598765432', 'short_message=Your code is 1234'].each { expected ->
    if (!logs.contains(expected)) {
        throw new org.citrusframework.exceptions.CitrusRuntimeException('SMPPSim did not log ' + expected + ' for a submit_sm')
    }
}
