# Slack Kamelet test

This test verifies the Slack Kamelets defined in [slack-source.kamelet.yaml](slack-source.kamelet.yaml)

## Objectives

The test verifies the Slack Kamelets by creating a Camel K integration that uses the Kamelet to listen for messages on a
Slack channel using the Slack bot API.

### Test Kamelets

The test performs the following high level steps:

*Preparation*
- Create a Slack API server that will simulate the Slack API (e.g. providing a webhook URL or verifying the Slack API channel history calls)

*Scenario* 
- Create the Camel K integration that uses the Slack Kamelets
- Wait for the Camel K integration to start and listen for Slack messages
- Create a new message on the Slack channel
- Verify that the integration has received the message event

*Cleanup*
- Delete the Camel K integration

## Installation

The test assumes that you have [JBang](https://www.jbang.dev/) installed and the Citrus CLI setup locally.

You can review the installation steps for the tooling in the documentation:

- [Install Camel K operator](https://camel.apache.org/camel-k/latest/installation/installation.html)
- [Install Citrus JBang App](https://citrusframework.org/citrus/reference/html/index.html#runtime-jbang-install)

## Run the test

```shell script
$ citrus run src/test/resources/slack/slack-source.it.yaml
$ citrus run src/test/resources/slack/slack-sink.it.yaml
```

You will be provided with the test log output and the test results.
