package com.brica.client.presentation;

import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentsApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ReferenceFieldsController {
    @FXML private VBox pane;
    @FXML private ListView<Field> fields;
    @FXML private TextField key;
    @FXML private TextField visibleName;
    @FXML private ComboBox<Attribute> attribute;
    @FXML private Label message;
    @FXML private Label pageLabel;
    private DocumentsApiClient api;
    private int page;
    @FXML private void initialize() {
        fields.getSelectionModel().selectedItemProperty().addListener((o, before, field) -> {
            if (field != null) { key.setText(field.key()); visibleName.setText(field.visibleName()); }
        });
    }
    void configure(DocumentsApiClient api) {
        this.api = api;
        DocumentUi.run(pane, message, api::attributes, values -> { attribute.getItems().setAll(values); refresh(); });
    }
    @FXML private void refresh() {
        DocumentUi.run(pane, message, () -> api.fields(page), values -> {
            fields.getItems().setAll(values); pageLabel.setText("Página " + (page + 1));
        });
    }
    @FXML private void previous() { if (page > 0) { page--; refresh(); } }
    @FXML private void next() { if (fields.getItems().size() == 50) { page++; refresh(); } }
    @FXML private void create() {
        if (attribute.getValue() == null) { message.setText("Selecciona un atributo aprobado."); return; }
        DocumentUi.run(pane, message, () -> api.createField(key.getText(), visibleName.getText(), attribute.getValue().id()), value -> refresh());
    }
    @FXML private void rename() {
        var field = fields.getSelectionModel().getSelectedItem();
        if (field == null) { message.setText("Selecciona el campo que deseas renombrar."); return; }
        DocumentUi.run(pane, message, () -> api.renameField(field.id(), visibleName.getText()), value -> refresh());
    }
}
