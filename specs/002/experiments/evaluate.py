#!/usr/bin/env python3
"""Reproducible Linux-only F0 experiment; never imported by the BRICA server.

Run only with the synthetic fixture generator in this directory. Requires bwrap,
Java 25, previously resolved candidate JARs and an unpacked Tesseract runtime.
Results are written outside the repository. No shell, database or real files.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import resource
import selectors
import shutil
import signal
import socket
import subprocess
import time


JAVA_FLAGS = [
    "-Xms32m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m",
    "-XX:CompressedClassSpaceSize=64m", "-XX:ReservedCodeCacheSize=64m",
    "-Djava.awt.headless=true", "-Duser.home=/tmp", "-Djava.io.tmpdir=/tmp",
]


def sandbox_command(fixtures, dependencies, classes, ocr, command):
    args = [
        "bwrap", "--unshare-all", "--die-with-parent", "--new-session",
        "--cap-drop", "ALL", "--ro-bind", "/usr", "/usr",
        "--symlink", "usr/lib", "/lib", "--symlink", "usr/lib64", "/lib64",
        "--proc", "/proc", "--dev", "/dev", "--tmpfs", "/tmp",
        "--ro-bind", str(fixtures), "/fixtures",
        "--ro-bind", str(dependencies), "/deps",
        "--ro-bind", str(classes), "/classes",
        "--ro-bind", str(ocr), "/ocr", "--chdir", "/tmp", "--clearenv",
        "--setenv", "PATH", "/usr/bin", "--setenv", "HOME", "/tmp",
        "--setenv", "LANG", "C.UTF-8", "--setenv", "OMP_THREAD_LIMIT", "1",
        "--setenv", "LD_LIBRARY_PATH", "/ocr/usr/lib/x86_64-linux-gnu",
    ]
    java_home = Path(shutil.which("java")).resolve().parent.parent
    java_security = (java_home / "conf/security/java.security").resolve()
    for name in ("/etc/fonts", "/etc/ld.so.cache", str(java_security)):
        if Path(name).exists():
            args += ["--ro-bind", name, name]
    return args + ["--"] + command


def resource_limits():
    # Per-process bounds, NOT an aggregate cgroup or a disk quota.
    resource.setrlimit(resource.RLIMIT_AS, (2 * 1024**3,) * 2)
    resource.setrlimit(resource.RLIMIT_CPU, (60, 60))
    resource.setrlimit(resource.RLIMIT_FSIZE, (16 * 1024**2,) * 2)
    resource.setrlimit(resource.RLIMIT_NOFILE, (128, 128))
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))


def execute_bounded(command, timeout=120, output_limit=1024**2, pass_fds=()):
    """Bound combined stdout/stderr while streaming; kill the sandbox on overflow.

    Single-threaded experiment launcher: preexec_fn is not for reuse in a server.
    bwrap owns a PID namespace so its death also terminates namespaced children.
    """
    start = time.monotonic()
    chunks = {"stdout": bytearray(), "stderr": bytearray()}
    reason = None
    before = resource.getrusage(resource.RUSAGE_CHILDREN)
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          env={"PATH": "/usr/bin", "F0_TEST_SECRET": "synthetic-not-a-credential"},
                          start_new_session=True, preexec_fn=resource_limits, pass_fds=pass_fds) as process:
        with selectors.DefaultSelector() as selector:
            for name, stream in (("stdout", process.stdout), ("stderr", process.stderr)):
                selector.register(stream, selectors.EVENT_READ, name)
            try:
                while selector.get_map():
                    remaining = timeout - (time.monotonic() - start)
                    if remaining <= 0:
                        reason = "TIMEOUT"
                        break
                    for key, _ in selector.select(min(remaining, 0.1)):
                        data = os.read(key.fileobj.fileno(), 8192)
                        if not data:
                            selector.unregister(key.fileobj)
                        else:
                            chunks[key.data].extend(data)
                            if sum(map(len, chunks.values())) > output_limit:
                                reason = "OUTPUT_LIMIT"
                                break
                    if reason:
                        break
                if not reason:
                    remaining = timeout - (time.monotonic() - start)
                    if remaining <= 0:
                        reason = "TIMEOUT"
                    else:
                        try:
                            process.wait(timeout=remaining)
                        except subprocess.TimeoutExpired:
                            reason = "TIMEOUT"
            finally:
                if process.poll() is None:
                    os.killpg(process.pid, signal.SIGKILL)
                process.wait()
    after = resource.getrusage(resource.RUSAGE_CHILDREN)
    output_bytes = sum(map(len, chunks.values()))
    return {
        "returncode": process.returncode, "reason": reason,
        "observed_output_bytes": output_bytes,
        "seconds": round(time.monotonic() - start, 3),
        # getrusage high-water mark may include earlier children: explicitly not per-step RSS.
        "children_peak_rss_kib_cumulative": after.ru_maxrss,
        "cpu_seconds": round(after.ru_utime + after.ru_stime - before.ru_utime - before.ru_stime, 3),
        **{name: bytes(value[:16384]).decode("utf-8", errors="replace") for name, value in chunks.items()},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath", type=Path, required=True)
    parser.add_argument("--ocr-root", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True, help="New external result directory")
    args = parser.parse_args()
    base = Path(__file__).resolve().parent
    work = args.work.resolve()
    if work.exists() or work.is_relative_to(base.parents[2]):
        parser.error("--work must be a NEW directory outside the repository")
    ocr = args.ocr_root.resolve(strict=True)
    jars = [Path(p).resolve(strict=True) for p in args.classpath.read_text().strip().split(os.pathsep)]
    if not shutil.which("bwrap") or not (ocr / "usr/bin/tesseract").is_file():
        parser.error("bwrap and unpacked Tesseract required; no unsandboxed fallback")
    work.mkdir(parents=True, mode=0o700)
    fixtures, deps, classes = (work / p for p in ("fixtures", "deps", "classes"))
    for directory in (fixtures, deps, classes):
        directory.mkdir(mode=0o700)
    for jar in jars:
        target = deps / jar.name
        if target.exists():
            parser.error("Duplicate JAR filename in classpath")
        shutil.copyfile(jar, target)
    cp = os.pathsep.join(str(p) for p in sorted(deps.glob("*.jar")))
    subprocess.run(["javac", "-cp", cp, "-d", str(classes), str(base / "F0Probe.java")], check=True)
    # Trusted source builds synthetic inputs; no untrusted source parsing occurs here.
    subprocess.run(["java", *JAVA_FLAGS, "-cp", cp + os.pathsep + str(classes),
                    "F0Probe", "build", str(fixtures)], check=True, timeout=60)
    manifest = [{"name": p.name, "bytes": p.stat().st_size,
                 "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(fixtures.iterdir())]
    (work / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    sandbox = lambda command: sandbox_command(fixtures, deps, classes, ocr, command)
    results = {}

    def expect(name, command, *, reason=None, timeout=10):
        result = execute_bounded(sandbox(command), timeout=timeout)
        result["passed"] = result["reason"] == reason and (reason is not None or result["returncode"] == 0)
        results[name] = result
        print(("PASS " if result["passed"] else "FAIL ") + name, flush=True)

    py = lambda source: ["/usr/bin/python3", "-c", source]
    marker = work / "private-marker.txt"
    marker.write_text("synthetic host marker")
    expect("host-files-hidden", py(f"from pathlib import Path; assert not Path({str(marker)!r}).exists(); assert not Path('/home/brian/Proyectos/BRICA/.env').exists()"))
    expect("environment-cleared", py("import os; assert 'F0_TEST_SECRET' not in os.environ; assert not any(k.startswith('DB_') for k in os.environ)"))
    expect("input-read-only", py("from pathlib import Path\nassert Path('/fixtures/S01-digital.pdf').is_file()\ntry:\n Path('/fixtures/new').write_text('x')\nexcept OSError:\n pass\nelse:\n raise AssertionError('writable input')"))
    with socket.socket() as host:
        host.bind(("127.0.0.1", 0))
        host.listen(1)
        port = host.getsockname()[1]
        host_network = os.readlink("/proc/self/ns/net")
        expect("host-network-blocked", py(f"import socket,os\nassert os.readlink('/proc/self/ns/net') != {host_network!r}\ns=socket.socket(); s.settimeout(1)\ntry:\n s.connect(('127.0.0.1',{port}))\nexcept OSError:\n pass\nelse:\n raise AssertionError('host reachable')"))
    expect("address-space-limit", py("import resource; assert resource.getrlimit(resource.RLIMIT_AS)[0] == 2*1024**3\ntry:\n x=bytearray(3*1024**3)\nexcept MemoryError:\n pass\nelse:\n raise AssertionError('memory bound absent')"))
    expect("file-size-limit", py("import os\nf=os.open('/tmp/large',os.O_CREAT|os.O_WRONLY,0o600)\ntry:\n os.ftruncate(f,17*1024**2)\nexcept OSError:\n pass\nelse:\n raise AssertionError('file bound absent')"))
    expect("output-limit", py("import os\nwhile True: os.write(1,b'x'*8192)"), reason="OUTPUT_LIMIT")
    expect("timeout-with-child", py("import subprocess,time; subprocess.Popen(['/usr/bin/python3','-c','import time; time.sleep(600)']); time.sleep(600)"), reason="TIMEOUT", timeout=1)
    expect("ocr-version", ["/ocr/usr/bin/tesseract", "--version"])
    java = str(Path(shutil.which("java")).resolve(strict=True))
    expect("readers-and-ocr", [java, *JAVA_FLAGS, "-cp", "/deps/*:/classes", "F0Probe", "read", "/fixtures"], timeout=120)
    unchanged = all(hashlib.sha256((fixtures / p["name"]).read_bytes()).hexdigest() == p["sha256"] for p in manifest)
    report = {"schema": 1, "synthetic_only": True, "sources_unchanged": unchanged, "results": results,
              "all_passed": unchanged and all(r["passed"] for r in results.values()),
              "limitations": ["No aggregate memory/PID cgroup or tmpfs quota", "Not verified inside production image",
                              "Not production parsing/confirmation or complete corpus acceptance"]}
    (work / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    print("Report: " + str(work / "report.json"))
    raise SystemExit(0 if report["all_passed"] else 1)


if __name__ == "__main__":
    main()
