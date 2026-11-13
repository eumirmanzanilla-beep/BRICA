package com.brica.documents.domain;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountPresentationTest {
    @ParameterizedTest
    @CsvSource({
            "1.184, 1.184", "1.186, 1.19", "1.19, 1.2", "1.198, 1.2",
            "-1.184, -1.184", "-1.186, -1.19", "-1.19, -1.2",
            "0.005, 0.01", "-0.005, -0.01", "0.004, 0.004", "12.000, 12"
    })
    void appliesTheThresholdToTheLastDecimalWithoutChangingTheSource(String input, String expected) {
        var original = new BigDecimal(input);
        assertEquals(expected, AmountPresentation.format(original));
        assertEquals(input, original.toPlainString());
    }
}
