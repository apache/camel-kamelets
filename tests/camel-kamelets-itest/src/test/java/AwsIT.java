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

@CitrusSupport
public class AwsIT {

    private final int awsSinkServerPort = SocketUtils.findAvailableTcpPort();

    @BindToRegistry
    HttpServer awsSinkServer = http()
                .server()
                .port(awsSinkServerPort)
                .timeout(10000L)
                .autoStart(true)
                .build();

    @BindToRegistry
    public SequenceBeforeTest beforeAws() {
        return new SequenceBeforeTest.Builder().onTests("aws-*").actions(
                createVariables().variable("aws.sink.server.port", String.valueOf(awsSinkServerPort))
        ).build();
    }

    @BindToRegistry
    public SequenceAfterTest afterAws() {
        return new SequenceAfterTest.Builder().onTests("aws-*").actions(
                purgeEndpoints().endpoint(awsSinkServer)
        ).build();
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsS3() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.s3");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsSqs() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.sqs");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsDdb() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.ddb");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsKinesis() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.kinesis");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsEventBridge() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.eventbridge");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsSns() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.sns");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsCloudWatch() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.cloudwatch");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsEc2() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.ec2");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsFirehose() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.firehose");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsSecretsManager() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.secretsmanager");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsSes() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.ses");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsComprehend() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.comprehend");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsLambda() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.lambda");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsPolly() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.polly");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsTimestream() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.timestream");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsBedrock() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.bedrock");
    }

    @CitrusTestFactory
    public Stream<DynamicTest> awsCloudTrail() {
        return CitrusTestFactorySupport.factory(TestLoader.YAML).packageScan("aws.cloudtrail");
    }

}
