#!/usr/bin/env python3
"""BRICA's local reader boundary. No Spring, DB, network, shell or Docker API.

Requires a delegated cgroup-v2 subtree and Bubblewrap with user namespaces.
Missing isolation is an error, never an unsandboxed fallback. Invoked only by
IsolatedSourceProcessor with its private, generated input directory.
"""
import argparse
import ctypes
import errno
import json
import os
from pathlib import Path
import platform
import resource
import selectors
import signal
import subprocess
import sys
import time
import uuid


class Comparison(ctypes.Structure):
    _fields_ = [("arg", ctypes.c_uint), ("op", ctypes.c_uint), ("a", ctypes.c_uint64), ("b", ctypes.c_uint64)]


def payload_filter():
    lib = ctypes.CDLL("libseccomp.so.2")
    lib.seccomp_init.argtypes = [ctypes.c_uint32]
    lib.seccomp_init.restype = ctypes.c_void_p
    lib.seccomp_release.argtypes = [ctypes.c_void_p]
    lib.seccomp_syscall_resolve_name.argtypes = [ctypes.c_char_p]
    lib.seccomp_syscall_resolve_name.restype = ctypes.c_int
    lib.seccomp_rule_add_array.argtypes = [ctypes.c_void_p, ctypes.c_uint32, ctypes.c_int, ctypes.c_uint, ctypes.POINTER(Comparison)]
    lib.seccomp_export_bpf.argtypes = [ctypes.c_void_p, ctypes.c_int]
    context = lib.seccomp_init(0x7FFF0000)
    if not context:
        raise RuntimeError("FILTER_INIT_FAILED")
    fd = None
    try:
        def deny(name, comparisons=(), error=errno.EPERM):
            number = lib.seccomp_syscall_resolve_name(name.encode())
            array = (Comparison * len(comparisons))(*comparisons)
            if number < 0 or lib.seccomp_rule_add_array(context, 0x00050000 | error, number, len(array), array) != 0:
                raise RuntimeError("FILTER_RULE_FAILED")
        for name in ("unshare", "setns", "mount", "umount2", "pivot_root", "mount_setattr", "fsopen", "fsconfig", "fsmount", "fspick",
                     "open_tree", "move_mount", "ptrace", "process_vm_readv", "process_vm_writev", "pidfd_getfd", "bpf"):
            deny(name)
        for bit in (0x00020000, 0x02000000, 0x04000000, 0x08000000, 0x10000000, 0x20000000, 0x40000000, 0x80):
            deny("clone", [Comparison(0, 7, bit, bit)])
        deny("clone3", error=errno.ENOSYS)
        for name in ("socket", "socketpair"):
            deny(name, [Comparison(0, 1, 1, 0)])
        fd = os.memfd_create("brica-reader-filter", os.MFD_CLOEXEC)
        if lib.seccomp_export_bpf(context, fd) != 0:
            raise RuntimeError("FILTER_EXPORT_FAILED")
        os.lseek(fd, 0, os.SEEK_SET)
        return fd
    except BaseException:
        if fd is not None:
            os.close(fd)
        raise
    finally:
        lib.seccomp_release(context)


def configure_group(root):
    root = root.resolve(strict=True)
    if root == Path("/sys/fs/cgroup") or not root.is_relative_to("/sys/fs/cgroup") or not os.access(root, os.W_OK):
        raise RuntimeError("CGROUP_DELEGATION_REQUIRED")
    group = root / ("brica-reader-" + str(uuid.uuid4()))
    group.mkdir(mode=0o700)
    try:
        for name, value in {"memory.max": "1073741824", "memory.swap.max": "0", "pids.max": "64", "cpu.max": "100000 100000"}.items():
            (group / name).write_text(value)
            if (group / name).read_text().strip() != value:
                raise RuntimeError("CGROUP_LIMIT_NOT_APPLIED")
        if not (group / "cgroup.kill").exists():
            raise RuntimeError("CGROUP_KILL_REQUIRED")
        return group
    except BaseException:
        group.rmdir()
        raise


