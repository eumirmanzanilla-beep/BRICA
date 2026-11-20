"""Executable F0 numerical examples; production will use Java BigDecimal.

This tests the chosen acceptance examples, not an application implementation.
"""
from decimal import Decimal, ROUND_HALF_UP
import unittest


class ControlPolicyExamplesTest(unittest.TestCase):
    def test_per_line_rounding_differs_from_final_rounding(self):
        amounts = [Decimal("1") * Decimal("0.335") for _ in range(3)]
        per_line = sum(v.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP) for v in amounts)
        at_end = sum(amounts).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
        self.assertEqual(Decimal("1.02"), per_line)
        self.assertEqual(Decimal("1.01"), at_end)
        self.assertEqual(Decimal("1.005"), sum(amounts))
        declared = Decimal("1.01")
        self.assertEqual(Decimal("-0.01"), declared - per_line)
        self.assertEqual(Decimal("1.01"), declared)

    def test_half_up_includes_signed_interpretable_corrections(self):
        self.assertEqual(Decimal("2.68"), Decimal("2.675").quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))
        self.assertEqual(Decimal("-2.68"), Decimal("-2.675").quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))

    def test_source_scale_is_not_control_scale(self):
        original = Decimal("5.2500")
        quantity = Decimal("2")
        self.assertEqual(Decimal("10.50"), (quantity * original).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))
        self.assertEqual("5.2500", str(original))


if __name__ == "__main__":
    unittest.main()
