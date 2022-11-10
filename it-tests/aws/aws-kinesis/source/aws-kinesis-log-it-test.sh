#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: aws-kinesis-log-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run aws-kinesis-log.yaml &

sleep 5

aws kinesis put-record --stream-name s3-camel-test-123 --data test --partition-key 1
aws kinesis put-record --stream-name s3-camel-test-123 --data test --partition-key 1
aws kinesis put-record --stream-name s3-camel-test-123 --data test --partition-key 1
aws kinesis put-record --stream-name s3-camel-test-123 --data test --partition-key 1
aws kinesis put-record --stream-name s3-camel-test-123 --data test --partition-key 1

sleep 5

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    echo "Test Successful" > ../../../tests/aws-kinesis-log-it-test.result;
else
    echo "Test failed" > ../../../tests/aws-kinesis-log-it-test.result;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop aws-kinesis-log

cd terraform/
terraform destroy -auto-approve
cd ../