def command_for(stage, manifest, fd):
    java_home = Path(manifest["javaHome"]).resolve(strict=True)
    classpath = manifest["classpath"]
    if not 1 <= len(classpath) <= 200 or not 1 <= len(manifest["inputs"]) <= 4:
        raise RuntimeError("RUNTIME_MANIFEST_LIMIT")
    command = ["bwrap", "--unshare-all", "--die-with-parent", "--new-session", "--cap-drop", "ALL",
               "--ro-bind", "/usr", "/usr", "--symlink", "usr/lib", "/lib", "--symlink", "usr/lib64", "/lib64",
               "--proc", "/proc", "--dir", "/dev", "--ro-bind", "/dev/null", "/dev/null",
               "--ro-bind", "/dev/urandom", "/dev/urandom", "--ro-bind", "/dev/random", "/dev/random",
               "--size", "134217728", "--tmpfs", "/tmp", "--ro-bind", str(stage), "/input", "--chdir", "/tmp",
               "--clearenv", "--setenv", "PATH", "/usr/bin", "--setenv", "HOME", "/tmp",
               "--setenv", "LANG", "C.UTF-8", "--setenv", "OMP_THREAD_LIMIT", "1", "--setenv", "BRICA_SOURCE_SANDBOX", "1"]
    if not java_home.is_relative_to("/usr"):
        command += ["--ro-bind", str(java_home), str(java_home)]
    for path in (Path("/etc/fonts"), Path("/etc/ld.so.cache"), (java_home / "conf/security/java.security").resolve()):
        if path.exists():
            command += ["--ro-bind", str(path), str(path)]
    mapped = []
    for index, name in enumerate(classpath):
        path = Path(name).resolve(strict=True)
        destination = "/runtime/cp-" + str(index) + (".jar" if path.is_file() else "")
        command += ["--ro-bind", str(path), destination]
        mapped.append(destination)
    java = [str(java_home / "bin/java"), "-Xms32m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m",
            "-XX:CompressedClassSpaceSize=64m", "-XX:ReservedCodeCacheSize=64m", "-Djava.awt.headless=true", "-Duser.home=/tmp", "-Djava.io.tmpdir=/tmp"]
    main = "com.brica.extraction.infrastructure.worker.SourceWorker"
    if manifest["boot"]:
        java += ["-Dloader.main=" + main, "-Dloader.home=/tmp", "-cp", ":".join(mapped), "org.springframework.boot.loader.launch.PropertiesLauncher"]
    else:
        java += ["-cp", ":".join(mapped), main]
    return command + ["--remount-ro", "/", "--seccomp", str(fd), "--"] + java


def run(stage, group):
    request = stage / "manifest.json"
    if request.stat().st_size > 65536:
        raise RuntimeError("RUNTIME_MANIFEST_LIMIT")
    manifest = json.loads(request.read_text())
    fd = payload_filter()
    def child_setup():
        (group / "cgroup.procs").write_text(str(os.getpid()))
        resource.setrlimit(resource.RLIMIT_AS, (2 * 1024**3,) * 2)
        resource.setrlimit(resource.RLIMIT_CPU, (60, 60))
        resource.setrlimit(resource.RLIMIT_FSIZE, (16 * 1024**2,) * 2)
        resource.setrlimit(resource.RLIMIT_NOFILE, (128, 128))
        resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    try:
        process = subprocess.Popen(command_for(stage, manifest, fd), stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   env={"PATH": "/usr/bin"}, pass_fds=(fd,), start_new_session=True, preexec_fn=child_setup)
    finally:
        os.close(fd)
    output = bytearray()
    errors = 0
    try:
        process.stdin.write(json.dumps(manifest["inputs"]).encode())
        process.stdin.close()
        with selectors.DefaultSelector() as selector:
            selector.register(process.stdout, selectors.EVENT_READ, "stdout")
            selector.register(process.stderr, selectors.EVENT_READ, "stderr")
            deadline = time.monotonic() + 120
            while selector.get_map():
                if time.monotonic() >= deadline:
                    raise RuntimeError("READER_TIMEOUT")
                for key, _ in selector.select(0.2):
                    data = os.read(key.fileobj.fileno(), 8192)
                    if not data:
                        selector.unregister(key.fileobj)
                    elif key.data == "stdout":
                        output.extend(data)
                    else:
                        errors += len(data)
                    if len(output) + errors > 1048576 or errors > 65536:
                        raise RuntimeError("READER_OUTPUT_LIMIT")
            process.wait(timeout=max(0.1, deadline - time.monotonic()))
        if process.returncode != 0:
            raise RuntimeError("READER_PROCESS_FAILED")
        json.loads(output)  # Protocol must be inert, complete JSON; raw logging never becomes data.
        sys.stdout.buffer.write(output)
    finally:
        if process.poll() is None:
            process.kill()
        (group / "cgroup.kill").write_text("1")
        process.wait(timeout=5)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--cgroup-root", type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != "Linux" or platform.machine() != "x86_64":
        raise RuntimeError("SUPPORTED_ISOLATION_REQUIRED")
    libc = ctypes.CDLL(None)
    if libc.prctl(1, signal.SIGKILL) != 0 or os.getppid() == 1:
        raise RuntimeError("PARENT_LIFECYCLE_UNAVAILABLE")
    group = configure_group(args.cgroup_root)
    try:
        run(args.input.resolve(strict=True), group)
    finally:
        (group / "cgroup.kill").write_text("1")
        for _ in range(50):
            try:
                group.rmdir()
                break
            except OSError as failure:
                if failure.errno != errno.EBUSY:
                    raise
                time.sleep(0.02)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        print("BRICA_READER_ISOLATION_OR_PROCESS_FAILED", file=sys.stderr)
        raise SystemExit(1)
