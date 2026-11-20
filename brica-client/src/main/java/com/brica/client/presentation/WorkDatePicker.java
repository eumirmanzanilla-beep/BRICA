package com.brica.client.presentation;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

final class WorkDatePicker {
    private WorkDatePicker() { }
    static DatePicker create(String value) {
        var picker = new DatePicker();
        picker.setConverter(new StringConverter<LocalDate>() {
            public String toString(LocalDate date) { return DocumentInput.format(date); }
            public LocalDate fromString(String text) { return DocumentInput.date(text); }
        });
        picker.setPromptText("dd/MM/aaaa");
        picker.valueProperty().addListener((observable, before, date) -> picker.getEditor().setText(DocumentInput.format(date)));
        picker.setValue(DocumentInput.date(value));
        return picker;
    }
    static LocalDate read(DatePicker picker) {
        // Read uncommitted editor text as well, so typing an invalid date never saves an old value.
        return DocumentInput.date(picker.getEditor().getText());
    }
}
