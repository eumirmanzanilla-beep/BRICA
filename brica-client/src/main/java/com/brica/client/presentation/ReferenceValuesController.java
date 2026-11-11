package com.brica.client.presentation;

import java.util.*;
import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentsApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ReferenceValuesController {
    @FXML private VBox pane;
    @FXML private TextField workId;
    @FXML private ComboBox<PackageOption> packageChoice;
    @FXML private VBox choices;
    @FXML private TextArea values;
    @FXML private Label message;
    private DocumentsApiClient api;
    private Long loadedWork;
    private final List<ComboBox<TemplateOption>> selections = new ArrayList<>();
    void configure(DocumentsApiClient api) { this.api = api; }
    @FXML private void initialize() {
        packageChoice.valueProperty().addListener((o, old, value) -> {
            choices.getChildren().clear(); selections.clear(); values.clear();
            if (value == null) { return; }
            var byType = new TreeMap<String, List<TemplateOption>>();
            value.templates().forEach(t -> byType.computeIfAbsent(t.documentType(), k -> new ArrayList<>()).add(t));
            byType.forEach((type, templates) -> {
                var combo = new ComboBox<TemplateOption>(); combo.getItems().setAll(templates); combo.setPromptText("No seleccionar");
                var clear = new Button("Quitar selección"); clear.setOnAction(e -> combo.setValue(null));
                choices.getChildren().add(new VBox(4, new Label(type), combo, clear)); selections.add(combo);
            });
        });
        workId.textProperty().addListener((o, old, value) -> { loadedWork = null; packageChoice.getItems().clear(); values.clear(); });
    }
    @FXML private void load() {
        DocumentUi.run(pane, message, () -> {
            var id = DocumentUi.optionalId(workId.getText());
            if (id == null) { throw new IllegalArgumentException("Indica la obra a consultar."); }
            loadedWork = id; return api.options(id);
        }, options -> {
            packageChoice.setValue(null);
            packageChoice.getItems().setAll(options);
            if (options.isEmpty()) { message.setText("No hay paquetes confirmados con plantillas elegibles para esta obra."); }
        });
    }
    @FXML private void resolve() {
        if (loadedWork == null || packageChoice.getValue() == null) { message.setText("Consulta la obra y elige un paquete."); return; }
        var selected = selections.stream().map(ComboBox::getValue).filter(Objects::nonNull).toList();
        DocumentUi.run(pane, message, () -> api.resolve(loadedWork, packageChoice.getValue(), selected), documents -> {
            var text = new StringBuilder();
            documents.forEach(d -> {
                text.append("Plantilla #").append(d.templateId()).append(" · versión ").append(d.version()).append('\n');
                if (d.staticDocument()) { text.append("Documento estático, sin sustituciones.\n"); }
                d.fields().forEach(f -> text.append(f.marker()).append(" · ").append(f.origin()).append(f.required() ? " · obligatorio" : " · opcional")
                        .append(" · ").append(f.status()).append("\n").append(f.textValue() == null ? "Sin valor" : f.textValue()).append("\n\n"));
            });
            values.setText(text.toString());
        });
    }
}
