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

import org.citrusframework.exceptions.CitrusRuntimeException
import software.amazon.awssdk.services.s3.S3Client

S3Client s3 = context.getReferenceResolver().resolve("firehoseS3Client", S3Client.class)

def objects = s3.listObjectsV2(b -> b.bucket('firehose-bucket').prefix('orders/')).contents()
if (objects.size() != 1) {
    // Fails this attempt, so that repeatOnError asks again
    throw new CitrusRuntimeException("Expected one file in the bucket but got " + objects)
}

String content = s3.getObjectAsBytes(b -> b.bucket('firehose-bucket').key(objects[0].key())).asUtf8String()
// Floci ends each record that it writes with a line break; Amazon Data Firehose writes the record as it is
if (!(content == 'order ORD-1\n')) {
    throw new org.citrusframework.exceptions.CitrusRuntimeException("Unexpected file content '" + content + "'")
}
