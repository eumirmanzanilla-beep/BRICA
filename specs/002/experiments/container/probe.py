#!/usr/bin/env python3
"""Synthetic F0 container experiment. Does not start the BRICA application."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, "/experiment")
from evaluate import JAVA_FLAGS, execute_bounded
from payload_filter import create_filter


def cgroup_value(name):
    return Path("/sys/fs/cgroup", name).read_text().strip()


def events(name):
    return {key: int(value) for key, value in (line.split() for line in cgroup_value(name).splitlines())}


def command_for(command, fd):
    java_home = Path(shutil.which("java")).resolve().parent.parent
    args = [
        "bwrap", "--unshare-all", "--die-with-parent", "--new-session", "--cap-drop", "ALL",
        "--ro-bind", "/usr", "/usr", "--symlink", "usr/lib", "/lib", "--symlink", "usr/lib64", "/lib64",
        "--proc", "/proc", "--dir", "/dev",
        "--ro-bind", "/dev/null", "/dev/null", "--ro-bind", "/dev/urandom", "/dev/urandom",
        "--ro-bind", "/dev/random", "/dev/random",
        "--size", "134217728", "--tmpfs", "/tmp",
        "--ro-bind", "/fixtures", "/fixtures", "--ro-bind", "/deps", "/deps",
        "--ro-bind", "/classes", "/classes", "--ro-bind", "/usr", "/ocr/usr",
        "--chdir", "/tmp", "--clearenv", "--setenv", "PATH", "/usr/bin",
        "--setenv", "HOME", "/tmp", "--setenv", "LANG", "C.UTF-8",
        "--setenv", "OMP_THREAD_LIMIT", "1",
    ]
    if not java_home.is_relative_to("/usr"):
        args += ["--ro-bind", str(java_home), str(java_home)]
    for path in (Path("/etc/fonts"), Path("/etc/ld.so.cache"), (java_home / "conf/security/java.security").resolve()):
        if path.exists():
            args += ["--ro-bind", str(path), str(path)]
    return args + ["--remount-ro", "/", "--seccomp", str(fd), "--"] + command


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["trace", "normal", "pids", "memory"], default="normal", nargs="?")
    args = parser.parse_args()
    if os.uname().machine != "x86_64":
        parser.error("This experiment is evaluated only on Linux amd64")
    before_memory = events("memory.events")
    before_pids = events("pids.events")
    results = {}

    def expect(name, command, *, reason=None, timeout=15, expected_code=0):
        fd = create_filter()  # Required: no unfiltered fallback, including trace mode.
        try:
            wrapped = command_for(command, fd)
            if args.mode == "trace":
                wrapped = ["strace", "-f", "-e", "trace=clone,clone3,unshare,mount,umount2,pivot_root,seccomp,prctl"] + wrapped
            result = execute_bounded(wrapped, timeout=timeout, pass_fds=(fd,))
        finally:
            os.close(fd)
        result["passed"] = result["reason"] == reason and (reason is not None or result["returncode"] == expected_code)
        results[name] = result

    py = lambda source: ["/usr/bin/python3", "-B", "-c", source]
    private_marker_present = Path("/synthetic-private/marker").is_file()
    manifest = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in Path("/fixtures").iterdir()}
    expect("payload-ready", py("from pathlib import Path; assert Path('/fixtures/S01-digital.pdf').is_file(); print('ready')"))
    # If namespace/filter setup fails, do not disguise subsequent EPERM as a successful security check.
    if results["payload-ready"]["passed"] and args.mode in ("normal", "trace"):
        outer_network = os.readlink("/proc/self/ns/net")
        expect("files-environment-capabilities", py("""
import os
from pathlib import Path
assert not Path('/synthetic-private/marker').exists()
assert not Path('/app/app.jar').exists()
assert not Path('/var/run/docker.sock').exists()
assert not Path('/sys/fs/cgroup').exists()
assert 'DB_PASSWORD' not in os.environ and 'F0_TEST_SECRET' not in os.environ
status=Path('/proc/self/status').read_text()
assert 'CapEff:\t0000000000000000' in status
assert 'NoNewPrivs:\t1' in status
assert 'Seccomp:\t2' in status
assert int(next(s.split(':')[1] for s in status.splitlines() if s.startswith('Seccomp_filters:'))) >= 2
"""))
        expect("network-filter-and-namespace", py(f"""
import errno,os,socket
assert os.readlink('/proc/self/ns/net') != {outer_network!r}
for family in (socket.AF_INET,socket.AF_INET6):
 try:
  socket.socket(family,socket.SOCK_STREAM)
 except OSError as e:
  assert e.errno == errno.EPERM
 else:
  raise AssertionError('network socket allowed')
with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM): pass
try:
 socket.socketpair(socket.AF_INET,socket.SOCK_STREAM)
except OSError as e:
 assert e.errno == errno.EPERM
else:
 raise AssertionError('non-UNIX socketpair allowed')
"""))
        expect("post-setup-namespace-and-mount-denied", py("""
