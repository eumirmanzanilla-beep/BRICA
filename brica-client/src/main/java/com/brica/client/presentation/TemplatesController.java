package com.brica.client.presentation;

import java.io.IOException;
import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentsApiClient;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TemplatesController {
    @FXML private VBox pane;
    @FXML private ListView<Template> templates;
    @FXML private ListView<Version> versions;
    @FXML private TextField operationId;
    @FXML private TextArea detail;
    @FXML private Label message;
    @FXML private Label pageLabel;
    @FXML private CheckBox includeDeleted;
    private DocumentsApiClient api;
    private Runnable changed = () -> { };
    private int page;
    @FXML private void initialize() {
        templates.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            if (value != null) {
                DocumentUi.run(pane, message, () -> api.versions(value.id()), items -> versions.getItems().setAll(items));
            } else { versions.getItems().clear(); detail.clear(); }
        });
        versions.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            if (value == null) { detail.clear(); return; }
            var text = new StringBuilder("Solo lectura · " + value.status() + "\nArchivo: " + value.originalName() + "\nSHA-256: " + value.sha256() + "\n");
            if (value.staticDocument()) { text.append("Documento estático: sin campos.\n"); }
            value.fields().forEach(f -> text.append(f.marker()).append(" · ").append(f.visibleName()).append(f.required() ? " · obligatorio\n" : " · opcional\n"));
            text.append("\nAnálisis conservado:\n").append(value.analysisJson()); detail.setText(text.toString());
        });
    }
    void configure(DocumentsApiClient api, Runnable changed) { this.api = api; this.changed = changed; refresh(); }
    @FXML void refresh() {
        refresh(() -> { });
    }
    private void refresh(Runnable after) {
        DocumentUi.run(pane, message, () -> api.templates(page, includeDeleted.isSelected()), values -> {
            templates.getItems().setAll(values); pageLabel.setText("Página " + (page + 1));
            after.run();
        });
    }
    @FXML private void changeDeletedFilter() { page = 0; refresh(); }
    @FXML private void previous() { if (page > 0) { page--; refresh(); } }
    @FXML private void next() { if (templates.getItems().size() == 50) { page++; refresh(); } }
    @FXML private void create() {
        edit(null);
    }
    @FXML private void update() {
        var selected = templates.getSelectionModel().getSelectedItem();
        if (selected == null) { message.setText("Selecciona una plantilla."); return; }
        if (selected.deleted()) { message.setText("La plantilla eliminada es de solo lectura."); return; }
        edit(selected);
    }
    private void edit(Template selected) {
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(pane.getScene().getWindow());
        dialog.setTitle(selected == null ? "Crear plantilla" : "Editar plantilla");
        var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        var name = new TextField(selected == null ? "" : selected.name());
        var type = new ComboBox<String>(); type.setEditable(true);
        type.getItems().setAll(templates.getItems().stream().map(Template::documentType).distinct().sorted().toList());
        if (selected != null) { type.setValue(selected.documentType()); }
        var state = new ComboBox<TemplateState>(); state.getItems().setAll(TemplateState.values());
        state.setValue(selected == null ? TemplateState.DRAFT : selected.state());
        var form = new javafx.scene.layout.GridPane(); form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label("Nombre"), name);
        form.addRow(1, new Label("Tipo documental (p. ej. Acta de inicio)"), type);
        form.addRow(2, new Label("Estado"), state);
        form.addRow(3, new Label("Formato de archivo"), new Label("DOCX"));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().lookupButton(save).disableProperty().bind(name.textProperty().isEmpty()
                .or(type.getEditor().textProperty().isEmpty()).or(state.valueProperty().isNull()));
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != save) { return; }
        DocumentUi.run(pane, message, () -> api.saveTemplate(selected == null ? null : selected.id(),
                name.getText(), type.getEditor().getText(), state.getValue()), value -> { refresh(); changed.run(); });
    }
    @FXML private void delete() {
        var selected = templates.getSelectionModel().getSelectedItem();
        if (selected == null) { return; }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar lógicamente la plantilla? Sus versiones se conservarán.", ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) { return; }
        DocumentUi.run(pane, message, () -> api.deleteTemplate(selected.id()), value -> { refresh(); changed.run(); });
    }
    @FXML private void addVersion() {
        var selected = templates.getSelectionModel().getSelectedItem();
        if (selected == null || selected.deleted()) { message.setText("Selecciona una plantilla no eliminada."); return; }
        try {
            var loader = new FXMLLoader(getClass().getResource("template-version-dialog.fxml"));
            var dialog = new Stage(); dialog.initOwner(pane.getScene().getWindow()); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Registrar versión · " + selected.name()); dialog.setScene(new Scene(loader.load(), 760, 620));
            TemplateVersionDialogController controller = loader.getController();
            controller.configure(api, selected.id(), dialog, () -> { refresh(); changed.run(); });
            dialog.show();
        } catch (IOException failure) { message.setText("No se pudo abrir el registro de versión."); }
    }
    @FXML private void recover() {
        DocumentUi.run(pane, message, () -> api.registration(operationId.getText(), true), value -> {
            String result = value.status() + (value.failure() == null ? " · versión " + value.versionNumber() : " · " + value.failure());
            refresh(() -> message.setText(result)); changed.run();
        });
    }
}
