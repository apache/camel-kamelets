# Apache Camel Kamelets - AWS SQS Commodity Configuration files

In this directory you'll find:

- Ansible
- Terraform
- Cloudformation 

Configuration files, to be used for creating your infra for using AWS S3 Kamelets.

This will create an SQS Queue with basic functionalities.

## Ansible

The commmand to create the infra is:

```bash
ansible-playbook -v ansible/aws-sqs.yaml --extra-vars queue_name=<queue_name> --extra-vars region=<region>
```

Once completed do:

```bash
ansible-playbook -v ansible/aws-sqs-removal.yaml --extra-vars queue_name=<queue_name> --extra-vars region=<region>
```

## Terraform

The commmand to create the infra is:

```bash
$ cd terraform/
$ terraform init
$ terraform apply -var="queue_name=<queue-name>"
```

Once completed do:

```bash
$ cd terraform/
$ terraform destroy
```

## Cloudformation

The commmand to create the infra is:

```bash
$ aws cloudformation deploy --template-file cloudformation/aws-sqs.yaml --stack-name my-new-stack --parameter-overrides QueueName=<queue-name>
```

Once completed do:

```bash
$ aws cloudformation delete-stack --stack-name my-new-stack
```


