#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: timer-azure-storage-blob-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

sleep 10

accountKey=`az storage account keys list -n kameletsaccount | jq -r ' .[0] | .value'`
echo $accountKey
az storage account keys list -n kameletsaccount | echo "camel.kamelet.azure-storage-blob-sink.accessKey = $accountKey" > azure-keys.properties

jbang run --fresh -Dcamel.jbang.version=$camel_version camel@apache/camel run --properties=azure-keys.properties timer-azure-storage-blob.yaml &

sleep 30

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get integration timer-azure-storage-blob | tail -n +2` 
echo $variable 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
echo $success $fail
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/timer-azure-storage-blob-it-test.result ;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/timer-azure-storage-blob-it-test.result ;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop timer-azure-storage-blob

cd terraform/
terraform destroy -auto-approve
cd ../

rm -rf azure-keys.properties

cat ../../../tests/timer-azure-storage-blob-it-test.result
