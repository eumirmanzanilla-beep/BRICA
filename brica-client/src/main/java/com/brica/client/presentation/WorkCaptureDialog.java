package com.brica.client.presentation;

import java.util.List;
import java.util.function.Consumer;
import com.brica.client.api.DocumentDtos.Field;
import com.brica.client.api.WorksApiClient;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

final class WorkCaptureDialog {
    private WorkCaptureDialog() { }
    static void show(Window owner, WorksApiClient api, WorksApiClient.VersionedInformation versioned,
            List<Field> fields, Label progress, Consumer<WorksApiClient.CompleteInformation> saved) {
        var work = versioned.information().work();
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(owner); dialog.setTitle("Preparar información · " + work.name()); dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        var heading = new Label("1. Contrato y presupuesto base (opcional)");
        var attachments = new ListView<WorksApiClient.Attachment>(); attachments.setPrefHeight(190);
        var kind = new ComboBox<String>(); kind.getItems().setAll("Contrato", "Presupuesto base"); kind.setValue("Contrato");
        var attach = new Button("Adjuntar archivo"); var download = new Button("Guardar copia del seleccionado");
        var note = new Label("DOCX, PDF o XLSX, hasta 10 MiB. Puedes omitir este paso. Los archivos se guardan sin extraer datos."); note.setWrapText(true);
        var message = new Label(); message.setWrapText(true);
        var docs = new VBox(10, note, new HBox(8, kind, attach), attachments, download);
        var form = new WorkInformationForm(work, versioned.information().documentInformation(), fields, true);
        var formScroll = new ScrollPane(form.node); formScroll.setFitToWidth(true);
        var preview = new TextArea(); preview.setEditable(false); preview.setWrapText(true);
        var content = new StackPane(docs); content.setPrefHeight(490);
        var back = new Button("Volver"); var next = new Button("Continuar"); var skip = new Button("Omitir documentos"); var finish = new Button("Guardar información");
        var controls = new HBox(10, back, skip, next, finish); int[] step = {0};
        Runnable render = () -> {
            heading.setText(List.of("1. Contrato y presupuesto base (opcional)", "2. Información de la obra", "3. Revisión de campos").get(step[0]));
            if (step[0] == 0) { content.getChildren().setAll(docs); }
            if (step[0] == 1) { content.getChildren().setAll(formScroll); }
            if (step[0] == 2) { content.getChildren().setAll(preview); }
            UiPagination.show(back, step[0] > 0); UiPagination.show(skip, step[0] == 0);
            UiPagination.show(next, step[0] < 2); UiPagination.show(finish, step[0] == 2);
        };
        next.setOnAction(event -> {
            try { if (step[0] == 1) { preview.setText("Obra #" + work.id() + " · cliente #" + work.clientId() + "\n\n" + form.preview()); }
                step[0]++; message.setText(""); render.run();
            } catch (RuntimeException failure) { message.setText("Revisa los datos: " + failure.getMessage()); }
        });
        back.setOnAction(event -> { step[0]--; render.run(); }); skip.setOnAction(event -> { step[0] = 1; render.run(); });
        attach.setOnAction(event -> {
            var chooser = new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos de obra", "*.docx", "*.pdf", "*.xlsx"));
            var file = chooser.showOpenDialog(owner); if (file == null) { return; }
            controls.setDisable(true); attach.setDisable(true); message.setText("Adjuntando…");
            api.upload(work.id(), kind.getValue().equals("Contrato") ? "CONTRACT" : "BASE_BUDGET", file.toPath())
                    .whenComplete((value, failure) -> javafx.application.Platform.runLater(() -> {
                        controls.setDisable(false); attach.setDisable(false);
                        if (failure == null) { attachments.getItems().add(value); message.setText("Archivo guardado."); }
                        else { message.setText(DocumentUi.cause(failure).getMessage()); }
                    }));
        });
        download.setOnAction(event -> {
            var value = attachments.getSelectionModel().getSelectedItem(); if (value == null) { return; }
            var chooser = new FileChooser(); chooser.setInitialFileName(value.originalName()); var file = chooser.showSaveDialog(owner);
            if (file != null) { DocumentUi.run(docs, message, () -> api.download(work.id(), value.id(), file.toPath()), ignored -> message.setText("Copia guardada.")); }
        });
        finish.setOnAction(event -> {
            try {
                var info = form.information(); String name = form.name.getText(), location = form.location.getText(), description = form.description.getText();
                AsyncDialogSave.run(dialog, progress, message,
                        () -> api.updateInformation(work.id(), versioned.etag(), name, location, description, info), saved);
            } catch (RuntimeException failure) { message.setText(failure.getMessage()); }
        });
        dialog.getDialogPane().setContent(new VBox(12, heading, content, message, controls));
        dialog.getDialogPane().setPrefWidth(780); DocumentUi.style(dialog); render.run(); dialog.show();
        api.allAttachments(work.id()).whenComplete((items, failure) -> javafx.application.Platform.runLater(() -> {
            if (failure == null) { attachments.getItems().setAll(items); } else { message.setText(DocumentUi.cause(failure).getMessage()); }
        }));
    }
}
