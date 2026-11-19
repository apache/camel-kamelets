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

// A phone sends an SMS into the SMPPSim SMS center: the web form of the
// simulator (port 89 of the container) injects it, and SMPPSim delivers it to
// the receivers logged in to it, such as the smpp-source Kamelet.
//
// String concatenation rather than Groovy GStrings, because Citrus resolves
// dollar-brace expressions in these scripts as its own test variables.

def smppsim = context.getReferenceResolver().resolve('smppsim', org.testcontainers.containers.GenericContainer.class)
def form = [
    short_message: 'YES please', source_addr: '4511111111', destination_addr: '1616', submit: 'Submit Message',
    service_type: '', source_addr_ton: '0', source_addr_npi: '0', dest_addr_ton: '0', dest_addr_npi: '0',
    esm_class: '0', protocol_ID: '0', priority_flag: '0', registered_delivery_flag: '0', data_coding: '0'
]
def query = form.collect { k, v -> k + '=' + URLEncoder.encode(v, 'UTF-8') }.join('&')
def url = 'http://' + smppsim.getHost() + ':' + smppsim.getMappedPort(89) + '/inject_mo?' + query

def conn = new URL(url).openConnection()
conn.connectTimeout = 10000
conn.readTimeout = 30000
def code = conn.responseCode
if (code != 200) {
    throw new IllegalStateException('Injecting the SMS into SMPPSim failed with HTTP ' + code)
}
