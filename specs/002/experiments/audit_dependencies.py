#!/usr/bin/env python3
"""Inventory candidate Maven artifacts and query OSV outside the reader sandbox.

This is evidence, not a security certification. Failure of the online query is a
failure, never an empty vulnerability report. POM inheritance is recorded as a
limitation when the artifact has no direct license declaration.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Output already exists; retain previous evidence")
    artifacts = []
    for path in args.classpath.read_text().strip().split(":"):
        jar = Path(path).resolve(strict=True)
        pom = ET.parse(jar.with_suffix(".pom")).getroot()
        # Some published POMs (e.g. XMLBeans) omit the default XML namespace.
        for element in pom.iter():
            element.tag = element.tag.rsplit("}", 1)[-1]
        value = pom.findtext
        group = value("groupId") or value("parent/groupId")
        version = value("version") or value("parent/version")
        if not all((group, value("artifactId"), version)) or any("${" in s for s in (group, value("artifactId"), version)):
            raise ValueError(f"Unresolved artifact identity: {jar.name}")
        licenses = [n.findtext("name") for n in pom.findall("licenses/license")]
        with zipfile.ZipFile(jar) as archive:
            notices = {n: hashlib.sha256(archive.read(n)).hexdigest() for n in archive.namelist()
                       if n.upper().startswith("META-INF/") and ("LICENSE" in n.upper() or "NOTICE" in n.upper())}
        artifacts.append({"package": f"{group}:{value('artifactId')}", "version": version,
                          "jar_sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                          "direct_pom_licenses": licenses, "embedded_notices_sha256": notices})
    queries = [{"package": {"ecosystem": "Maven", "name": a["package"]}, "version": a["version"]} for a in artifacts]
    request = urllib.request.Request("https://api.osv.dev/v1/querybatch",
                                     data=json.dumps({"queries": queries}).encode(),
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=60) as response:
        results = json.load(response)["results"]
    if len(results) != len(artifacts):
        raise RuntimeError("Incomplete OSV result")
    for artifact, result in zip(artifacts, results):
        artifact["osv_response"] = result
    report = {"schema": 1, "queried_at_utc": datetime.now(timezone.utc).isoformat(),
              "endpoint": "https://api.osv.dev/v1/querybatch", "artifacts": artifacts,
              "limitations": ["Candidate standalone graph, not Spring Boot dependency mediation",
                              "No absence-of-vulnerabilities guarantee or native runtime scan",
                              "Missing direct POM license requires parent/embedded notice review"]}
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(f"Audited {len(artifacts)} artifacts; report: {args.output}")
    if any(a["osv_response"].get("vulns") for a in artifacts):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
