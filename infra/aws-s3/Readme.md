# Apache Camel Kamelets - AWS S3 Commodity Configuration files

In this directory you'll find:

- Ansible
- Terraform
- Cloudformation 

Configuration files, to be used for creating your infra for using AWS S3 Kamelets.

This will create an S3 Bucket with basic functionalities.

## Ansible

The commmand to create the infra is:

```bash
$ ansible-playbook -v ansible/aws-s3.yaml --extra-vars bucket_name=<bucket-name>
```

## Terraform

The commmand to create the infra is:

```bash
$ cd terraform/
$ terraform init
$ terraform apply -var="bucket_name=<bucket-name>"
```

Once completed do:

```bash
$ cd terraform/
$ terraform destroy
```

## Cloudformation

The commmand to create the infra is:

```bash
$ aws cloudformation deploy --template-file cloudformation/aws-s3.yaml --stack-name my-new-stack --parameter-overrides BucketName=<bucket-name>
```

Once completed do:

```bash
$ aws cloudformation delete-stack --stack-name my-new-stack
```


