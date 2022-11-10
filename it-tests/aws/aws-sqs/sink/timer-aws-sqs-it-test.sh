#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: timer-aws-sqs-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run timer-aws-sqs.yaml &

sleep 10

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/timer-aws-sqs-it-test.result ;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/timer-aws-sqs-it-test.result ;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop timer-aws-sqs

cd terraform/
terraform destroy -auto-approve
cd ../

cat ../../../tests/timer-aws-sqs-it-test.result
