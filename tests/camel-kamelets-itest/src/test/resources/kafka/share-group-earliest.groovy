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

// A new share group starts at the newest records. Set the group configuration share.auto.offset.reset
// to earliest before the group first reads, so that the test does not depend on when the share consumers
// have joined the group: the records can be sent as soon as the integration is started.
//
//  - kafka.bootstrapServers: the Kafka broker
//  - kafka.share.group:      the name of the share group

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.AlterConfigOp
import org.apache.kafka.clients.admin.ConfigEntry
import org.apache.kafka.common.config.ConfigResource

import java.util.concurrent.TimeUnit

def servers = context.getVariable('kafka.bootstrapServers')
def group = context.getVariable('kafka.share.group')

def admin = Admin.create([(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG): servers])
try {
    def resource = new ConfigResource(ConfigResource.Type.GROUP, group)
    def op = new AlterConfigOp(new ConfigEntry('share.auto.offset.reset', 'earliest'), AlterConfigOp.OpType.SET)
    admin.incrementalAlterConfigs([(resource): [op]]).all().get(30, TimeUnit.SECONDS)
} finally {
    admin.close()
}
