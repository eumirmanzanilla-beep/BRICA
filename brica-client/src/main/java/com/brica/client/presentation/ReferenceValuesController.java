package com.brica.client.presentation;

import java.util.*;
import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentsApiClient;
import com.brica.client.api.WorkDto;
import com.brica.client.api.WorksApiClient;
import com.brica.client.config.ClientConfig;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ReferenceValuesController {
    @FXML private VBox pane;
    @FXML private VBox form;
    @FXML private Button openButton;
    @FXML private Label accessMessage;
    @FXML private ComboBox<WorkDto> workChoice;
    @FXML private ComboBox<PackageOption> packageChoice;
    @FXML private VBox choices;
    @FXML private TextArea values;
    @FXML private Label message;
    private DocumentsApiClient api;
    private final WorksApiClient works = new WorksApiClient(ClientConfig.serverBaseUri());
    private int workPage;
    private Long loadedWork;
    private final List<ComboBox<TemplateOption>> selections = new ArrayList<>();
    private Dialog<ButtonType> dialog;
    void configure(DocumentsApiClient api, boolean worksRead) {
        this.api = api;
        openButton.setDisable(!worksRead);
        accessMessage.setText(worksRead ? "" : "Solicita WORKS_READ para elegir una obra. DOCUMENT_VALUES_READ no concede ese acceso.");
    }
    @FXML private void initialize() {
        pane.getChildren().remove(form);
        form.disableProperty().addListener((observable, old, busy) -> pane.setDisable(busy));
        packageChoice.valueProperty().addListener((o, old, value) -> {
            choices.getChildren().clear(); selections.clear(); values.clear();
            if (value == null) { return; }
            var byType = new TreeMap<String, List<TemplateOption>>();
            value.templates().forEach(t -> byType.computeIfAbsent(t.documentType(), k -> new ArrayList<>()).add(t));
            byType.forEach((type, templates) -> {
                var combo = new ComboBox<TemplateOption>(); combo.getItems().setAll(templates); combo.setPromptText("No seleccionar");
                combo.valueProperty().addListener((obs, previous, current) -> values.clear());
                var clear = new Button("Quitar selección"); clear.setOnAction(e -> combo.setValue(null));
                choices.getChildren().add(new VBox(4, new Label(type), combo, clear)); selections.add(combo);
            });
        });
        workChoice.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(WorkDto work) { return work == null ? "" : work.name() + " · #" + work.id(); }
            @Override public WorkDto fromString(String text) { return null; }
        });
        workChoice.valueProperty().addListener((o, old, value) -> { loadedWork = null; packageChoice.getItems().clear(); values.clear(); });
    }
    @FXML private void openDialog() {
        if (dialog == null) {
            dialog = new Dialog<>();
            dialog.initOwner(pane.getScene().getWindow());
            dialog.setTitle("Consultar valores por obra");
            dialog.getDialogPane().setContent(form);
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(760);
            dialog.setResizable(true);
            dialog.setOnCloseRequest(event -> { if (form.isDisable()) { event.consume(); } });
        }
        dialog.show();
    }
    @FXML private void loadWorks() {
        DocumentUi.run(form, message, () -> works.list(workPage, 50), items -> {
            workChoice.getItems().setAll(items);
            if (items.isEmpty()) { message.setText("No hay obras en esta página. Se requiere WORKS_READ para consultarlas."); }
        });
    }
    @FXML private void previousWorks() { if (workPage > 0) { workPage--; loadWorks(); } }
    @FXML private void nextWorks() { if (workChoice.getItems().size() == 50) { workPage++; loadWorks(); } }
    @FXML private void load() {
        DocumentUi.run(form, message, () -> {
            var work = workChoice.getValue();
            if (work == null) { throw new IllegalArgumentException("Selecciona una obra; necesitas el permiso WORKS_READ."); }
            loadedWork = null; packageChoice.setValue(null); packageChoice.getItems().clear();
            return api.options(work.id());
        }, options -> {
            loadedWork = workChoice.getValue().id();
            packageChoice.setValue(null);
            packageChoice.getItems().setAll(options);
            if (options.isEmpty()) { message.setText("No hay paquetes confirmados con plantillas elegibles para esta obra."); }
        });
    }
    @FXML private void resolve() {
        if (loadedWork == null || packageChoice.getValue() == null) { message.setText("Consulta la obra y elige un paquete."); return; }
        var selected = selections.stream().map(ComboBox::getValue).filter(Objects::nonNull).toList();
        values.clear();
        DocumentUi.run(form, message, () -> api.resolve(loadedWork, packageChoice.getValue(), selected), documents -> {
            var text = new StringBuilder();
            documents.forEach(d -> {
                text.append("Plantilla #").append(d.templateId()).append(" · versión ").append(d.version()).append('\n');
                if (d.staticDocument()) { text.append("Documento estático, sin sustituciones.\n"); }
                d.fields().forEach(f -> {
                    text.append(f.marker()).append(" · ").append(f.origin()).append(f.required() ? " · obligatorio" : " · opcional")
                            .append(" · ").append(f.status()).append("\n").append(f.presentationValue() == null ? "Sin valor" : f.presentationValue());
                    if ("AMOUNT".equals(f.type()) && f.textValue() != null) { text.append("\nValor preciso: ").append(f.textValue()); }
                    text.append("\n\n");
                });
            });
            values.setText(text.toString());
        });
    }
}
