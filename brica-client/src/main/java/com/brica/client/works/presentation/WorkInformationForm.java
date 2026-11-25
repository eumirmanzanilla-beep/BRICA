package com.brica.client.works.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.brica.client.documents.api.DocumentDtos.Field;
import com.brica.client.documents.presentation.DocumentInput;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient.DocumentInformation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

final class WorkInformationForm {
    private static final Set<String> HIDDEN_ADDITIONAL_KEYS = Set.of(
            "OBSERVACIONES", "FECHA_ACTUAL", "CLAUSULAS", "MONTO_ANTICIPO",
            "SUPERVISOR", "LUGAR_FIRMA", "FICHA_FIRMA");
    final GridPane node = new GridPane();
    final TextField name, location;
    final TextArea description;
    private final TextField contract, contractor, total, beforeTax, tax;
    private final DatePicker start, end, actual;
    private final ComboBox<String> status = new ComboBox<>();
    private final Map<Field, TextInputControl> additional = new LinkedHashMap<>();
    private final Map<String, String> retained;
    private final Map<Control, Label> errors = new LinkedHashMap<>();
    WorkInformationForm(WorkDto work, DocumentInformation info, List<Field> fields, boolean editable) {
        node.setHgap(12); node.setVgap(8);
        name = new TextField(text(work.name())); location = new TextField(text(work.location())); description = new TextArea(text(work.description()));
        name.setPromptText("Letras, números y _; inicia con letra");
        description.setPrefRowCount(3); description.setWrapText(true);
        contract = new TextField(text(info.contractNumber())); contractor = new TextField(text(info.contractor()));
        total = amount(info.contractedAmount()); beforeTax = amount(info.amountBeforeTax()); tax = amount(info.taxAmount());
        start = WorkDatePicker.create(info.startDate()); end = WorkDatePicker.create(info.endDate()); actual = WorkDatePicker.create(info.actualEndDate());
        status.getItems().setAll("En Proceso", "En pausa", "Terminada", "Cancelada");
        status.getSelectionModel().select(switch (info.status()) { case "ON_HOLD" -> 1; case "COMPLETED" -> 2; case "CANCELLED" -> 3; default -> 0; });
        addRow(0, "Nombre de la obra", name); addRow(1, "Descripción", description);
        addRow(2, "Ubicación", location); addRow(3, "Número de contrato", contract);
        addRow(4, "Contratista", contractor); addRow(5, "Monto contratado", total);
        addRow(6, "Monto antes de IVA", beforeTax); addRow(7, "IVA", tax);
        addRow(8, "Inicio (opcional)", start); addRow(9, "Terminación contractual", end);
        addRow(10, "Terminación real", actual); addRow(11, "Estado", status);
        retained = new LinkedHashMap<>(info.additionalValues()); int row = 12;
        for (Field field : fields) {
            if (!"ADDITIONAL".equals(field.origin()) || hidden(field.key())) { continue; }
            String value = text(retained.get(field.key())); TextInputControl input;
            if ("TEXT".equals(field.type())) { var area = new TextArea(value); area.setPrefRowCount(2); input = area; }
            else { input = new TextField(value); }
            input.setPromptText("DATE".equals(field.type()) ? "dd/MM/aaaa" : "AMOUNT".equals(field.type()) ? "Importe preciso" : "Texto");
            additional.put(field, input); addRow(row++, field.key(), input);
        }
        var known = additional.keySet().stream().map(Field::key).collect(java.util.stream.Collectors.toSet());
        for (var entry : retained.entrySet()) {
            if (!known.contains(entry.getKey()) && !hidden(entry.getKey())) {
                var text = new TextArea(entry.getValue()); text.setEditable(false); text.setPrefRowCount(2); node.addRow(row++, new Label(entry.getKey()), text);
            }
        }
        installLiveValidation();
        validateFields();
        if (!editable) { node.getChildren().stream().filter(n -> n instanceof Control && !(n instanceof Label)).forEach(n -> n.setDisable(true)); }
    }
    DocumentInformation information() {
        if (!validateFields()) {
            throw new IllegalArgumentException("Corrige los campos señalados.");
        }
        var values = new LinkedHashMap<>(retained);
        additional.forEach((field, input) -> {
            String text = input.getText();
            if (!text.isBlank()) {
                if ("DATE".equals(field.type())) { DocumentInput.date(text); }
                if ("AMOUNT".equals(field.type())) { new BigDecimal(text); }
            }
            values.put(field.key(), text);
        });
        var first = WorkDatePicker.read(start); var last = WorkDatePicker.read(end); var real = WorkDatePicker.read(actual);
        return new DocumentInformation(contract.getText(), contractor.getText(), decimal(total), decimal(beforeTax), decimal(tax),
                iso(first), iso(last), iso(real),
                List.of("IN_PROGRESS", "ON_HOLD", "COMPLETED", "CANCELLED").get(status.getSelectionModel().getSelectedIndex()), values);
    }
    String preview() {
        var info = information();
        StringBuilder text = new StringBuilder();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("NOMBRE_OBRA", name.getText()); values.put("DESCRIPCION_TRABAJO", description.getText()); values.put("UBICACION", location.getText());
        values.put("NUMERO_CONTRATO", info.contractNumber()); values.put("CONTRATISTA", info.contractor());
        values.put("MONTO_CONTRATADO", info.contractedAmount()); values.put("MONTO_ANTES_IVA", info.amountBeforeTax()); values.put("IVA", info.taxAmount());
        values.put("FECHA_INICIO", spanish(info.startDate())); values.put("FECHA_TERMINACION", spanish(info.endDate())); values.put("FECHA_TERMINACION_REAL", spanish(info.actualEndDate()));
        values.put("ESTADO_OBRA", status.getValue()); values.putAll(info.additionalValues());
        additional.forEach((field, input) -> { if ("DATE".equals(field.type()) && !input.getText().isBlank()) { values.put(field.key(), spanish(input.getText())); } });
        values.forEach((key, value) -> text.append(key).append(": ").append(value == null || value.toString().isBlank() ? "Sin capturar" : value).append('\n'));
        return text.toString();
    }
    private static String spanish(String date) {
        var value = DocumentInput.date(date);
        return value == null ? null : value.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM 'del' uuuu", java.util.Locale.forLanguageTag("es-MX")));
    }
    private void addRow(int row, String label, Control input) {
        var error = new Label();
        error.getStyleClass().add("content-note");
        error.setWrapText(true);
        errors.put(input, error);
        node.addRow(row, new Label(label), input, error);
    }

