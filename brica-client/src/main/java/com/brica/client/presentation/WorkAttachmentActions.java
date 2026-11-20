package com.brica.client.presentation;

import com.brica.client.api.WorksApiClient;
import javafx.scene.control.*;
import javafx.stage.Window;

final class WorkAttachmentActions {
    private WorkAttachmentActions() { }
    static Button delete(Window owner, WorksApiClient api, long workId, ListView<WorksApiClient.Attachment> list, Label message) {
        var button = new Button("Eliminar adjunto");
        button.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull().or(list.disabledProperty()));
        button.setOnAction(event -> {
            var value = list.getSelectionModel().getSelectedItem(); if (value == null) { return; }
            var confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el adjunto «" + value.originalName()
                    + "»? Dejará de estar disponible en esta obra. Se conservarán el archivo y su historial.", ButtonType.OK, ButtonType.CANCEL);
            confirm.initOwner(owner);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) { return; }
            DocumentUi.run(list, message, () -> api.deleteAttachment(workId, value.id()), ignored -> {
                list.getItems().remove(value); message.setText("Adjunto eliminado.");
            });
        });
        return button;
    }
}
