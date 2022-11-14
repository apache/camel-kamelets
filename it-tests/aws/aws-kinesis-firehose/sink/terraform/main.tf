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

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.27"
    }
  }

  required_version = ">= 0.14.9"
}

provider "aws" {
  profile = "default"
  region  = "eu-west-1"
}

variable "kinesis_delivery_stream_name" {
  type = string
  default = "kinesis-camel-test-123"
}

resource "aws_kinesis_firehose_delivery_stream" "extended_s3_stream" {
  name        = var.kinesis_delivery_stream_name
  destination = "extended_s3"

  extended_s3_configuration {
    role_arn   = aws_iam_role.firehose_role.arn
    bucket_arn = aws_s3_bucket.bucket.arn
  }
}

variable "s3_bucket_delivery_stream_name" {
  type = string
  default = "s3-camel-test-123"
}


resource "aws_s3_bucket" "bucket" {
  bucket = var.s3_bucket_delivery_stream_name
}

resource "aws_iam_role" "firehose_role" {
  name = "firehose_test_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "firehose.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

output "Kinesis-firehose-dist-stream" {
  value       = aws_kinesis_firehose_delivery_stream.extended_s3_stream.arn
  description = "The Kinesis Stream ARN"
}
output "S3-Bucket" {
  value       = aws_s3_bucket.bucket.id
  description = "The S3 Bucket"
}	
