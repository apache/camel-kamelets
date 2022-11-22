#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: sftp-log-it-test.sh camel-version
    exit 1
fi

camel_version=$1

docker pull emberstack/sftp
docker run -p 24:22 --name sftp -d emberstack/sftp 
sftpid=`docker ps -aqf "name=sftp"`

echo $sftpid

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel run --local-kamelet-dir=../../../../kamelets/ sftp-log.yaml &

sleep 10

docker exec -it $sftpid bash -c "echo 'Ciao' >> /home/demo/sftp/demos/file1.txt"
docker exec -it $sftpid bash -c "echo 'Ciao' >> /home/demo/sftp/demos/file2.txt"
docker exec -it $sftpid bash -c "echo 'Ciao' >> /home/demo/sftp/demos/file3.txt"
docker exec -it $sftpid bash -c "echo 'Ciao' >> /home/demo/sftp/demos/file4.txt"
docker exec -it $sftpid bash -c "echo 'Ciao' >> /home/demo/sftp/demos/file5.txt"

sleep 10

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/sftp-log-it-test.result;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/sftp-log-it-test.result;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop sftp-log

docker stop sftp
docker rm sftp

cat ../../../tests/sftp-log-it-test.result
