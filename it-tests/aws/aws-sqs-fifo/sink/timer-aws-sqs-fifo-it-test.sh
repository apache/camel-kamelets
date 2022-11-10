#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: timer-aws-sqs-fifo-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run timer-aws-sqs-fifo.yaml &

sleep 10

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
echo $success $fail
if [[ $success == 5 && $fail == 0 ]] 
then 
    echo "Test Successful" > ../../../tests/timer-aws-sqs-fifo-it-test.result ;
else
    echo "Test failed" > ../../../tests/timer-aws-sqs-fifo-it-test.result ;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop timer-aws-sqs-fifo

cd terraform/
terraform destroy -auto-approve
cd ../
