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

// Prepares the share files of the Samba server in the container smb for a test: the directories orders and
// upload, with the file orders/order 1.csv, whose name has a space, the file upload/day 1.csv, which the
// sink test replaces, and the files 0.txt (1 byte), a.txt and B.txt (2 bytes each) at the top of the share.
// It also turns on guest access: the user Guest, with no password, logs in as a guest (map to guest = Bad User,
// guest ok = yes on the share), and adds the share secure, the directory /shared/secure, which requires encryption
// (smb encrypt = required).

import org.testcontainers.containers.GenericContainer

def smb = context.getReferenceResolver().resolve('smb', GenericContainer.class)

def command = "mkdir -p /shared/orders /shared/upload && printf 'id,item\\n1,book\\n' > '/shared/orders/order 1.csv'" +
        " && printf 'old' > '/shared/upload/day 1.csv'" +
        " && printf '0' > /shared/0.txt && printf 'aa' > /shared/a.txt && printf 'BB' > /shared/B.txt" +
        " && chown -R citrus /shared && chgrp -R smb /shared" +
        " && sed -i 's/^\\[global\\]/[global]\\n    map to guest = Bad User/' /etc/samba/smb.tmp" +
        " && printf '   guest ok = yes\\n' >> /etc/samba/smb.tmp" +
        " && mkdir -p /shared/secure && chown citrus /shared/secure && chgrp smb /shared/secure" +
        " && printf '[secure]\\n    path = /shared/secure\\n    read only = no\\n    smb encrypt = required\\n" +
        "    force user = citrus\\n    force group = smb\\n' >> /etc/samba/smb.tmp" +
        " && smbcontrol smbd reload-config"
def result = smb.execInContainer('sh', '-c', command)
if (result.getExitCode() != 0) {
    throw new IllegalStateException('Failed to prepare the share: ' + result.getStderr())
}
