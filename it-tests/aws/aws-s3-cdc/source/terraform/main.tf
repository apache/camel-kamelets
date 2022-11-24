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

variable "s3_bucket_name" {
  type = string
  default = "s3-camel-test-123"
}

variable "sqs_queue_name" {
  type = string
  default = "sqs-camel-test-123"
}


data "aws_caller_identity" "current" {}

# Create a new S3 bucket
resource "aws_s3_bucket" "MyS3Bucket" {
  bucket = var.s3_bucket_name
  force_destroy = true
}

# Send notifications to EventBridge for all events in the bucket
resource "aws_s3_bucket_notification" "MyS3BucketNotification" {
  bucket      = aws_s3_bucket.MyS3Bucket.id
  eventbridge = true
}

# Create an EventBridge rule
resource "aws_cloudwatch_event_rule" "MyEventRule" {
  description   = "Object create events on bucket s3://${aws_s3_bucket.MyS3Bucket.id}"
  event_pattern = <<EOF
{
  "source": [
    "aws.s3"
  ],
  "detail": {
    "bucket": {
      "name": ["${aws_s3_bucket.MyS3Bucket.id}"]
    }
  }
}
EOF
}

# Set the SNS topic as a target of the EventBridge rule
resource "aws_cloudwatch_event_target" "MyEventRuleTarget" {
  rule      = aws_cloudwatch_event_rule.MyEventRule.name
  arn       = aws_sqs_queue.sqs-queue.arn
}

# Create a new SQS queue
resource "aws_sqs_queue" "sqs-queue" {
  name = var.sqs_queue_name
}

# Allow EventBridge to publish to the SQS queue
resource "aws_sqs_queue_policy" "MySQSQueuePolicy" {
  queue_url = aws_sqs_queue.sqs-queue.id
  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AWSEventsPermission",
      "Effect": "Allow",
      "Principal": {
        "Service": "events.amazonaws.com"
      },
      "Action": "sqs:SendMessage",
      "Resource": "${aws_sqs_queue.sqs-queue.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_cloudwatch_event_rule.MyEventRule.arn}"
        }
      }
    }
  ]
}
POLICY
}

# Display the EventBridge rule, S3 bucket and SQS queue
output "EventBridge-Rule-Name" {
  value       = aws_cloudwatch_event_rule.MyEventRule.name
  description = "The EventBridge Rule Name"
}
output "S3-Bucket" {
  value       = aws_s3_bucket.MyS3Bucket.id
  description = "The S3 Bucket"
}
output "SQS-Queue-Name" {
  value       = aws_sqs_queue.sqs-queue.name
  description = "The SQS Queue Name"
}
output "SQS-Queue-ARN" {
  value       = aws_sqs_queue.sqs-queue.arn
  description = "The SQS Queue Arn"
}
	
