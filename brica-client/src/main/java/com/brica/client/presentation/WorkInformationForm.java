package com.brica.client.presentation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.brica.client.api.DocumentDtos.Field;
import com.brica.client.api.WorkDto;
import com.brica.client.api.WorksApiClient.DocumentInformation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

final class WorkInformationForm {
    final GridPane node = new GridPane();
    final TextField name, location;
    final TextArea description;
    private final TextField contract, contractor, total, beforeTax, tax, start, end, actual;
    private final ComboBox<String> status = new ComboBox<>();
    private final Map<Field, TextInputControl> additional = new LinkedHashMap<>();
    private final Map<String, String> retained;
    WorkInformationForm(WorkDto work, DocumentInformation info, List<Field> fields, boolean editable) {
        node.setHgap(12); node.setVgap(8);
        name = new TextField(work.name()); location = new TextField(work.location()); description = new TextArea(work.description());
        description.setPrefRowCount(3); description.setWrapText(true);
        contract = new TextField(info.contractNumber()); contractor = new TextField(info.contractor());
        total = amount(info.contractedAmount()); beforeTax = amount(info.amountBeforeTax()); tax = amount(info.taxAmount());
        start = date(info.startDate()); end = date(info.endDate()); actual = date(info.actualEndDate());
        status.getItems().setAll("En Proceso", "En pausa", "Terminada", "Cancelada");
        status.getSelectionModel().select(switch (info.status()) { case "ON_HOLD" -> 1; case "COMPLETED" -> 2; case "CANCELLED" -> 3; default -> 0; });
        node.addRow(0, new Label("Nombre de la obra"), name); node.addRow(1, new Label("Descripción"), description);
        node.addRow(2, new Label("Ubicación"), location); node.addRow(3, new Label("Número de contrato"), contract);
        node.addRow(4, new Label("Contratista"), contractor); node.addRow(5, new Label("Monto contratado"), total);
        node.addRow(6, new Label("Monto antes de IVA"), beforeTax); node.addRow(7, new Label("IVA"), tax);
        node.addRow(8, new Label("Inicio (opcional)"), start); node.addRow(9, new Label("Terminación contractual"), end);
        node.addRow(10, new Label("Terminación real"), actual); node.addRow(11, new Label("Estado"), status);
        retained = new LinkedHashMap<>(info.additionalValues()); int row = 12;
        for (Field field : fields) {
            if (!"ADDITIONAL".equals(field.origin())) { continue; }
            String value = retained.getOrDefault(field.key(), ""); TextInputControl input;
            if ("TEXT".equals(field.type())) { var area = new TextArea(value); area.setPrefRowCount(2); input = area; }
            else { input = new TextField(value); }
            input.setPromptText("DATE".equals(field.type()) ? "dd/MM/aaaa" : "AMOUNT".equals(field.type()) ? "Importe preciso" : "Texto");
            additional.put(field, input); node.addRow(row++, new Label(field.key()), input);
        }
        var known = additional.keySet().stream().map(Field::key).collect(java.util.stream.Collectors.toSet());
        for (var entry : retained.entrySet()) {
            if (!known.contains(entry.getKey())) { var text = new TextArea(entry.getValue()); text.setEditable(false); text.setPrefRowCount(2); node.addRow(row++, new Label(entry.getKey()), text); }
        }
        if (!editable) { node.getChildren().stream().filter(n -> n instanceof Control && !(n instanceof Label)).forEach(n -> n.setDisable(true)); }
    }
    DocumentInformation information() {
        if (name.getText().isBlank() || name.getText().length() > 255) { throw new IllegalArgumentException("Indica un nombre de hasta 255 caracteres."); }
        var values = new LinkedHashMap<>(retained);
        additional.forEach((field, input) -> {
            String text = input.getText();
            if (!text.isBlank()) {
                if ("DATE".equals(field.type())) { DocumentInput.date(text); }
                if ("AMOUNT".equals(field.type())) { new BigDecimal(text); }
            }
            values.put(field.key(), text);
        });
        var first = DocumentInput.date(start.getText()); var last = DocumentInput.date(end.getText());
        if (first != null && last != null && last.isBefore(first)) { throw new IllegalArgumentException("La terminación no puede ser anterior al inicio."); }
        return new DocumentInformation(contract.getText(), contractor.getText(), decimal(total), decimal(beforeTax), decimal(tax),
                iso(first), iso(last), iso(DocumentInput.date(actual.getText())),
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
    private static TextField amount(BigDecimal value) { return new TextField(value == null ? "" : value.toPlainString()); }
    private static BigDecimal decimal(TextField field) { return field.getText().isBlank() ? null : new BigDecimal(field.getText()); }
    private static TextField date(String value) { var field = new TextField(DocumentInput.format(DocumentInput.date(value))); field.setPromptText("dd/MM/aaaa"); return field; }
    private static String iso(java.time.LocalDate value) { return value == null ? null : value.toString(); }
}
