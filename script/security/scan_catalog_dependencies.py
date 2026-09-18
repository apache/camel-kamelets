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

Only the pinned artifacts are checked. "camel:<component>" dependencies are not
in the SBOM by design: the catalog does not choose their version, so advisories
against them belong to the Camel release in use rather than to this catalog.

Writes a Markdown report to the path given by --report and exits 1 when
something is found, so a workflow can decide whether to raise an issue.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request

OSV_QUERYBATCH = "https://api.osv.dev/v1/querybatch"
OSV_VULN = "https://api.osv.dev/v1/vulns/"
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
        declared_by = ""
        for prop in component.get("properties", []):
            if prop.get("name") == "camel.apache.org/declared-by":
                declared_by = prop.get("value", "")
        components.append({
            "coordinate": f"{component['group']}:{component['name']}",
            "version": component["version"],
            "declared_by": declared_by,
        })
    return components


def query(components):
    """One batch call, then fetch the details of whatever came back."""
    queries = [
        {"package": {"ecosystem": "Maven", "name": c["coordinate"]}, "version": c["version"]}
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


def render(findings, checked):
    lines = []
    if not findings:
        lines.append(f"No known advisories against the {checked} artifacts the catalog pins.")
        return "\n".join(lines) + "\n"

    lines.append(f"OSV reports advisories against {len({f['component']['coordinate'] for f in findings})} "
                 f"of the {checked} artifacts the catalog pins.\n")
    lines.append("| artifact | version | advisory | severity | declared by |")
    lines.append("|---|---|---|---|---|")
    for finding in sorted(findings, key=lambda f: (f["component"]["coordinate"], f["id"])):
        component = finding["component"]
        aliases = ", ".join(a for a in finding["aliases"] if a.startswith("CVE-"))
        advisory = f"[{finding['id']}](https://osv.dev/vulnerability/{finding['id']})"
        if aliases:
            advisory += f" ({aliases})"
        lines.append(f"| `{component['coordinate']}` | `{component['version']}` | {advisory} "
                     f"| {finding['severity']} | {component['declared_by']} |")

    lines.append("\nEach of these versions is pinned in `spec.dependencies` of the Kamelets named above, "
                 "so bumping one is a change to this repository rather than to Camel.")
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
        findings = query(components)
    except (urllib.error.URLError, OSError) as err:
        print(f"Could not reach OSV: {err}", file=sys.stderr)
        return 2

    report = render(findings, len(components))
    with open(args.report, "w") as handle:
        handle.write(report)
    print(report)
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
