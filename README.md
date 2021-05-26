# Apache Camel - Kamelet Catalog

This repository contains the default Kamelet catalog used by Apache Camel and its sub-projects.

Kamelets in this repository can be used natively in [Apache Camel K](https://github.com/apache/camel-k) integrations, without additional configuration steps:
users just need to reference the Kamelets by name in the URI (e.g. `kamelet:timer-source?message=Hello`), or use them in a `KameletBinding`.

**NOTE**: Camel K (and other sub-projects) will only use a specific version of this Kamelet catalog. Refer to the release notes of the sub-project for more information.

Documents and guides about Kamelets can be found in the [Kamelets User and Developer Guides](https://camel.apache.org/camel-k/latest/kamelets/kamelets.html).

## Guidelines for contributions

Kamelets in this repository are intended to be generic connectors that any external platform can embed in order to leverage the Apache Camel integration capabilities.

All Kamelets posted here will be subject to the scrutiny of the Apache Camel PMC to assess their compliance with the ecosystem and, in any case, they **MUST** be in line with the general [Apache Code of Conduct](https://www.apache.org/foundation/policies/conduct.html).

### General Format

Kamelets **MUST** be provided in the *Kubernetes YAML* format, i.e. they **MUST** be resources that can be applied on a cluster using the Kubernetes `kubectl` CLI.

The file name of each Kamelet **MUST** follow this specific pattern: `<kamelet-name>.kamelet.yaml`. The `<kamelet-name>` **MUST** match field `metadata` -> `name` inside the Kamelet YAML.

For the time being, we'll accept only these kinds of Kamelets:

- **Sources**: Kamelets producing data that can be forwarded to any chosen destination. In the Camel jargon, a source can be used consumer-side.
Kamelets belonging to this category **MUST** be marked with label: `camel.apache.org/kamelet.type=source`.
- **Sinks**: Kamelets that accept data with a specific datashape and forward it to an external system. In the Camel jargon, a sink can be used producer-side.
Kamelets belonging to this category **MUST** be marked with label: `camel.apache.org/kamelet.type=sink`.
- **Actions**: Kamelets that can be used as intermediate steps as they both accept and produce data, applying transformations to it or changing the behavior of the whole integration flow (e.g. using enterprise integration patterns). Kamelets belonging to this category **MUST** be marked with label: `camel.apache.org/kamelet.type=action`.

All Kamelets **MUST** provide a value for label `camel.apache.org/kamelet.type`.

All Kamelets **MUST** declare an icon in the `camel.apache.org/kamelet.icon` annotation using the embedded URL `data:image` format. An icon annotation **CANNOT** contain a link to an external location. A Kamelet **SHOULD** use the specific `data:image/svg+xml;base64` format whenever it's possible.

Kamelets that are logically related (e.g. all Kamelets that allow doing things with Twitter) **SHOULD** be linked together using the label `camel.apache.org/kamelet.group=<name of the group>` (e.g. `camel.apache.org/kamelet.group=Twitter`) to ease visualization in tooling.

All Kamelets present in this repository **MUST** have the *annotation* `camel.apache.org/provider` set to `"Apache Software Foundation"`.

We provide an example of Kamelet to give more context to the following sections:

```yaml
apiVersion: camel.apache.org/v1alpha1
kind: Kamelet
metadata:
  name: timer-source
  annotations:
    camel.apache.org/kamelet.icon: data:image/svg+xml;base64,PD94...
    camel.apache.org/provider: "Apache Software Foundation"
  labels:
    camel.apache.org/kamelet.type: source
spec:
  definition:
    title: Timer Source
    description: Produces periodic events with a custom payload
    required:
      - message
    properties:
      period:
        title: Period
        description: The interval between two events in milliseconds
        type: integer
        default: 1000
      message:
        title: Message
        description: The message to generate
        type: string
        example: hello world
  types:
    out:
      mediaType: text/plain
  flow:
    from:
      uri: timer:tick
      parameters:
        period: "{{period}}"
      steps:
        - set-body:
            constant: "{{message}}"
        - to: kamelet:sink

```


### Flow Code

The Camel route that defines the behavior of the Kamelet **MUST** be provided in YAML flow syntax.
The Kamelet **MAY** declare additional supporting routes that **MUST** be written in YAML syntax (to provide better support in all Camel subprojects).

The code of a "source" Kamelet must send data to the `kamelet:sink` special endpoint. The code of a "sink" Kamelet must consume data from the special endpoint `kamelet:source`.

The Kamelet **MAY** declare dependencies on Camel components using the syntax `camel:<component-name>` (e.g. `camel:telegram`). Some Camel dependencies are implicitly
added by the runtime when a certain Camel URI is used (e.g. there's no need to declare explicit dependency on `camel:timer` if the `flow` section uses the `timer` URI).

The Kamelet **CAN** declare dependencies on artifacts of one of the Camel subprojects. In case it does, the Kamelet must contain label `camel.apache.org/requires.runtime=<name-of-the-project>` (e.g. `camel.apache.org/requires.runtime=camel-quarkus`).

The Kamelet **CAN** declare dependencies on other artifacts in Maven Central, provided that they are released with a license compatible with Apache License 2.0 (syntax is `mvn:group:artifact:version`).

All source code dependencies (e.g. `github:organization:project`) **MUST** link to Apache Camel repositories only. Contributors are welcome to use this same repository to share libraries in the form of source code.

All these should be added to the `spec` -> `dependencies` section, like in the following example:

```yaml
# ...
spec:
  # ...
  dependencies:
  - camel:telegram
  - mvn:org.apache.commons:commons-vfs2:2.7.0
  - github:apache/camel-kamelets
  flow:
    # ...
```

### Configuration properties

Kamelets **SHOULD** declare their configuration properties in the `spec` -> `definition` section as a JSON-schema specification.

The top-level part of the JSON-schema document refer to the Kamelet itself, so the following properties are **MANDATORY**:

- `title`: the visual name of the Kamelet
- `description`: a detailed description of what the Kamelet is useful for

The `properties` part can then contain a set of configuration properties available for the Kamelet.

Property name `id` is reserved and implicit for all Kamelets.

### Data Types

Kamelets **SHOULD** declare the `mediaType` of data that they produce or consume when it's known in advance. If technically possible, they **SHOULD** also indicate the schema of the corresponding data.

Kamelets **MAY** choose not to declare a `mediaType` when it varies depending on the configuration of the parameters. Contributors **MAY** consider to split a Kamelet into multiple instances if that can help determining unique types and schemas.

### Testing

Kamelets **SHOULD** be accompained with testing code that verifies their correct behavior.

[Yaks](https://github.com/citrusframework/yaks) is the testing framework of choice for Kamelets and the one implemented in the CI.

Test code must be submitted in the `test/<kamelet-name>/` directory in the root of this repository.

Kamelets submitted with tests that verify their correctness **MUST** be labeled with `camel.apache.org/kamelet.verified=true`.

**NOTE**: there's no way at the moment to inject credentials for external systems into the CI in order to write more advanced tests, but we can expect we'll find an usable strategy in the long run

## Releasing

This project is released as standard Apache Camel module.

To release it, first set the version of the project to the next tag and release it:

```
export CAMEL_KAMELET_VERSION=x.y.z

./mvnw versions:set -DnewVersion=$CAMEL_KAMELET_VERSION -DgenerateBackupPoms=false
```

Then, build the project to update Kamelet references:

```
./mvnw clean install
```

Stage the commits in SVN:

```
git commit -am "Prepare for release $CAMEL_KAMELET_VERSION"
```

Check that everything is alright with a dryRun:

```
./mvnw release:prepare -Prelease \
  -DdryRun \
  -DreleaseVersion=$CAMEL_KAMELET_VERSION \
  -DdevelopmentVersion=main-SNAPSHOT \
  -Dtag=v$CAMEL_KAMELET_VERSION
```

Check the signatures of the files, then clean and prepare the actual release:

```
./mvnw release:clean -Prelease
./mvnw release:prepare -Prelease \
  -DreleaseVersion=$CAMEL_KAMELET_VERSION \
  -DdevelopmentVersion=main-SNAPSHOT \
  -Dtag=v$CAMEL_KAMELET_VERSION
```

Then perform the release:

```
./mvnw release:perform -Prelease
```
