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

// Checks that the bucket s3.bucket has the file s3.key, with the content s3.content, read with the S3 client
// s3.client of the Floci container, and, when s3.contentType is set, with that content type.

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

S3Client s3 = context.getReferenceResolver().resolve(context.getVariable('s3.client'), S3Client.class)

def response = s3.getObjectAsBytes(GetObjectRequest.builder()
        .bucket(context.getVariable('s3.bucket'))
        .key(context.getVariable('s3.key'))
        .build())

String content = response.asUtf8String()
String expected = context.getVariable('s3.content')
if (!(content == expected)) {
    throw new org.citrusframework.exceptions.CitrusRuntimeException("Expected '" + expected + "' but got '" + content + "'")
}

if (context.getVariables().containsKey('s3.contentType')) {
    String contentType = response.response().contentType()
    String expectedType = context.getVariable('s3.contentType')
    if (!(contentType == expectedType)) {
        throw new org.citrusframework.exceptions.CitrusRuntimeException("Expected content type '" + expectedType + "' but got '" + contentType + "'")
    }
}
