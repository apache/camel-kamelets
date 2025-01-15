# Kamelets Citrus Tests

This folder contains a suite of integration tests for Kamelets

You need the following tools to run the tests:
- Java 17
- [JBang](https://www.jbang.dev/)
- [Camel JBang](https://camel.apache.org/manual/camel-jbang.html)
- [Citrus JBang App](https://citrusframework.org/citrus/reference/html/index.html#runtime-jbang-install)

Once everything is set you just need to run

```console
  mvn verify -Denable.integration.tests
```

This runs all available Citrus tests.

You can run individual tests when specifying its folder name on the class `KameletsIT` (e.g. `aws`).

```console
  mvn verify -Dtest=KameletsIT#aws
```

You can run the tests also from your favorite Java IDE.
By default, the Citrus tests use local runtime where Camel integrations, Kamelets, bindings and pipes are run with Camel JBang.
