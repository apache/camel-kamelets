# AWS EventBridge Tests

## Disabled Tests

### aws-eventbridge-sink.citrus.it.yaml.disabled

This test is currently disabled due to known limitations in LocalStack's EventBridge implementation.

**Issue**: EventBridge rules do not correctly forward events to SQS queue targets in LocalStack.

**Known LocalStack Issues**:
- https://github.com/localstack/localstack/issues/3070 - Events not forwarded to SQS
- https://github.com/localstack/localstack/issues/2843 - EventBridge target of SQS does not find queue
- https://github.com/localstack/localstack/issues/3586 - DetailType not handled correctly
- https://github.com/localstack/localstack/issues/5701 - Event pattern matching failures

**Test Configuration**: The test setup is correct:
- EventBridge rule with proper event pattern matching
- SQS queue with correct IAM policy allowing EventBridge to send messages
- Target properly configured with queue ARN
- Events sent with matching source and detail-type

**Status**: This test works against real AWS EventBridge but fails in LocalStack due to incomplete EventBridge-to-SQS routing implementation.

To re-enable when LocalStack fixes the issue, rename the file back to `aws-eventbridge-sink.citrus.it.yaml`.
