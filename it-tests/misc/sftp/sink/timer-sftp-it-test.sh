#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: timer-sftp-it-test.sh camel-version
    exit 1
fi

camel_version=$1

docker pull emberstack/sftp
docker run -p 24:22 --name sftp -d emberstack/sftp 

sleep 5

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run --local-kamelet-dir=../../../../kamelets/ timer-sftp.yaml &

sleep 10

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/timer-sftp-it-test.result ;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/timer-sftp-it-test.result ;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop timer-sftp

docker stop sftp
docker rm sftp

cat ../../../tests/timer-sftp-it-test.result
