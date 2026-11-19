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

// Prepares the OpenSSH server in the container ssh for a test: the directories orders and upload in the home
// directory (/config) of the user citrus, with the file orders/order 1.csv, whose name has a space, and a
// known_hosts file with the keys of the server (variable ssh.known.hosts) for the address the integration
// connects to.

import java.nio.file.Files

import org.testcontainers.containers.GenericContainer

def ssh = context.getReferenceResolver().resolve('ssh', GenericContainer.class)

def run = { String command ->
    def result = ssh.execInContainer('sh', '-c', command)
    if (result.getExitCode() != 0) {
        throw new IllegalStateException('Failed to run ' + command + ': ' + result.getStderr())
    }
    return result.getStdout()
}

run("mkdir -p /config/orders /config/upload && printf 'id,item\\n1,book\\n' > '/config/orders/order 1.csv' && chown -R 1000:1000 /config/orders /config/upload")

def address = '[' + ssh.getHost() + ']:' + ssh.getMappedPort(2222)
def knownHosts = Files.createTempFile('kamelets-ssh-known-hosts-', '')
Files.writeString(knownHosts, run('cat /config/ssh_host_keys/*.pub').readLines()
        .findAll { it.trim() }
        .collect { line -> def parts = line.split(' '); address + ' ' + parts[0] + ' ' + parts[1] }
        .join('\n') + '\n')
context.setVariable('ssh.known.hosts', knownHosts.toString())
