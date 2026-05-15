# Apache Camel Kamelets - AI Agent Guidelines

Guidelines for AI agents working on this codebase.

## Project Info

The Apache Camel Kamelet Catalog is the default catalog of reusable Camel route
templates ("Kamelets"), distributed as Kubernetes-style YAML and consumed by
Camel, Camel K, Camel Quarkus and Camel Spring Boot.

- Version: 4.20.0
- Camel: 4.20.0
- Java: 17+
- Build: Maven (`mvn verify`); Go is required for the `script/` and `crds/`
  generators
- Issue tracker: GitHub — https://github.com/apache/camel-kamelets/issues
- Related repository: `apache/camel` (Camel core — the Kamelet **runtime**
  lives there, not here)

## AI Agent Rules of Engagement

These rules apply to ALL AI agents working on this codebase.

### Attribution

- All AI-generated content (GitHub PR descriptions, review comments, issue
  comments) MUST clearly identify itself as AI-generated and mention the human
  operator. Example: "_Claude Code on behalf of [Human Name]_"

### PR Volume

- An agent MUST NOT open more than 10 PRs per day per operator to ensure human
  reviewers can keep up.
- Prioritize quality over quantity — fewer well-tested PRs are better than many
  shallow ones.

### Git branch

- An agent MUST NEVER push commits to a branch it did not create.
- If a contributor's PR needs changes, the agent may suggest changes via review
  comments, but must not push to their branch without explicit permission.
- An agent should prefer to use its own fork to push branches instead of the
  main `apache/camel-kamelets` repository, to avoid filling the main repository
  with uncleaned branches.
- Branch names: fix-issue → `ci-issue-<ISSUE_NUMBER>`, quick-fix →
  `quick-fix/<short-slug>`, CI fix → `ci-fix/<short-slug>`. Include the topic
  and issue number where possible.
- After a Pull Request is merged or rejected, delete the branch.

### GitHub Issue Ownership

- An agent MUST ONLY pick up **unassigned** GitHub issues.
- If an issue is already assigned to a human, the agent must not reassign it or
  work on it.
- Before starting work, assign the issue to its operator.
- Beginner tasks carry the `good first issue` label; experienced tasks carry
  `help wanted`.

### Commits

- Fix an issue: `Fix #<ISSUE_NUMBER>: <brief description>`
- Quick fix / chore: `chore: <brief description>`
- CI fix: `ci: <brief description>`
- Always reference the GitHub issue when applicable. The repository accepts
  **squash** and **rebase** merges only (no merge commits).

### PR Description Maintenance

When pushing new commits to a PR, **always update the PR description** (and
title if needed) to reflect the current state of the changeset. Use
`gh pr edit --title "..." --body "..."` after each push.

### PR Reviewers

When creating a PR, **always identify and request reviews** from the most
relevant committers:

- Run `git log --format='%an' --since='1 year' -- <affected-files> | sort | uniq -c | sort -rn | head -10`
  to find who has been most active on the affected files.
- Use `git blame` on key modified files to identify who wrote the code.
- Cross-reference with the
  [committer list](https://home.apache.org/committers-by-project.html#camel).
- Request review from **at least 2 relevant committers** via
  `gh pr edit --add-reviewer`.
- When all comments are addressed and checks are green, re-request review.

### Merge Requirements

- An agent MUST NOT merge a PR if there are any **unresolved review
  conversations**.
- An agent MUST NOT merge a PR without at least **one human approval**.
- An agent MUST NOT approve its own PRs — human review is always required.

### Code Quality

- Every new or changed Kamelet SHOULD include Citrus tests under
  `tests/camel-kamelets-itest/src/test/resources/<kamelet-name>/`; Kamelets with
  passing behaviour tests are labelled `camel.apache.org/kamelet.verified=true`.
- After adding or modifying a Kamelet, **regenerate and commit** the generated
  docs and validate, or CI will fail:
  ```bash
  cd script/generator && go run . ../../kamelets/ ../../docs/modules/ROOT/
  cd ../validator     && go run . ../../kamelets/      # must report no ERRORS
  ```
- `nav.adoc` and the per-Kamelet doc pages are **generated** — do not hand-edit
  them.
- A full `mvn verify` from the repository root must pass before pushing.

### Asynchronous Testing

Do **NOT** use `Thread.sleep()` in test code; it leads to flaky, slow,
non-deterministic tests. Use the project's Citrus test constructs (or
Awaitility, where Java test code applies) with an explicit timeout instead.

### Issue Investigation (Before Implementation)

Before implementing a fix, **thoroughly investigate** the issue. Kamelets are a
long-lived shared catalog — a template often looks "wrong" but exists for a
reason (compatibility with a Camel component default, an explicit insecure
convenience Kamelet, an intentional inbound-header mapping).

1. **Validate the issue** — confirm it is real and reproducible; question
   assumptions in the description.
