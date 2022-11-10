#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: aws-sqs-log-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

queue_url=`aws sqs get-queue-url --queue-name s3-camel-test-123 --region eu-west-1 | jq -r '.QueueUrl'`

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run aws-sqs-log.yaml &

sleep 5

aws sqs send-message --queue-url $queue_url --message-body "Test" 
aws sqs send-message --queue-url $queue_url --message-body "Test" 
aws sqs send-message --queue-url $queue_url --message-body "Test" 
aws sqs send-message --queue-url $queue_url --message-body "Test" 
aws sqs send-message --queue-url $queue_url --message-body "Test" 

sleep 5

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    echo "Test Successful" > ../../../tests/aws-sqs-log-it-test.result;
else
    echo "Test failed" > ../../../tests/aws-sqs-log-it-test.result;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop aws-sqs-log

cd terraform/
terraform destroy -auto-approve
cd ../