    private void installLiveValidation() {
        errors.keySet().forEach(input -> {
            if (input instanceof TextInputControl text) {
                text.textProperty().addListener((observable, previous, value) -> validateFields());
            } else if (input instanceof DatePicker date) {
                date.valueProperty().addListener((observable, previous, value) -> validateFields());
                date.getEditor().textProperty().addListener((observable, previous, value) -> validateFields());
            } else if (input instanceof ComboBox<?> choice) {
                choice.valueProperty().addListener((observable, previous, value) -> validateFields());
            }
        });
    }

    private boolean validateFields() {
        errors.values().forEach(label -> label.setText(""));
        error(name, WorkNameInputRules.error(name.getText()));
        error(location, location.getText().length() > 255 ? "Máximo 255 caracteres." : "");
        error(contract, contract.getText().length() > 50 ? "Máximo 50 caracteres." : "");
        error(contractor, contractor.getText().length() > 200 ? "Máximo 200 caracteres." : "");
        validateAmount(total); validateAmount(beforeTax); validateAmount(tax);
        LocalDate first = validateDate(start);
        LocalDate last = validateDate(end);
        LocalDate real = validateDate(actual);
        if (first != null && last != null && last.isBefore(first)) {
            error(end, "La terminación no puede ser anterior al inicio.");
        }
        if (first != null && real != null && real.isBefore(first)) {
            error(actual, "La terminación real no puede ser anterior al inicio.");
        }
        error(status, status.getValue() == null ? "Selecciona el estado de la obra." : "");
        additional.forEach((field, input) -> {
            if (input.getText().isBlank()) { return; }
            try {
                if ("DATE".equals(field.type())) { DocumentInput.date(input.getText()); }
                if ("AMOUNT".equals(field.type())) { new BigDecimal(input.getText()); }
            } catch (RuntimeException failure) {
                error(input, "DATE".equals(field.type()) ? "Usa una fecha válida." : "Indica un importe válido.");
            }
        });
        return errors.values().stream().allMatch(label -> label.getText().isEmpty());
    }

    private void validateAmount(TextField input) {
        if (input.getText().isBlank()) { return; }
        try {
            new BigDecimal(input.getText());
        } catch (NumberFormatException failure) {
            error(input, "Indica un importe válido.");
        }
    }

    private LocalDate validateDate(DatePicker input) {
        try {
            return WorkDatePicker.read(input);
        } catch (RuntimeException failure) {
            error(input, "Usa una fecha válida.");
            return null;
        }
    }

    private void error(Control input, String text) {
        errors.get(input).setText(text);
    }

    private static boolean hidden(String key) {
        return key != null && HIDDEN_ADDITIONAL_KEYS.contains(key.toUpperCase(Locale.ROOT));
    }
    private static String text(String value) { return value == null ? "" : value; }
    private static TextField amount(BigDecimal value) { return new TextField(value == null ? "" : value.toPlainString()); }
    private static BigDecimal decimal(TextField field) { return field.getText().isBlank() ? null : new BigDecimal(field.getText()); }
    private static String iso(java.time.LocalDate value) { return value == null ? null : value.toString(); }
}
