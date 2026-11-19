"""Second seccomp filter, applied by Bubblewrap AFTER namespace/mount setup.

Supplemental deny rules layered over the pinned Docker allowlist. libseccomp
exports classic BPF to a memfd; no compiler, setuid helper or privilege needed.
For this experiment only; not a portable server API.
"""
import ctypes
import errno
import os


class ArgComparison(ctypes.Structure):
    _fields_ = [("arg", ctypes.c_uint), ("op", ctypes.c_uint),
                ("datum_a", ctypes.c_uint64), ("datum_b", ctypes.c_uint64)]


def create_filter():
    seccomp = ctypes.CDLL("libseccomp.so.2", use_errno=True)
    seccomp.seccomp_init.argtypes = [ctypes.c_uint32]
    seccomp.seccomp_init.restype = ctypes.c_void_p
    seccomp.seccomp_release.argtypes = [ctypes.c_void_p]
    seccomp.seccomp_syscall_resolve_name.argtypes = [ctypes.c_char_p]
    seccomp.seccomp_syscall_resolve_name.restype = ctypes.c_int
    seccomp.seccomp_rule_add_array.argtypes = [ctypes.c_void_p, ctypes.c_uint32, ctypes.c_int,
                                              ctypes.c_uint, ctypes.POINTER(ArgComparison)]
    seccomp.seccomp_export_bpf.argtypes = [ctypes.c_void_p, ctypes.c_int]
    context = seccomp.seccomp_init(0x7FFF0000)  # SCMP_ACT_ALLOW, only this supplemental layer.
    if not context:
        raise RuntimeError("Cannot allocate payload seccomp context")
    fd = None
    try:
        def deny(name, comparisons=(), error=errno.EPERM):
            number = seccomp.seccomp_syscall_resolve_name(name.encode())
            if number < 0:
                raise RuntimeError(f"Unknown syscall in mandatory filter: {name}")
            args = (ArgComparison * len(comparisons))(*comparisons)
            result = seccomp.seccomp_rule_add_array(context, 0x00050000 | error, number, len(args), args)
            if result != 0:
                raise RuntimeError(f"Cannot constrain syscall {name}: {result}")

        for name in ("unshare", "setns", "mount", "umount2", "pivot_root", "mount_setattr",
                     "fsopen", "fsconfig", "fsmount", "fspick", "open_tree", "move_mount",
                     "ptrace", "process_vm_readv", "process_vm_writev", "pidfd_getfd", "bpf"):
            deny(name)
        # Reject namespace creation while retaining ordinary fork/threads for JVM and OCR.
        for bit in (0x00020000, 0x02000000, 0x04000000, 0x08000000,
                    0x10000000, 0x20000000, 0x40000000, 0x00000080):
            deny("clone", [ArgComparison(0, 7, bit, bit)])  # SCMP_CMP_MASKED_EQ
        # clone3 uses a struct pointer, not safely filterable by flag value; libc falls back.
        deny("clone3", error=errno.ENOSYS)
        deny("socket", [ArgComparison(0, 1, 1, 0)])  # SCMP_CMP_NE: only AF_UNIX sockets.
        deny("socketpair", [ArgComparison(0, 1, 1, 0)])
        fd = os.memfd_create("f0-payload-seccomp", os.MFD_CLOEXEC)
        if seccomp.seccomp_export_bpf(context, fd) != 0:
            raise RuntimeError("Cannot export payload BPF")
        os.lseek(fd, 0, os.SEEK_SET)
        return fd
    except BaseException:
        if fd is not None:
            os.close(fd)
        raise
    finally:
        seccomp.seccomp_release(context)
