package com.brica.documents.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Formato de presentación; no modifica el valor preciso de origen. */
public final class AmountPresentation {
    private AmountPresentation() { }

    public static String format(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        BigDecimal magnitude = amount.abs();
        int scale = magnitude.scale();
        if (scale > 0) {
            int lastDigit = magnitude.unscaledValue().abs().mod(java.math.BigInteger.TEN).intValue();
            if (lastDigit >= 5) {
                magnitude = magnitude.setScale(scale - 1, RoundingMode.UP);
            }
        }
        BigDecimal presented = amount.signum() < 0 ? magnitude.negate() : magnitude;
        return presented.stripTrailingZeros().toPlainString();
    }
}
