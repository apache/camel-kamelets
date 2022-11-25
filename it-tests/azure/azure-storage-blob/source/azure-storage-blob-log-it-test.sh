#!/bin/bash

if [ $# -ne 1 ]; then
    echo $0: usage: azure-storage-blob-log-it-test.sh camel-version
    exit 1
fi

camel_version=$1

cd terraform/
terraform init
terraform apply -auto-approve
cd ../

accountKey=`az storage account keys list -n kameletsaccount | jq -r ' .[0] | .value'`
az storage account keys list -n kameletsaccount | echo "camel.kamelet.azure-storage-blob-source.accessKey = $accountKey" > azure-keys.properties

jbang run --fresh -Dcamel.jbang.version=$camel_version camel@apache/camel run --properties=azure-keys.properties azure-storage-blob-log.yaml &

sleep 5

az storage blob upload --account-name kameletsaccount --container-name kamelets --name example-file-uploaded1.txt --file example-file.txt --account-key $accountKey
az storage blob upload --account-name kameletsaccount --container-name kamelets --name example-file-uploaded2.txt --file example-file.txt --account-key $accountKey
az storage blob upload --account-name kameletsaccount --container-name kamelets --name example-file-uploaded3.txt --file example-file.txt --account-key $accountKey
az storage blob upload --account-name kameletsaccount --container-name kamelets --name example-file-uploaded4.txt --file example-file.txt --account-key $accountKey
az storage blob upload --account-name kameletsaccount --container-name kamelets --name example-file-uploaded5.txt --file example-file.txt --account-key $accountKey

sleep 5

variable=`jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel get | tail -n +2` 
success=`echo $variable | cut -d' ' -f11`
fail=`echo $variable | cut -d' ' -f12`
if [[ $success == 5 && $fail == 0 ]] 
then 
    mkdir -p ../../../tests/
    echo "Test Successful" > ../../../tests/azure-storage-blob-log-it-test.result;
else
    mkdir -p ../../../tests/
    echo "Test failed" > ../../../tests/azure-storage-blob-log-it-test.result;
fi

jbang run -Dcamel.jbang.version=$camel_version camel@apache/camel stop azure-storage-blob-log

cd terraform/
terraform destroy -auto-approve
cd ../

rm -rf azure-keys.properties

cat ../../../tests/azure-storage-blob-log-it-test.result
