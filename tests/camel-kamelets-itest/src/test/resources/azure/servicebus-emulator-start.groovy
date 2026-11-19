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

// Starts the Azure Service Bus emulator, with the queues orders and blob-events, and the SQL Server that it keeps its
// data in, on a network of their own, and gives the connection string of the namespace in the test variable
// servicebus.connectionString.
//
// String concatenation rather than Groovy GStrings, because Citrus resolves
// dollar-brace expressions in these scripts as its own test variables.

import java.time.Duration

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

// a password of the SQL Server of this test only, which the emulator logs in with
def sqlPassword = 'Kamelets_Test_2026'

def network = Network.newNetwork()

def sqlServer = new GenericContainer(DockerImageName.parse('mcr.microsoft.com/mssql/server:2022-CU28-ubuntu-22.04'))
        .withNetwork(network)
        .withNetworkAliases('sqlserver')
        .withEnv('ACCEPT_EULA', 'Y')
        .withEnv('MSSQL_SA_PASSWORD', sqlPassword)
        .waitingFor(Wait.forLogMessage('.*SQL Server is now ready for client connections.*', 1).withStartupTimeout(Duration.ofMinutes(3)))
sqlServer.start()

def serviceBus = new GenericContainer(DockerImageName.parse('mcr.microsoft.com/azure-messaging/servicebus-emulator:2.0.1'))
        .withNetwork(network)
        .withEnv('SQL_SERVER', 'sqlserver')
        .withEnv('MSSQL_SA_PASSWORD', sqlPassword)
        .withEnv('ACCEPT_EULA', 'Y')
        .withCopyFileToContainer(MountableFile.forClasspathResource('azure/servicebus-emulator-config.json'),
                '/ServiceBus_Emulator/ConfigFiles/Config.json')
        .withExposedPorts(5672)
        .waitingFor(Wait.forLogMessage('.*Emulator Service is Successfully Up.*', 1).withStartupTimeout(Duration.ofMinutes(3)))
serviceBus.start()

context.getReferenceResolver().bind('serviceBusNetwork', network)
context.getReferenceResolver().bind('serviceBusSqlServer', sqlServer)
context.getReferenceResolver().bind('serviceBusEmulator', serviceBus)

context.setVariable('servicebus.connectionString', 'Endpoint=sb://' + serviceBus.getHost() + ':' + serviceBus.getMappedPort(5672)
        + ';SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;')
