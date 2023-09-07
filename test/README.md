# Kamelets YAKS Tests

This folder contains a suite of integration tests for Kamelets

You need the following tools to run the tests:
- Java 17
- [JBang](https://www.jbang.dev/)
- [Camel JBang](https://camel.apache.org/manual/camel-jbang.html)
- [YAKS](https://github.com/citrusframework/yaks)

Once everything is set you just need to run

```console
  make yaks
```

This runs all available YAKS tests that are not marked as `@ignored`.

You can run individual tests when specifying its folder or feature file name.

```console
  make yaks test=timer-source/timer-source.feature
```

The Makefile is a wrapper for the YAKS binary which is able to run the BDD Gherkin feature files. 
You can also run YAKS tooling directly form your local machine.
By default, the YAKS tests use local runtime where Camel integrations, Kamelets, bindings and pipes are run with Camel JBang.
