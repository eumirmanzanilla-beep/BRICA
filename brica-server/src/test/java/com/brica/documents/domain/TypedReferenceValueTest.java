package com.brica.documents.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypedReferenceValueTest {
    @Test void resolvesDatesInSpanishAndReportsInvalidInput() {
        var field = new ReferenceField("FECHA", "Fecha", ReferenceAttribute.WORK_START_DATE);
        assertEquals("16 de julio del 2005", ReferenceValue.resolve(field, LocalDate.of(2005, 7, 16)).textValue());
        assertEquals("16 de julio del 2005", ReferenceValue.resolve(field, "16/07/2005").textValue());
        assertEquals(ResolutionStatus.INVALID, ReferenceValue.resolve(field, "31/02/2026").status());
        assertEquals(ResolutionStatus.MISSING, ReferenceValue.resolve(field, " ").status());
    }
    @Test void keepsPreciseAmountAndSeparatePresentation() {
        var field = new ReferenceField("MONTO", "Monto", ReferenceAttribute.WORK_CONTRACTED_AMOUNT);
        var value = ReferenceValue.resolve(field, new BigDecimal("1.186"));
        assertEquals("1.186", value.textValue());
        assertEquals("1.19", value.presentationValue());
        assertEquals(ResolutionStatus.PRESENT, ReferenceValue.resolve(field, BigDecimal.ZERO).status());
        assertEquals(ResolutionStatus.INVALID, ReferenceValue.resolve(field, "NaN").status());
    }
}
