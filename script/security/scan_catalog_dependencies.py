#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Query OSV for the third party artifacts the Kamelet catalog pins.

Reads camel-kamelets-catalog-sbom.json, which lists the
"mvn:group:artifact:version" entries declared in spec.dependencies, and asks
https://osv.dev whether any of those exact versions has a known advisory.

Two groups are checked and reported apart. Artifacts the catalog pins are fixed
here by editing the Kamelet. Camel artifacts are versioned by the runtime, so an
advisory against one is fixed by moving Camel, and the version scanned is the
newest Camel release rather than the SNAPSHOT the catalog builds against -- OSV
answers nothing at all for a SNAPSHOT, which would read as an all-clear.

Writes a Markdown report to the path given by --report and exits 1 when
something is found, so a workflow can decide whether to raise an issue.
"""

import argparse
import json
import re
import sys
import urllib.error
import urllib.request

OSV_QUERYBATCH = "https://api.osv.dev/v1/querybatch"
OSV_VULN = "https://api.osv.dev/v1/vulns/"
CAMEL_METADATA = "https://repo1.maven.org/maven2/org/apache/camel/camel-core/maven-metadata.xml"
VERSIONED_BY_RUNTIME = "runtime"
TIMEOUT = 30


def post(url, payload):
    body = json.dumps(payload).encode()
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
        return json.load(response)


def get(url):
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        return json.load(response)


def read_components(sbom_path):
    with open(sbom_path) as handle:
        sbom = json.load(handle)
    components = []
    for component in sbom.get("components", []):
        props = {p.get("name"): p.get("value", "") for p in component.get("properties", [])}
        components.append({
            "coordinate": f"{component['group']}:{component['name']}",
            "version": component["version"],
            "declared_by": props.get("camel.apache.org/declared-by", ""),
            "runtime_versioned": props.get("camel.apache.org/versioned-by") == VERSIONED_BY_RUNTIME,
        })
    return components


def latest_camel_release():
    """OSV knows nothing about a SNAPSHOT, so scan the newest real release."""
    with urllib.request.urlopen(CAMEL_METADATA, timeout=TIMEOUT) as response:
        xml = response.read().decode()
    match = re.search(r"<release>([^<]+)</release>", xml)
    if not match:
        raise RuntimeError("no <release> in camel-core maven-metadata.xml")
    return match.group(1)


def resolve_runtime_versions(components):
    """Point the runtime-versioned entries at a version OSV can actually answer for."""
    if not any(c["runtime_versioned"] for c in components):
        return None
    release = latest_camel_release()
    for component in components:
        if component["runtime_versioned"]:
            component["scanned_version"] = release
    return release


def query(components):
    """One batch call, then fetch the details of whatever came back."""
    queries = [
        {"package": {"ecosystem": "Maven", "name": c["coordinate"]},
         "version": c.get("scanned_version", c["version"])}
        for c in components
    ]
    results = post(OSV_QUERYBATCH, {"queries": queries}).get("results", [])

    findings = []
    for component, result in zip(components, results):
        for vuln in result.get("vulns", []) or []:
            vuln_id = vuln.get("id")
            try:
                detail = get(OSV_VULN + vuln_id)
            except urllib.error.URLError:
                detail = {}
            findings.append({
                "component": component,
                "id": vuln_id,
                "summary": (detail.get("summary") or "").strip(),
                "aliases": detail.get("aliases", []),
                "severity": severity_of(detail),
            })
    return findings


def severity_of(detail):
    for entry in detail.get("severity", []) or []:
        if entry.get("score"):
            return entry["score"]
    for affected in detail.get("affected", []) or []:
        ecosystem = affected.get("ecosystem_specific", {}) or {}
        if ecosystem.get("severity"):
            return ecosystem["severity"]
    return "unrated"


def table(findings):
    lines = ["| artifact | version | advisory | severity | declared by |",
             "|---|---|---|---|---|"]
    for finding in sorted(findings, key=lambda f: (f["component"]["coordinate"], f["id"])):
        component = finding["component"]
        aliases = ", ".join(a for a in finding["aliases"] if a.startswith("CVE-"))
        advisory = f"[{finding['id']}](https://osv.dev/vulnerability/{finding['id']})"
        if aliases:
            advisory += f" ({aliases})"
        version = component.get("scanned_version", component["version"])
        lines.append(f"| `{component['coordinate']}` | `{version}` | {advisory} "
                     f"| {finding['severity']} | {component['declared_by']} |")
    return lines


def render(findings, components, camel_release):
    pinned_total = sum(1 for c in components if not c["runtime_versioned"])
    camel_total = sum(1 for c in components if c["runtime_versioned"])

    pinned = [f for f in findings if not f["component"]["runtime_versioned"]]
    camel = [f for f in findings if f["component"]["runtime_versioned"]]

    lines = []

    lines.append(f"## Artifacts the catalog pins ({pinned_total})\n")
    if pinned:
        lines.append("Each version below is pinned in `spec.dependencies` of the Kamelets named, "
                     "so bumping one is a change to this repository.\n")
        lines += table(pinned)
    else:
        lines.append("No known advisories.")
    lines.append("")

    lines.append(f"## Camel components ({camel_total}), scanned at {camel_release or 'n/a'}\n")
    if camel:
        lines.append("These are versioned by the runtime rather than by the catalog, so an advisory "
                     "here is addressed by moving Camel, not by editing a Kamelet. The version scanned "
                     "is the newest Camel release, which is not necessarily the one any given "
                     "deployment runs.\n")
        lines += table(camel)
    else:
        lines.append("No known advisories.")
    lines.append("")

    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sbom", default="camel-kamelets-sbom/camel-kamelets-catalog-sbom.json")
    parser.add_argument("--report", default="catalog-dependency-scan.md")
    args = parser.parse_args()

    components = read_components(args.sbom)
    if not components:
        print(f"No components in {args.sbom}", file=sys.stderr)
        return 2

    try:
        camel_release = resolve_runtime_versions(components)
    except (urllib.error.URLError, OSError, RuntimeError) as err:
        # Never fall through to a scan that would report these as clean.
        print(f"Could not resolve the Camel release to scan: {err}", file=sys.stderr)
        return 2

    try:
        findings = query(components)
    except (urllib.error.URLError, OSError) as err:
        print(f"Could not reach OSV: {err}", file=sys.stderr)
        return 2

    report = render(findings, components, camel_release)
    with open(args.report, "w") as handle:
        handle.write(report)
    print(report)
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
