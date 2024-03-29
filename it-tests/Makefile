# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

all:
	mkdir -p tests
	cd aws/aws-s3/source/ && \
	./aws-s3-log-it-test.sh $(camel-version)
	cd aws/aws-s3/sink/ && \
	./timer-aws-s3-it-test.sh $(camel-version)
	cd aws/aws-kinesis/source/ && \
	./aws-kinesis-log-it-test.sh $(camel-version)
	cd aws/aws-kinesis/sink/ && \
	./timer-aws-kinesis-it-test.sh $(camel-version)
	cd aws/aws-sqs/source/ && \
	./aws-sqs-log-it-test.sh $(camel-version)
	cd aws/aws-sqs/sink/ && \
	./timer-aws-sqs-it-test.sh $(camel-version)
	cd aws/aws-sqs-fifo/sink/ && \
	./timer-aws-sqs-fifo-it-test.sh $(camel-version)
	cd aws/aws-sns/sink/ && \
	./timer-aws-sns-it-test.sh $(camel-version)
	cd aws/aws-kinesis-firehose/sink/ && \
	./timer-aws-kinesis-firehose-it-test.sh $(camel-version)
	cd misc/sftp/source/ && \
	./sftp-log-it-test.sh $(camel-version)
	cd misc/sftp/sink/ && \
	./timer-sftp-it-test.sh $(camel-version)
	cd misc/scp/sink/ && \
	./timer-scp-it-test.sh $(camel-version)
	cd azure/azure-storage-blob/source/ && \
	./azure-storage-blob-log-it-test.sh $(camel-version)
	cd azure/azure-storage-blob/sink/ && \
	./timer-azure-storage-blob-it-test.sh $(camel-version)
	./scripts/results.sh
	rm -rf tests	
aws:
	mkdir -p tests
	cd aws/aws-s3/source/ && \
	./aws-s3-log-it-test.sh $(camel-version)
	cd aws/aws-s3/sink/ && \
	./timer-aws-s3-it-test.sh $(camel-version)
	cd aws/aws-kinesis/source/ && \
	./aws-kinesis-log-it-test.sh $(camel-version)
	cd aws/aws-kinesis/sink/ && \
	./timer-aws-kinesis-it-test.sh $(camel-version)
	cd aws/aws-sqs/source/ && \
	./aws-sqs-log-it-test.sh $(camel-version)
	cd aws/aws-sqs/sink/ && \
	./timer-aws-sqs-it-test.sh $(camel-version)
	cd aws/aws-sqs-fifo/sink/ && \
	./timer-aws-sqs-fifo-it-test.sh $(camel-version)
	cd aws/aws-sns/sink/ && \
	./timer-aws-sns-it-test.sh $(camel-version)
	cd aws/aws-kinesis-firehose/sink/ && \
	./timer-aws-kinesis-firehose-it-test.sh $(camel-version)
	./scripts/results.sh
	rm -rf tests
azure:
	mkdir -p tests
	cd azure/azure-storage-blob/source/ && \
	./azure-storage-blob-log-it-test.sh $(camel-version)
	cd azure/azure-storage-blob/sink/ && \
	./timer-azure-storage-blob-it-test.sh $(camel-version)
	./scripts/results.sh
	rm -rf tests	
misc:
	mkdir -p tests
	cd misc/sftp/source/ && \
	./sftp-log-it-test.sh $(camel-version)
	cd misc/sftp/sink/ && \
	./timer-sftp-it-test.sh $(camel-version)
	cd misc/scp/sink/ && \
	./timer-scp-it-test.sh $(camel-version)
	./scripts/results.sh
	rm -rf tests
	
cdc:
	mkdir -p tests
	cd aws/aws-s3-cdc/source/ && \
	./aws-s3-cdc-log-it-test.sh $(camel-version)
	./scripts/results.sh
	rm -rf tests
	
.PHONY: all aws azure misc cdc clean
