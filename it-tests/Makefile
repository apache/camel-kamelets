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

tests:
	mkdir tests
	cd aws/aws-s3/source/ && \
	./aws-s3-log-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-s3/sink/ && \
	./timer-aws-s3-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-kinesis/source/ && \
	./aws-kinesis-log-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-kinesis/sink/ && \
	./timer-aws-kinesis-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-sqs/source/ && \
	./aws-sqs-log-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-sqs/sink/ && \
	./timer-aws-sqs-it-test.sh 3.20.0-SNAPSHOT
	cd aws/aws-sqs-fifo/sink/ && \
	./timer-aws-sqs-fifo-it-test.sh 3.20.0-SNAPSHOT
	./scripts/results.sh
	rm -rf tests
