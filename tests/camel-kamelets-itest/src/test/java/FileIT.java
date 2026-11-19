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

import java.util.stream.Stream;

import org.citrusframework.api.common.TestLoader;
import org.citrusframework.container.SequenceAfterTest;
import org.citrusframework.container.SequenceBeforeTest;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.junit.jupiter.CitrusTestFactory;
import org.citrusframework.junit.jupiter.CitrusTestFactorySupport;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.DynamicTest;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariables;
import static org.citrusframework.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

/**
 * Tests of the file Kamelets: file-watch-source on a local directory, the FTP and FTPS Kamelets against an
 * Apache FtpServer started in the test, and the SFTP, SCP and SSH Kamelets against an OpenSSH server in a
 * container.
 */
@CitrusSupport
public class FileIT {

    private static final String TESTS = "(file-watch|ftp|ftps|sftp|scp|ssh)-.*";

    private final int fileHttpServerPort = SocketUtils.findAvailableTcpPort();

    // the FTP server runs in the test, on a free port of this machine; FTP opens a second connection for each
    // transfer, which a server in a container cannot offer on a port that is mapped to a random one
    private final int ftpServerPort = SocketUtils.findAvailableTcpPort();

    // a container and a first Camel JBang run that downloads the libraries take a while
    @BindToRegistry
    HttpServer fileHttpServer = http()
                .server()
                .port(fileHttpServerPort)
                .timeout(120000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeFile() {
        return new SequenceBeforeTest.Builder().onTests(TESTS).actions(
                createVariables()
                        .variable("file.http.server.port", String.valueOf(fileHttpServerPort))
                        .variable("ftp.server.port", String.valueOf(ftpServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterFile() {
        return new SequenceAfterTest.Builder().onTests(TESTS).actions(
                purgeEndpoints().endpoint(fileHttpServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> fileWatch() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("filewatch");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> ftp() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("ftp");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> ssh() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("ssh");
    }
}
