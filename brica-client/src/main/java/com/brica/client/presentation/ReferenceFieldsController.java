package com.brica.client.presentation;

import com.brica.client.core.ui.AsyncDialogSave;
import com.brica.client.core.ui.UiPagination;
import java.util.List;
import com.brica.client.documents.api.DocumentDtos.*;
import com.brica.client.documents.api.DocumentsApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class ReferenceFieldsController {
    @FXML private VBox pane;
    @FXML private ListView<Field> fields;
    @FXML private Label message;
    @FXML private Label pageLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    private DocumentsApiClient api;
    private List<Attribute> attributes = List.of();
    private int page;

    void configure(DocumentsApiClient api) {
        this.api = api;
        refresh();
    }
    @FXML private void refresh() {
        refresh(() -> { });
    }
    private void refresh(Runnable after) {
        DocumentUi.run(pane, message, () -> api.fields(page), values -> {
            fields.getItems().setAll(values); pageLabel.setText("Página " + (page + 1));
            UiPagination.update(previousButton, nextButton, pageLabel, page, false);
            int loaded = page;
            api.fields(page + 1).whenComplete((following, failure) -> javafx.application.Platform.runLater(() -> {
                if (failure == null && loaded == page) { UiPagination.update(previousButton, nextButton, pageLabel, page, !following.isEmpty()); }
            }));
            after.run();
        });
    }
    @FXML private void previous() { if (page > 0) { page--; refresh(); } }
    @FXML private void next() { if (fields.getItems().size() == 50) { page++; refresh(); } }
    @FXML private void create() {
        var key = new TextField(); key.setPromptText("RESPONSABLE_TECNICO");
        var type = new ComboBox<String>(); type.getItems().setAll("TEXT", "DATE", "AMOUNT");
        type.setValue("TEXT"); type.setPromptText("Tipo del campo adicional");
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(pane.getScene().getWindow());
        dialog.setTitle("Crear campo de referencia");
        var save = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        var form = new GridPane(); form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label("Clave canónica"), key);
        form.addRow(1, new Label("Tipo de dato"), type);
        var help = new Label("La clave es también el nombre visible. Los datos fijos de obra/cliente tienen origen interno; los nuevos campos son adicionales por obra.");
        help.setWrapText(true); help.setMaxWidth(430); form.add(help, 0, 2, 2, 1);
        var error = new Label(); error.setWrapText(true); form.add(error, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(form);
        var submit = dialog.getDialogPane().lookupButton(save);
        submit.disableProperty().bind(key.textProperty().isEmpty().or(type.valueProperty().isNull()));
        submit.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            if (!key.getText().matches("[A-Z0-9_]{1,50}")) { error.setText("Usa de 1 a 50 letras mayúsculas, números o guion bajo."); return; }
            AsyncDialogSave.run(dialog, message, error, () -> api.createAdditionalField(key.getText(), key.getText(), type.getValue()), value -> refresh());
        });
        DocumentUi.style(dialog); dialog.show();
    }
    @FXML private void rename() {
        var field = fields.getSelectionModel().getSelectedItem();
        if (field == null) { message.setText("Selecciona el campo que deseas editar."); return; }
        var dialog = new TextInputDialog(field.visibleName()); dialog.initOwner(pane.getScene().getWindow());
        dialog.setTitle("Editar campo"); dialog.setHeaderText("Clave y origen no se modifican: " + field.key());
        dialog.setContentText("Nombre visible:");
        dialog.showAndWait().ifPresent(name -> DocumentUi.run(pane, message, () -> api.renameField(field.id(), name), value -> refresh()));
    }
    @FXML private void delete() {
        var field = fields.getSelectionModel().getSelectedItem();
        if (field == null) { message.setText("Selecciona el campo que deseas eliminar."); return; }
        var remove = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        var dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar definitivamente el campo " + field.key() + "?\n"
                        + "El servidor comprobará si tiene vínculos con versiones. Solo se eliminará si no los tiene.", remove, ButtonType.CANCEL);
        dialog.setTitle("Eliminar campo de referencia");
        dialog.setHeaderText(field.visibleName());
        dialog.initOwner(pane.getScene().getWindow());
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != remove) { return; }
        DocumentUi.run(pane, message, () -> api.deleteField(field.id()), value -> {
            if (fields.getItems().size() == 1 && page > 0) { page--; }
            refresh(() -> message.setText("Campo " + field.key() + " eliminado."));
        });
    }
}
