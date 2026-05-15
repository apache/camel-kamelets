# Security Policy

The Apache Camel Kamelet Catalog is an Apache Camel sub-project and follows the
Apache Camel security process.

## Supported Versions

To see which versions of Apache Camel (and the Kamelet Catalog shipped with
them) are supported, please refer to this
[page](https://camel.apache.org/download/).

## Reporting a Vulnerability

For information on how to report a new security problem please see
[here](https://camel.apache.org/security/).

Do not open a public GitHub issue or pull request for an unpublished
vulnerability — follow the private ASF process and stop.

## Security Model

Before submitting a report, please read the project's
[Security Model](docs/modules/ROOT/pages/security-model.adoc). It documents who
is trusted, where the trust boundaries sit, which classes the Camel PMC accepts
as a Kamelet Catalog vulnerability, and which categories are out of scope
(route-author or operator responsibility, a Kamelet doing the dangerous thing it
is named for, the Kamelet execution runtime that lives in `apache/camel`,
third-party CVEs not caused by a template, DoS through unthrottled routes, etc.).
It specialises the
[Apache Camel Security Model](https://camel.apache.org/manual/security-model.html);
where this catalog's model is silent, the Camel model governs. Reports outside
the documented scope will be closed with a reference to that page.
