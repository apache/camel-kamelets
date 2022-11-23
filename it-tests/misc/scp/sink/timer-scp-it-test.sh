#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: timer-scp-it-test.sh camel-version
    exit 1
fi

camel_version=$1

docker pull rastasheep/ubuntu-sshd:14.04
docker run -d -P --name test_sshd -p 32768:22 rastasheep/ubuntu-sshd:14.04

sleep 5

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run --local-kamelet-dir=../../../../kamelets/ timer-scp.yaml &

sleep 10

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/timer-scp-it-test.result ;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/timer-scp-it-test.result ;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop timer-scp

docker stop test_sshd
docker rm test_sshd

cat ../../../tests/timer-scp-it-test.result
