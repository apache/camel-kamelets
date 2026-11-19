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

import software.amazon.awssdk.services.s3.S3Client

S3Client s3 = context.getReferenceResolver().resolve("s3StreamingClient", S3Client.class)

def keys = s3.listObjectsV2(b -> b.bucket('${aws.s3.bucketNameOrArn}')).contents().collect { it.key() }
assert keys == ['logs/app.log'] : "Expected one file named logs/app.log but got " + keys

String content = s3.getObjectAsBytes(b -> b.bucket('${aws.s3.bucketNameOrArn}').key('logs/app.log')).asUtf8String()
assert content == 'line 1;line 2;' : "Unexpected file content '" + content + "'"
