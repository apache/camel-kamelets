# Transformations

The tests verify the transformations done by the action Kamelets and the data types: `data-type-action`, the field
actions (`drop-field-action`, `extract-field-action`, `hoist-field-action`, `insert-field-action`,
`mask-field-action`, `replace-field-action`), the template actions (FreeMarker, Mustache, MVEL, String Template,
Velocity), `jolt-transformation-action`, `jslt-action` and `pdf-action`.

## Objectives

Each test runs a route, `<kamelet>-route.yaml`, that sends a message from a timer through the Kamelet under test to
the Citrus HTTP server. The test checks the body the Kamelet gives, its Java type (the `X-Body-Type` header) and its
`Content-Type` (the `X-Content-Type` header).

## Run the tests

Run all the transformation tests from the repository root:

```shell script
$ mvn verify -pl :camel-kamelets-itest -Denable.integration.tests -Dit.test=CommonIT#transformation
```

See [testing a Kamelet](../../../../../../docs/modules/ROOT/pages/testing.adoc) for how the tests are written.