2. **Check git history** — `git log --oneline <file>` and `git blame <file>`;
   read commit messages and linked issues.
3. **Search for related issues/PRs** on GitHub for prior discussion or
   intentional decisions.
4. **Check the developer guide** — `docs/modules/ROOT/pages/development.adoc`
   and the catalog `README.md` for authoring rules.
5. **Check the runtime boundary** — if the behaviour is in the `kamelet:`
   component, `{{property}}` placeholder binding, or
   `org.apache.camel.kamelets.utils.*`, the fix belongs in **`apache/camel`**,
   not here.
6. **Check if the "fix" reverts prior work** — if so, stop and reconsider; if
   still justified, acknowledge it explicitly in the PR description.

**Present findings** to the operator before implementing. Flag risks,
ambiguities, or cases where the issue may be invalid.

### Knowledge Cutoff Awareness

AI agents have a training cutoff. **Never make authoritative claims about
external project state (Camel component options, dependency versions) based
solely on training knowledge** — verify against the Camel catalog, Maven
Central, or release notes before relying on or questioning a version.

### Documentation Conventions

When writing or modifying `.adoc` documentation:

- Use `xref:` for internal links, never external `https://camel.apache.org/...`
  URLs for pages that exist in this module.
- Do not hand-edit generated pages (`nav.adoc`, per-Kamelet pages); change the
  Kamelet YAML and regenerate.
- When reviewing doc PRs, check `xref:` links and anchors resolve.

## Security Model

