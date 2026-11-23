package com.brica.client.presentation;

import com.brica.client.core.ui.AsyncDialogSave;
import java.util.List;
import java.util.function.Consumer;
import com.brica.client.documents.api.DocumentDtos.Field;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

final class WorkDocumentInformationDialog {
    private WorkDocumentInformationDialog() { }
    static void show(Window owner, WorkDto work, WorksApiClient api, WorksApiClient.VersionedDocumentInformation versioned,
            List<Field> fields, boolean editable, Label progress, Consumer<WorksApiClient.CompleteInformation> saved) {
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(owner); dialog.setTitle(editable ? "Editar obra" : "Información de la obra");
        var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(editable ? List.of(save, ButtonType.CLOSE) : List.of(ButtonType.CLOSE));
        var form = new WorkInformationForm(work, versioned.information(), fields, editable);
        var error = new Label(); error.setWrapText(true);
        var body = new VBox(10, new Label("Obra #" + work.id() + " · cliente #" + work.clientId()), form.node, error);
        if (api != null) {
            var attachments = new ListView<WorksApiClient.Attachment>(); attachments.setPrefHeight(140);
            var download = new Button("Guardar copia del adjunto seleccionado");
            body.getChildren().addAll(new Label("Contrato y presupuesto base"), attachments, download);
            if (editable) { body.getChildren().add(WorkAttachmentActions.delete(owner, api, work.id(), attachments, error)); }
            api.allAttachments(work.id()).whenComplete((items, failure) -> javafx.application.Platform.runLater(() -> {
                if (failure == null) { attachments.getItems().setAll(items); } else { error.setText(DocumentUi.cause(failure).getMessage()); }
            }));
            download.setOnAction(event -> {
                var value = attachments.getSelectionModel().getSelectedItem(); if (value == null) { return; }
                var chooser = new javafx.stage.FileChooser(); chooser.setInitialFileName(value.originalName()); var file = chooser.showSaveDialog(owner);
                if (file != null) { DocumentUi.run(attachments, error, () -> api.download(work.id(), value.id(), file.toPath()), ignored -> error.setText("Copia guardada.")); }
            });
        }
        var scroll = new ScrollPane(body); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(530);
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefWidth(740); dialog.setResizable(true);
        if (editable) {
            dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                try {
                    var info = form.information(); String name = form.name.getText(), location = form.location.getText(), description = form.description.getText();
                    AsyncDialogSave.run(dialog, progress, error, () -> api.updateInformation(work.id(), versioned.etag(), name, location, description, info), saved);
                } catch (RuntimeException failure) { error.setText("Revisa los datos: " + failure.getMessage()); }
            });
        }
        DocumentUi.style(dialog); dialog.show();
    }
    // Compatibility for the graphical smoke and older callers; regular flows pass the complete work.
    static void show(Window owner, long id, WorksApiClient api, WorksApiClient.VersionedDocumentInformation versioned,
            List<Field> fields, boolean editable, Consumer<Void> saved) {
        show(owner, new WorkDto(id, null, "Obra", null, null, null, null), api, versioned, fields, editable,
                new Label(), value -> saved.accept(null));
    }
}