import ctypes,errno,os
from pathlib import Path
lib=ctypes.CDLL(None,use_errno=True)
for flags in (0x10000000,0x7e020000):
 assert lib.unshare(flags) == -1 and ctypes.get_errno() == errno.EPERM
assert lib.mount(b'tmpfs',b'/tmp',b'tmpfs',0,None) == -1 and ctypes.get_errno() == errno.EPERM
assert lib.syscall(56,0x7e020000|17,0,0,0,0) == -1 and ctypes.get_errno() == errno.EPERM
assert lib.syscall(435,0,0) == -1 and ctypes.get_errno() == errno.ENOSYS
try:
 Path('/outside-scratch').write_text('x')
except OSError as e:
 assert e.errno == errno.EROFS
else:
 raise AssertionError('sandbox root writable')
"""))
        expect("source-read-only", py("""
from pathlib import Path
import errno
try:
 Path('/fixtures/S01-digital.pdf').write_bytes(b'x')
except OSError as e:
 assert e.errno == errno.EROFS
else:
 raise AssertionError('source writable')
"""))
        expect("scratch-aggregate-quota", py("""
import errno,os
total=0
try:
 for n in range(9):
  with open('/tmp/chunk-'+str(n),'wb',buffering=0) as f:
   for block in range(16):
    total += f.write(b'x'*1024**2)
except OSError as e:
 assert e.errno == errno.ENOSPC, e
 assert 120*1024**2 <= total <= 128*1024**2
 print('ENOSPC after',total,'bytes')
else:
 raise AssertionError('scratch quota absent')
"""))
        expect("output-limit", py("import os\nwhile True: os.write(1,b'x'*8192)"), reason="OUTPUT_LIMIT")
        expect("timeout-with-child", py("import subprocess,time; subprocess.Popen(['/usr/bin/python3','-c','import time; time.sleep(600)']); time.sleep(600)"), reason="TIMEOUT", timeout=1)
        java = str(Path(shutil.which("java")).resolve())
        expect("readers-and-ocr", [java, *JAVA_FLAGS, "-cp", "/deps/*:/classes", "F0Probe", "read", "/fixtures"], timeout=120)
    elif results["payload-ready"]["passed"] and args.mode == "pids":
        expect("aggregate-pids-limit", py("""
import errno,subprocess
children=[]
limited=False
try:
 for n in range(80):
  try:
   children.append(subprocess.Popen(['/usr/bin/python3','-c','import time; time.sleep(60)']))
  except OSError as e:
   assert e.errno == errno.EAGAIN
   limited=True
   break
 assert limited, 'PID cap absent'
 print('EAGAIN after',len(children),'children')
finally:
 for child in children: child.terminate()
 for child in children: child.wait(timeout=10)
"""), timeout=30)
    elif results["payload-ready"]["passed"] and args.mode == "memory":
        expect("aggregate-memory-limit", py("""
import subprocess
children=[]
try:
 for i in range(2):
  children.append(subprocess.Popen(['/usr/bin/python3','-c',
   'import time; data=bytearray(160*1024**2); print("allocated",flush=True); time.sleep(5)']))
 codes=[child.wait(timeout=15) for child in children]
 assert -9 in codes, codes
 print('children exit codes',codes)
finally:
 for child in children:
  if child.poll() is None: child.kill()
 for child in children: child.wait()
"""), timeout=25)

    memory_after, pids_after = events("memory.events"), events("pids.events")
    extra_ok = memory_after["oom_kill"] == before_memory["oom_kill"] and pids_after["max"] == before_pids["max"]
    if args.mode == "pids":
        extra_ok = pids_after["max"] > before_pids["max"]
    if args.mode == "memory":
        extra_ok = memory_after["oom_kill"] > before_memory["oom_kill"]
    unchanged = manifest == {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in Path("/fixtures").iterdir()}
    packages = subprocess.run(["dpkg-query", "-W", "-f=${Package}=${Version}\\n", "bubblewrap", "libseccomp2",
                               "tesseract-ocr", "tesseract-ocr-spa", "tesseract-ocr-eng", "liblept5"],
                              capture_output=True, text=True, check=True).stdout
    report = {"schema": 1, "mode": args.mode, "uid": os.getuid(), "synthetic_only": True,
              "private_marker_present_outside_payload": private_marker_present,
              "sources_unchanged": unchanged, "packages": packages, "results": results,
              "container_limits": {k: cgroup_value(k) for k in ("memory.max", "memory.swap.max", "pids.max", "cpu.max")},
              "memory_events_before": before_memory, "memory_events_after": memory_after,
              "pids_events_before": before_pids, "pids_events_after": pids_after,
              "cgroup_mount": [s for s in Path('/proc/self/mountinfo').read_text().splitlines() if ' /sys/fs/cgroup ' in s],
              "delegation_unavailable": not os.access("/sys/fs/cgroup", os.W_OK),
              "all_passed": private_marker_present and unchanged and extra_ok and all(r["passed"] for r in results.values()),
              "limitations": ["Docker cgroup applies to this whole experimental container, not an independently delegated worker",
                              "No production integration, startup or deployment; only synthetic fixtures"]}
    print(json.dumps(report, indent=2))
    return 0 if report["all_passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
