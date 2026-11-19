"""Pure unit checks of the experimental profile derivation, no Docker/network."""
import json
from unittest import TestCase
from unittest.mock import Mock, patch
from container.create_profile import ALL_NAMESPACES, UPSTREAM_SHA256, create_profile


class ContainerProfileTest(TestCase):
    def test_unknown_upstream_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unrecognized"):
            create_profile(b"{}")

    def test_allow_by_default_is_rejected_even_with_matching_pin(self):
        with patch("container.create_profile.hashlib.sha256", return_value=Mock(hexdigest=lambda: UPSTREAM_SHA256)):
            with self.assertRaisesRegex(ValueError, "deny-by-default"):
                create_profile(json.dumps({"defaultAction": "SCMP_ACT_ALLOW"}).encode())

    def test_only_observed_namespace_flag_sets_are_added(self):
        original = {"defaultAction": "SCMP_ACT_ERRNO", "defaultErrnoRet": 1,
                    "syscalls": [{"names": ["bpf"], "action": "SCMP_ACT_ERRNO"}]}
        with patch("container.create_profile.hashlib.sha256", return_value=Mock(hexdigest=lambda: UPSTREAM_SHA256)):
            derived = create_profile(json.dumps(original).encode())
        self.assertEqual("SCMP_ACT_ERRNO", derived["defaultAction"])
        self.assertEqual(original["syscalls"][0], derived["syscalls"][0])
        self.assertEqual(1, len(original["syscalls"]))
        additions = derived["syscalls"][1:]
        self.assertEqual(3, len(additions))
        unshare = next(r for r in additions if r["names"] == ["unshare"])
        clone = next(r for r in additions if r["names"] == ["clone"])
        mounts = next(r for r in additions if "mount" in r["names"])
        self.assertEqual([{"index": 0, "value": 0x10000000, "op": "SCMP_CMP_EQ"}], unshare["args"])
        self.assertEqual([{"index": 0, "value": ALL_NAMESPACES | 17, "op": "SCMP_CMP_EQ"}], clone["args"])
        self.assertEqual(["mount", "umount2", "pivot_root"], mounts["names"])
