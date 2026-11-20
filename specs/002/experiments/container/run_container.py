#!/usr/bin/env python3
"""Host-side runner for a named experimental image, no Docker access in payload.

Accepts only a prior synthetic F0 evaluation directory. Creates/stops/removes
only the containers whose IDs this invocation obtained. Does not touch Compose,
networks, volumes, images or application containers.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
sys.dont_write_bytecode = True
from create_profile import DERIVED_SHA256


def docker(*args, timeout=30, check=True):
    return subprocess.run(["docker", *args], capture_output=True, text=True, timeout=timeout, check=check)


def run(mode, image_id, profile, inputs, work):
    # Stress at 256 MiB to prove aggregate OOM without allocating a GiB on the host.
    memory = "256m" if mode == "memory" else "1g"
    args = ["create", "--label", "brica.experiment=spec002-f0", "--network", "none", "--read-only",
            "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
            "--security-opt", "seccomp=" + str(profile), "--memory", memory,
            "--memory-swap", memory, "--pids-limit", "64", "--cpus", "1",
            "--ulimit", "core=0", "--tmpfs", "/tmp:rw,nosuid,nodev,noexec,size=128m,mode=1777",
            "--env", "F0_TEST_SECRET=synthetic-not-a-credential", "--env", "DB_PASSWORD=synthetic-not-a-credential"]
    for name in ("fixtures", "deps", "classes"):
        args += ["--mount", f"type=bind,src={inputs/name},dst=/{name},readonly"]
    args += [image_id, mode]
    container = docker(*args).stdout.strip()
    if len(container) != 64 or any(c not in "0123456789abcdef" for c in container):
        raise RuntimeError("Invalid container ID")
    started = time.monotonic()
    try:
        before = json.loads(docker("inspect", container).stdout)[0]
        config = before["HostConfig"]
        restrictions = (config["ReadonlyRootfs"] and not config["Privileged"] and not config["CapAdd"]
                        and config["CapDrop"] == ["ALL"] and config["NetworkMode"] == "none"
                        and "no-new-privileges" in config["SecurityOpt"]
                        and len(before["Mounts"]) == 3
                        and all(m["Type"] == "bind" and not m["RW"]
                                and m["Destination"] in ("/fixtures", "/deps", "/classes") for m in before["Mounts"]))
        if not restrictions:
            raise RuntimeError("Container configuration differs from the restricted experiment")
        # Reviewable evidence: no bind to Docker socket, source repository or persistent volumes.
        (work / f"{mode}-config.json").write_text(json.dumps({
            "id": container, "image": before["Image"], "user": before["Config"]["User"],
            "mounts": before["Mounts"], "host_config": before["HostConfig"]}, indent=2) + "\n")
        result = docker("start", "--attach", container, timeout=180, check=False)
        state = json.loads(docker("inspect", container).stdout)[0]["State"]
        (work / f"{mode}-stdout.json").write_text(result.stdout)
        (work / f"{mode}-stderr.txt").write_text(result.stderr)
        report = json.loads(result.stdout)
        expected_memory = 256 * 1024**2 if mode == "memory" else 1024**3
        limits = report["container_limits"]
        valid = (int(limits["memory.max"]) == expected_memory and limits["memory.swap.max"] == "0"
                 and limits["pids.max"] == "64" and limits["cpu.max"] == "100000 100000")
        return {"mode": mode, "container_id": container, "state": state,
                "seconds": round(time.monotonic() - started, 3), "limits_verified": valid,
                "restrictions_verified": restrictions,
                "checks": len(report["results"]), "all_passed": result.returncode == 0 and state["ExitCode"] == 0 and valid and report["all_passed"]}
    finally:
        # IDs are created by this invocation, never selected by name/pattern or project labels.
        state = json.loads(docker("inspect", container).stdout)[0]["State"]
        if state["Running"]:
            docker("kill", container)
        docker("rm", container)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--image", required=True)
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--synthetic-run", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    work = args.work.resolve()
    if work.exists() or work.is_relative_to(Path(__file__).resolve().parents[4]):
        parser.error("--work must be new and outside the repository")
    source = args.synthetic_run.resolve(strict=True)
    previous = json.loads((source / "report.json").read_text())
    if not previous["synthetic_only"] or not previous["all_passed"]:
        parser.error("Requires a successful synthetic F0 run")
    manifest = json.loads((source / "manifest.json").read_text())
    if {p.name for p in (source / "fixtures").iterdir()} != {p["name"] for p in manifest}:
        parser.error("Fixture set differs from recorded synthetic manifest")
    for entry in manifest:
        path = source / "fixtures" / entry["name"]
        if path.parent != source / "fixtures" or hashlib.sha256(path.read_bytes()).hexdigest() != entry["sha256"]:
            parser.error("Invalid synthetic fixture manifest")
    profile = args.profile.resolve(strict=True)
    if hashlib.sha256(profile.read_bytes()).hexdigest() != DERIVED_SHA256:
        parser.error("Unreviewed setup profile; use the pinned create_profile.py output")
    image = json.loads(docker("image", "inspect", args.image).stdout)[0]
    if image["Architecture"] != "amd64":
        parser.error("Only the amd64 image/profile was evaluated")
    image_id = image["Id"]
    work.mkdir(parents=True, mode=0o700)
    inputs = work / "inputs"
    for name in ("fixtures", "deps", "classes"):
        shutil.copytree(source / name, inputs / name)
        # Different non-root container UID needs read access to these SYNTHETIC copies only.
        for path in [inputs / name, *(inputs / name).rglob("*")]:
            path.chmod(0o755 if path.is_dir() else 0o644)
    reports = []
    for mode in ("normal", "pids", "memory"):
        report = run(mode, image_id, profile, inputs, work)
        reports.append(report)
        print(("PASS " if report["all_passed"] else "FAIL ") + mode, flush=True)
    summary = {"schema": 1, "image_id": image_id, "profile_sha256": hashlib.sha256(profile.read_bytes()).hexdigest(),
               "runs": reports, "all_passed": all(r["all_passed"] for r in reports)}
    (work / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print("Report: " + str(work / "summary.json"))
    return 0 if summary["all_passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
