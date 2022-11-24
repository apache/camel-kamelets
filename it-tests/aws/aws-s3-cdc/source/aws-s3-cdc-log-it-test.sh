#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: aws-s3-cdc-log-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run --local-kamelet-dir=../../../../kamelets/ --properties=aws-keys.properties aws-s3-cdc-log.yaml &

sleep 5

aws s3api put-object --bucket s3-camel-test-123 --key example-file-uploaded-1.txt --body example-file.txt --region eu-west-1
aws s3api put-object --bucket s3-camel-test-123 --key example-file-uploaded-2.txt --body example-file.txt --region eu-west-1
aws s3api put-object --bucket s3-camel-test-123 --key example-file-uploaded-3.txt --body example-file.txt --region eu-west-1
aws s3api put-object --bucket s3-camel-test-123 --key example-file-uploaded-4.txt --body example-file.txt --region eu-west-1
aws s3api put-object --bucket s3-camel-test-123 --key example-file-uploaded-5.txt --body example-file.txt --region eu-west-1

sleep 20

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/aws-s3-cdc-log-it-test.result;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/aws-s3-cdc-log-it-test.result;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop aws-s3-cdc-log

cd terraform/
terraform destroy -auto-approve
cd ../

cat ../../../tests/aws-s3-cdc-log-it-test.result
