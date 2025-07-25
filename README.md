# Apache Camel - Kamelet Catalog

This repository contains the default Kamelet catalog used by Apache Camel and its sub-projects.

Kamelets in this repository can be used natively in Camel K, Camel, Camel-Quarkus and Camel-spring-boot integrations, without additional configuration steps. Users just need to reference the Kamelets by name in the URI (e.g. `kamelet:timer-source?message=Hello`), or use them in a `Pipe`, for Camel K, in particular.

> [!NOTE]
> Camel K (and other sub-projects) will only use a specific version of this Kamelet catalog. Refer to the release notes of the sub-project for more information.

Documents and guides about Kamelets can be found in the [Kamelets User and Developer Guides](https://camel.apache.org/camel-k/latest/kamelets/kamelets.html).

## Guidelines for contributions

Kamelets in this repository are intended to be generic connectors that any external platform can embed in order to leverage the Apache Camel integration capabilities.

All Kamelets posted here are subject to the scrutiny of the Apache Camel PMC to assess their compliance with the ecosystem and, in any case, they **MUST** be in line with the general [Apache Code of Conduct](https://www.apache.org/foundation/policies/conduct.html).

### Building and validating

Building the project:

```bash
$ ./mvnw clean install
```

> [!IMPORTANT]
> After adding or modifying a kamelet remember to generate:
> ```bash
> $ cd script/generator
> $ go run . ../../kamelets/ ../../docs/modules/ROOT/
> ```
> and validate:
> ```bash
> $ cd script/validator
> $ go run . ../../kamelets/
> ```
> be sure that there aren't reported ERRORS.

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
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: timer-source
  annotations:
    camel.apache.org/kamelet.icon: data:image/svg+xml;base64,PD94...
    camel.apache.org/provider: "Apache Software Foundation"
  labels:
    camel.apache.org/kamelet.type: "source"
spec:
  definition:
    title: Timer Source
    description: Produces periodic events with a custom payload.
    required:
      - message
    properties:
      period:
        title: Period
        description: The interval between two events in milliseconds.
        type: integer
        default: 1000
      message:
        title: Message
        description: The message to generat.
        type: string
        example: hello world
  types:
    out:
      mediaType: text/plain
  template:
    from:
      uri: timer:tick
      parameters:
        period: "{{period}}"
      steps:
        - set-body:
            constant: "{{message}}"
        - to: kamelet:sink

```


### Template Code

The Camel route that defines the behavior of the Kamelet **MUST** be provided in YAML flow syntax (in `.spec.template` parameter).
The Kamelet **MAY** declare additional supporting routes that **MUST** be written in YAML syntax (to provide better support in all Camel subprojects).

The code of a "source" Kamelet must send data to the `kamelet:sink` special endpoint. The code of a "sink" Kamelet must consume data from the special endpoint `kamelet:source`.

The Kamelet **MAY** declare dependencies on Camel components using the syntax `camel:<component-name>` (e.g. `camel:telegram`). Some Camel dependencies are implicitly
added by the runtime when a certain Camel URI is used (e.g. there's no need to declare explicit dependency on `camel:timer` if the `template` section uses the `timer` URI).

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
  template:
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

Kamelets **SHOULD** be accompanied by tests that verify their correct behavior.

[Citrus](https://github.com/citrusframework/citrus) is the testing framework of choice for Kamelets and the one implemented in the CI.

Test code must be submitted in the `tests/camel-kamelets-itests/src/test/resources/<kamelet-name>/` directory in the root of this repository.

Kamelets submitted with tests that verify their correctness **MUST** be labeled with `camel.apache.org/kamelet.verified=true`.

> [!NOTE]
> There's no way at the moment to inject credentials for external systems into the CI in order to write more advanced tests, but we can expect we'll find a usable strategy in the long run.

### Kamelet Pipe Examples

Pipe examples are highly encouraged to be added under `templates/pipes/camel-k` directory for Kamelet Pipe and `templates/pipes/core` for the YAML routes.

When the Kamelet Catalog documentation is generated, the examples in each Kamelet documentation page are automatically generated, but the generator code is not wise enough and it may generate a Kamelet Pipe that doesn't work, requiring additional steps. In this case, the pipe example should be added to the above mentioned directories, and add the comment marker at the first line `"# example_for_kamelet_doc"` only in the Camel K Kamelet Pipe example (in `templates/pipes/camel-k`). When the documentation mechanism runs, it will source this pipe example into the kamelet documentation page as example.

## Releasing

This project is released as standard Apache Camel module.

To release it, first set the next release version in the kamelets:

```bash
export CAMEL_KAMELET_VERSION=x.y.z

./mvnw clean install -DreleaseVersion=$CAMEL_KAMELET_VERSION
```

Stage the commits in the Source Version Control:

```bash
git commit -am "Update Kamelets for release $CAMEL_KAMELET_VERSION"

git push upstream main
```

Check that everything is alright with a dryRun:

```bash
./mvnw release:prepare -Prelease \
  -DdryRun \
  -DreleaseVersion=$CAMEL_KAMELET_VERSION \
  -DdevelopmentVersion=<next_snapshot> \
  -Dtag=v$CAMEL_KAMELET_VERSION
```

Check the signatures of the files, then clean and prepare the actual release:

```bash
./mvnw release:clean -Prelease
./mvnw release:prepare -Prelease \
  -DreleaseVersion=$CAMEL_KAMELET_VERSION \
  -DdevelopmentVersion=<next_snapshot> \
  -Dtag=v$CAMEL_KAMELET_VERSION
```

Then perform the release:

```bash
./mvnw release:perform -Prelease
```

Go to https://repository.apache.org/ and close the staging repository.

A URL is generated for the repository, like: https://repository.apache.org/content/repositories/orgapachecamel-xxxx. The URL needs to be communicated during the voting process.

Now run:

```bash
cd release-utils/scripts/
./upload-source.sh $CAMEL_KAMELET_VERSION $CAMEL_KAMELET_VERSION
```

You'll be requested to insert the password to unlock the secret key to sign the artifacts and after uploading to nexus dev repository.

You could verify the result at the following URL:

https://dist.apache.org/repos/dist/dev/camel/camel-kamelets/<$CAMEL_KAMELET_VERSION> 

Restore Kamelets:

```bash
./mvnw clean install
```

Update remote git:

```bash
git commit -am "Restore Kamelets for development"

git push upstream main
```

Send an email to dev mailing list to start the vote.

## Post Release

Once the vote for the release has been completed, you need to send the Vote Result mail to mailing list.

Now, you'll need to release the artifacts from Apache staging repositories to Apache releases repository.

To do this you'll need to access the Apache Nexus Server.

You'll need then to promote the release from dist/dev location to dist/release location.

There is an handy script for this:

Now run:

```bash
cd release-utils/scripts/
./promote-release.sh $CAMEL_KAMELET_VERSION 
```

### Updating documentation version

If we are talking about a minor release, it is enough to edit the 'docs/antora.yml' file like this:

```yaml
name: camel-kamelets
title: Kamelet Catalog
version: 0.9.x

nav:
- modules/ROOT/nav.adoc

asciidoc:
  attributes:
    requires: "'util=camel-website-util,kamelets=xref:js/kamelets.js'"
    # Update to appropriate released camel-k version on release
    version-used: true
    camel-k-version: 1.10.1
    camel-k-docs-version: 1.10.x
#    camel-kafka-connector-version: none
#    camel-kafka-connector-docs-version: none
    camel-version: 3.18.2
    camel-docs-version: 3.18.x
```

and modify the values accordingly.

After this step you'll need to open a PR to Camel-Website for listing the release. The location is the following: https://github.com/apache/camel-website/tree/main/content/releases/k

In the case of a major release, you'll need to create a new branch from the tag release.

You'll need to align the antora.yml file in docs accordingly.

After this step you'll need to open a PR to Camel-Website for listing the release. The location is the following: https://github.com/apache/camel-website/tree/main/content/releases/k

And also adding the branch to the documentation to be published here: https://github.com/apache/camel-website/blob/main/antora-playbook-snippets/antora-playbook.yml

In the camel-kamelets section.
