// ---------------------------------------------------------------------------
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ---------------------------------------------------------------------------

// Starts the Azure Event Hubs emulator, with the event hub orders of one partition, and the Azurite storage emulator
// that it keeps its data in, on a network of their own, and gives the connection string of the event hub in the
// test variable eventhubs.connectionString.
//
// String concatenation rather than Groovy GStrings, because Citrus resolves
// dollar-brace expressions in these scripts as its own test variables.

import java.time.Duration

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

def network = Network.newNetwork()

def azurite = new GenericContainer(DockerImageName.parse('mcr.microsoft.com/azure-storage/azurite:3.37.0'))
        .withNetwork(network)
        .withNetworkAliases('azurite')
        .waitingFor(Wait.forLogMessage('.*Azurite Table service is successfully listening.*', 1))
azurite.start()

def eventHubs = new GenericContainer(DockerImageName.parse('mcr.microsoft.com/azure-messaging/eventhubs-emulator:2.2.1'))
        .withNetwork(network)
        .withEnv('BLOB_SERVER', 'azurite')
        .withEnv('METADATA_SERVER', 'azurite')
        .withEnv('ACCEPT_EULA', 'Y')
        .withCopyFileToContainer(MountableFile.forClasspathResource('azure/eventhubs-emulator-config.json'),
                '/Eventhubs_Emulator/ConfigFiles/Config.json')
        .withExposedPorts(5672)
        .waitingFor(Wait.forLogMessage('.*Emulator Service is Successfully Up.*', 1).withStartupTimeout(Duration.ofMinutes(3)))
eventHubs.start()

context.getReferenceResolver().bind('eventHubsNetwork', network)
context.getReferenceResolver().bind('eventHubsAzurite', azurite)
context.getReferenceResolver().bind('eventHubsEmulator', eventHubs)

context.setVariable('eventhubs.connectionString', 'Endpoint=sb://' + eventHubs.getHost() + ':' + eventHubs.getMappedPort(5672)
        + ';SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;EntityPath=orders')
