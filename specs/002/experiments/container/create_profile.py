#!/usr/bin/env python3
"""Derive an experimental setup profile from a HASH-PINNED Moby allowlist.

No ALLOW-by-default profile, unconfined mode or added host capabilities. This
permits namespace setup for Bubblewrap, NOT a production-wide profile. Payload
must receive a second seccomp filter after setup. Linux amd64 evaluation only.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import platform


UPSTREAM_URL = "https://raw.githubusercontent.com/moby/profiles/9ee3204f0d0cfa19ca2c6a7b0fcf5a2b353954ae/seccomp/default.json"
UPSTREAM_SHA256 = "3320527d4568265ac97a077609b1bfd4cfd0d789eeb9239b7e094b1526675406"
DERIVED_SHA256 = "5ef564bebbaee166b6b5ed0c8fe72746019ee95b37dfbefe7241306fa0a01b2d"
ALL_NAMESPACES = 0x7E020000


def create_profile(base_bytes):
    if hashlib.sha256(base_bytes).hexdigest() != UPSTREAM_SHA256:
        raise ValueError("Unrecognized upstream profile; review before changing the pin")
    profile = copy.deepcopy(json.loads(base_bytes))
    if profile["defaultAction"] != "SCMP_ACT_ERRNO":
        raise ValueError("Expected Moby deny-by-default profile")
    # Exact flag sets only. Further sets may be added only from recorded traces.
    for flags in (0x10000000,):
        profile["syscalls"].append({
            "names": ["unshare"], "action": "SCMP_ACT_ALLOW",
            "args": [{"index": 0, "value": flags, "op": "SCMP_CMP_EQ"}],
        })
    profile["syscalls"].append({
        "names": ["clone"], "action": "SCMP_ACT_ALLOW",
        "args": [{"index": 0, "value": ALL_NAMESPACES | 17, "op": "SCMP_CMP_EQ"}],
        "comment": "Observed Bubblewrap 0.9.0 all namespaces plus SIGCHLD on amd64",
    })
    profile["syscalls"].append({
        "names": ["mount", "umount2", "pivot_root"], "action": "SCMP_ACT_ALLOW",
        "comment": "F0 namespace setup only; kernel userns boundary and mandatory payload filter",
    })
    return profile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != "Linux" or platform.machine() != "x86_64":
        parser.error("Profile evaluated only on Linux amd64; no cross-architecture fallback")
    if args.output.exists():
        parser.error("Output already exists; retain previous evidence")
    profile = create_profile(args.base.read_bytes())
    serialized = json.dumps(profile, indent=2) + "\n"
    if hashlib.sha256(serialized.encode()).hexdigest() != DERIVED_SHA256:
        raise ValueError("Derived profile differs from reviewed experiment; review before changing the pin")
    args.output.write_text(serialized)
    print(f"Profile: {args.output}; base: {UPSTREAM_URL}")


if __name__ == "__main__":
    main()