The Kamelet Catalog has a documented security model that defines who is
trusted, where the trust boundaries sit, what counts as a **catalog**
vulnerability, and what is route-author or operator responsibility. The
canonical document is
[`docs/modules/ROOT/pages/security-model.adoc`](docs/modules/ROOT/pages/security-model.adoc).
It **specialises** the
[Apache Camel Security Model](https://camel.apache.org/manual/security-model.html);
where the catalog model is silent, the Camel model governs. Use it as the
reference when triaging security reports, deciding whether a finding warrants a
CVE, or reviewing a security-sensitive Kamelet PR.

For the vulnerability **reporting** convention,
[`SECURITY.md`](SECURITY.md) at the repository root is the entry point GitHub
and security tooling expect. It points to the security model for scope and to
the Apache Camel ASF process for private disclosure. An agent that discovers or
is handed a suspected vulnerability MUST NOT open a public issue, PR, or
mailing-list post about it — follow the private process and stop.

### Trust assumptions

- **Kamelet authors and the Camel PMC** are trusted: the Kamelet template *is*
  route code, reviewed by the PMC. The catalog's special obligation is that the
  *template author is the catalog*, so a shipped template must be
  safe-by-default for the untrusted-data boundary.
- **Route authors and operators** are **fully trusted**. They bind every
  property — `url`, `query`, `template`, `expression`, `executable`, file
  paths, credentials — from configuration. Binding a property to
  attacker-controlled data is route-author error, not a catalog vulnerability.
- **Data flowing through a Kamelet** (the message a source emits or a
  sink/action consumes) is **untrusted**. This is the primary attacker model.
- **The Kamelet runtime is not in this repository.** The `kamelet:` component,
  placeholder binding and `org.apache.camel.kamelets.utils.*` live in
  `apache/camel`; defects there are routed to that project.

The fundamental trust boundary is between **the Kamelet (template + bound
configuration)** and **the data flowing through it** — unchanged from Camel,
except the author of the trusted template is now the catalog.

### What is in scope (concise summary)

A report is in scope when a **shipped Kamelet template**, in its default
configuration, lets untrusted data cross a boundary the template — not the
operator's wiring — should have held:

- A sink/action template that maps an untrusted inbound header/body into a
  dispatch position (`CamelHttpUri`, `CamelFileName`, `Camel*DestinationName`,
  `CamelExec*`, `CamelBeanMethodName`, …) without stripping/fixing the
  dispatch headers it does not deliberately consume.
- A template that passes message data (not a `{{property}}`) to a
  `simple`/template-language/JSONPath/query evaluator the Kamelet's purpose did
  not require.
- A template that ships a Camel component with a security-relevant **insecure
  default** (Java serialisation on an untrusted consumer, TLS verification off,
  admin surface on `0.0.0.0`, permissive header filter), reachable just by
  deploying the Kamelet.
- A secret property not marked `format: password` +
  `x-descriptors: [urn:camel:group:credentials]`, or a missing `pattern:`
  that turns operator contract into reachable unintended behaviour
  (hardening tier).

### What is out of scope

- An operator/route author binding a property (`{{template}}`, `{{query}}`,
  `{{expression}}`, `{{url}}`, `{{executable}}`, credentials, paths) to
  untrusted data — including all template-language and SQL/NoSQL/GraphQL
  Kamelets.
- A Kamelet doing, by design, the dangerous thing it is **named** for
  (`exec-sink`, `ssh-*`, `scp-sink`) or network exposure of a source
  (`webhook-source`, `http-source`); `*-secured-*` means auth *options* exist,
  not that auth is on by default.
- Explicitly-named insecure conveniences (`*-not-secured-*`,
  `kafka-not-secured-*`).
- Underlying Camel component or transitive-dependency CVEs not caused by the
  template.
- Defects in the Kamelet execution runtime (route to `apache/camel`).
- DoS via resource exhaustion (operator applies throttling/limits).
- The `data:image` icon annotation (metadata for tooling, never executed).
- `camel-kamelets-catalog` "parsing YAML" — it reads only build-bundled
  classpath YAML, not untrusted documents.
- Build/CI/test/scaffolding code (`script/`, `crds/` generator, `tests/`,
  `templates/`, `kamelets-maven-plugin`).
- Scanner reports without a PoC through a shipped template.

### Operator hardening checklist

When reviewing or recommending a deployment, surface:

- Treat a deployed Kamelet exactly like a route you wrote — same privileges and
  trust.
- Load Kamelets only from a trusted, integrity-checked catalog (an entity that
  can modify a Kamelet definition has arbitrary code execution by design).
- Never bind a property from untrusted message data.
- Strip `Camel*` headers from untrusted producers before a sink Kamelet, even
  though many templates also do this for known dispatch headers.
- Do not place `exec-sink` / `ssh-*` / `scp-sink` downstream of untrusted
  input.
- Secure inbound source Kamelets with network controls and the `*-secured-*`
  auth options.
- Resolve credentials through a Camel vault, not plaintext properties.
- Pin the catalog and Camel versions; follow Camel security announcements.

### Reviewer checklist (for security-sensitive Kamelet PRs)

When reviewing a PR that adds or changes a Kamelet template:

- Does the template map an untrusted inbound header/body into a
  dispatch-controlling position? It MUST strip or fix every Camel-internal
  header it does not deliberately consume, before the dispatching step
  (compare `http-sink`'s `removeHeader: CamelHttpUri`).
- Does the template pass message data (not a `{{property}}`) to an
  expression/template/query evaluator? That is the in-scope injection class —
  the evaluated input must be a bound property.
- Does the template add a component with a security-relevant default? Ship the
  safe default; if it must be relaxed, name the Kamelet `*-not-secured-*`,
  document it, and get PMC sign-off.
- Does a property carry a secret? It MUST be `format: password` with
  `x-descriptors: [urn:camel:group:credentials]`.
- Does a free-form property feed an endpoint/resource URI? Add a `pattern:`
  (operator-typo containment — not a trust control).
- Does the change relax a default or widen what a template forwards? It needs
  an upgrade-guide entry and PMC review.

## Structure

```
camel-kamelets/
├── kamelets/                    # ~250 *.kamelet.yaml route templates (the product)
├── library/
│   ├── camel-kamelets/          # resource bundle (jars the YAML)
│   ├── camel-kamelets-bom/      # Maven BOM (pom only)
│   ├── camel-kamelets-catalog/  # runtime metadata reader (Java)
│   ├── camel-kamelets-crds/     # Fabric8-generated K8s CRD POJOs (Java)
│   └── kamelets-maven-plugin/   # build-time validation plugin
├── crds/                        # Go CRD client generator (build/CI)
├── script/                      # Go doc generator + YAML validator (build/CI)
├── templates/                   # init .vm template + Pipe examples
├── tests/camel-kamelets-itest/  # Citrus integration tests
└── docs/modules/ROOT/           # Antora AsciiDoc (security-model.adoc lives here)
```

## Build

```bash
mvn verify                                   # full build (from root)
mvn verify -Pcoverage                        # with coverage
cd script/generator && go run . ../../kamelets/ ../../docs/modules/ROOT/  # regen docs
cd script/validator && go run . ../../kamelets/                            # validate
```

## Kamelet Authoring Conventions

- One `*.kamelet.yaml` per Kamelet; file name MUST match `metadata.name`.
- Each Kamelet is exactly one of `source`, `sink`, or `action`
  (`camel.apache.org/kamelet.type` label, mandatory).
- `camel.apache.org/provider` MUST be `"Apache Software Foundation"`.
- Icons MUST be embedded `data:image` (no external URLs).
- Source templates send to `kamelet:sink`; sink templates consume from
  `kamelet:source`.
- Dependencies go in `spec.dependencies` (`camel:<component>`,
  `mvn:group:artifact:version` with Apache-compatible license, or
  `github:apache/...` source only).
- Properties are declared as JSON-schema in `spec.definition`; mark secrets
  `format: password` + `x-descriptors: [urn:camel:group:credentials]`.

## Links

- https://camel.apache.org/
- https://github.com/apache/camel-kamelets
- https://github.com/apache/camel-kamelets/issues
- https://camel.apache.org/camel-k/latest/kamelets/kamelets.html
- https://camel.apache.org/security/
- dev@camel.apache.org
