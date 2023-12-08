# Kamelets YAKS Tests

This folder contains a suite of integration tests for Kamelets

You need the following tools to run the tests:
- Java 17
- [JBang](https://www.jbang.dev/)
- [Camel JBang](https://camel.apache.org/manual/camel-jbang.html)
- [YAKS](https://github.com/citrusframework/yaks)

Once everything is set you just need to run

```console
  mvn verify -Denable.yaks.tests
```

This runs all available YAKS tests that are not marked as `@ignored`.

You can run individual tests when specifying its folder or feature file name.

```console
  mvn verify -Dcucumber.feature=aws/s3/aws-s3-to-http.feature -Denable.yaks.tests
```

You can run the tests also from your favorite Java IDE.
By default, the YAKS tests use local runtime where Camel integrations, Kamelets, bindings and pipes are run with Camel JBang.
