package com.brica.client.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentDtos.Package;
import com.brica.client.api.DocumentsApiClient;
import com.brica.client.config.ClientConfig;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Context never comes from an editable ID: it is fixed by the selected work/client. */
final class ContextDocumentsWindow {
    private final Long clientId, workId;
    private final DocumentsApiClient api = new DocumentsApiClient(ClientConfig.serverBaseUri());
    private final ListView<Package> packages = new ListView<>();
    private final ListView<PackageTemplate> templates = new ListView<>();
    private final ListView<Version> versions = new ListView<>();
    private final Label message = new Label(), title = new Label("Elige un paquete o crea uno nuevo.");
    private final Stage stage = new Stage();
    private final Button previous = new Button("Anterior"), next = new Button("Siguiente");
    private int page;

    private ContextDocumentsWindow(Window owner, Long clientId, Long workId, String name) {
        this.clientId = clientId; this.workId = workId;
        stage.initOwner(owner); stage.initModality(Modality.WINDOW_MODAL); stage.setTitle("Paquetes documentales · " + name);
        var newPackage = new Button("Nuevo paquete"); newPackage.setOnAction(e -> packageName(null));
        var rename = new Button("Cambiar nombre"); rename.setOnAction(e -> { if (selected() != null) { packageName(selected()); } });
        var refresh = new Button("Actualizar"); refresh.setOnAction(e -> load());
        var newTemplate = new Button("Crear plantilla"); newTemplate.setOnAction(e -> createTemplate());
        var existing = new Button("Agregar plantilla existente"); existing.setOnAction(e -> existing());
        var newVersion = new Button("Registrar versión"); newVersion.setOnAction(e -> version());
        var editTemplate = new Button("Editar plantilla"); editTemplate.setOnAction(e -> editTemplate());
        var deleteTemplate = new Button("Eliminar plantilla"); deleteTemplate.setOnAction(e -> deleteTemplate());
        var discard = new Button("Descartar borrador"); discard.setOnAction(e -> {
            var value = selected(); if (value == null) { return; }
            if (!value.state().equals("DRAFT")) { message.setText("Solo se puede descartar un paquete borrador."); return; }
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Descartar el borrador? No se eliminarán plantillas, versiones ni archivos.", ButtonType.OK, ButtonType.CANCEL);
            alert.initOwner(stage);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { run(() -> api.discard(value), ignored -> packages.getItems().removeIf(p -> p.id().equals(value.id()))); }
        });
        var confirm = new Button("Confirmar paquete"); confirm.setOnAction(e -> {
            var value = selected(); if (value == null) { return; }
            run(() -> api.savePackage(value, workId == null ? clientId : null, workId, value.name(),
                    value.templates().stream().map(PackageTemplate::id).toList(), true), this::replace);
        });
        var remove = new Button("Retirar plantilla"); remove.setOnAction(e -> {
            var value = selected(); var template = templates.getSelectionModel().getSelectedItem();
            if (value == null || template == null) { return; }
            run(() -> api.savePackage(value, workId == null ? clientId : null, workId, value.name(),
                    value.templates().stream().map(PackageTemplate::id).filter(id -> !id.equals(template.id())).toList(), false), this::replace);
        });
        packages.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            templates.getItems().setAll(value == null ? List.of() : value.templates()); versions.getItems().clear();
            title.setText(value == null ? "Elige un paquete" : value.name() + " · " + value.state());
            newTemplate.setDisable(value == null || !value.state().equals("DRAFT"));
            newTemplate.setTooltip(new Tooltip("Crea plantillas dentro de un paquete borrador; después registra sus versiones y confirma el paquete."));
        });
        templates.setCellFactory(view -> new ListCell<>() { @Override protected void updateItem(PackageTemplate t, boolean empty) {
            super.updateItem(t, empty); setText(empty || t == null ? null : t.name() + (t.deleted() ? " (eliminada)" : ""));
        }});
        templates.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            versions.getItems().clear(); if (value == null) { return; }
            api.versions(value.id()).whenComplete((items, failure) -> Platform.runLater(() -> {
                if (templates.getSelectionModel().getSelectedItem() != value) { return; }
                if (failure == null) { versions.getItems().setAll(items); } else { message.setText(DocumentUi.cause(failure).getMessage()); }
            }));
        });
        versions.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2 || versions.getSelectionModel().getSelectedItem() == null) { return; }
            var v = versions.getSelectionModel().getSelectedItem();
            var detail = new Alert(Alert.AlertType.INFORMATION); detail.initOwner(stage); detail.setHeaderText("Versión " + v.number() + " · " + v.originalName());
            var text = new TextArea("SHA-256: " + v.sha256() + "\n" + v.fields().stream().map(f -> f.marker() + (f.required() ? " · obligatorio" : " · opcional")).collect(java.util.stream.Collectors.joining("\n")));
            text.setEditable(false); detail.getDialogPane().setContent(text); detail.show();
        });
        previous.setOnAction(e -> { page--; load(); }); next.setOnAction(e -> { page++; load(); });
        UiPagination.show(previous, false); UiPagination.show(next, false);
        var left = new VBox(8, new Label("1. Paquetes"), packages, new HBox(8, newPackage, refresh), discard, new HBox(8, previous, next));
        var right = new VBox(8, title, rename, new Label("2. Plantillas del paquete"), templates, new HBox(8, newTemplate, existing, remove),
                new HBox(8, editTemplate, deleteTemplate), new Label("3. Versiones de la plantilla seleccionada"), versions, newVersion, confirm);
        VBox.setVgrow(packages, Priority.ALWAYS); VBox.setVgrow(templates, Priority.ALWAYS); VBox.setVgrow(versions, Priority.ALWAYS);
        var split = new SplitPane(left, right); split.setDividerPositions(.35);
        var root = new VBox(10, new Label(workId == null ? "Exclusivo del cliente: " + name : "Exclusivo de la obra: " + name), split, message);
        root.setPadding(new javafx.geometry.Insets(16)); VBox.setVgrow(split, Priority.ALWAYS); message.setWrapText(true);
        stage.setScene(new Scene(root, 1050, 700)); DocumentUi.style(stage.getScene()); stage.show(); load();
    }
    static void show(Window owner, Long clientId, Long workId, String name) { new ContextDocumentsWindow(owner, clientId, workId, name); }
    private Package selected() { return packages.getSelectionModel().getSelectedItem(); }
    private void load() {
        run(() -> api.packages(page, clientId, workId), values -> {
            packages.getItems().setAll(values); UiPagination.show(previous, page > 0); UiPagination.show(next, false);
            int loaded = page;
            api.packages(page + 1, clientId, workId).whenComplete((following, failure) -> Platform.runLater(() -> {
                if (failure == null && loaded == page) { UiPagination.show(next, !following.isEmpty()); }
            }));
        });
    }
    private void replace(Package value) {
        int index = -1; for (int i = 0; i < packages.getItems().size(); i++) { if (packages.getItems().get(i).id().equals(value.id())) { index = i; break; } }
        if (index >= 0) { packages.getItems().set(index, value); } else { packages.getItems().add(value); }
        packages.getSelectionModel().select(value);
    }
    private void packageName(Package previousValue) {
        nameDialog(previousValue == null ? "Nuevo paquete" : "Nombre del paquete", previousValue == null ? "" : previousValue.name(), false,
                values -> api.savePackage(previousValue, workId == null ? clientId : null, workId, values[0],
                        previousValue == null ? List.of() : previousValue.templates().stream().map(PackageTemplate::id).toList(), false), this::replace);
    }
    private void createTemplate() {
        var value = selected(); if (value == null) { return; }
        nameDialog("Crear plantilla en " + value.name(), "", true, inputs -> api.createTemplate(value, inputs[0], inputs[1]), result -> {
            replace(result.documentPackage()); templates.getSelectionModel().select(result.documentPackage().templates().stream()
                    .filter(t -> t.id().equals(result.template().id())).findFirst().orElse(null));
        });
    }
    private void version() {
        var value = templates.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        try {
            var loader = new FXMLLoader(getClass().getResource("template-version-dialog.fxml"));
            var dialog = new Stage(); dialog.initOwner(stage); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Registrar versión · " + value.name()); dialog.setScene(new Scene(loader.load(), 760, 620));
            TemplateVersionDialogController controller = loader.getController();
            controller.configure(api, value.id(), dialog, () -> run(() -> api.versions(value.id()), items -> {
                var current = templates.getSelectionModel().getSelectedItem();
                if (current != null && current.id().equals(value.id())) { versions.getItems().setAll(items); }
            }));
            dialog.show();
        } catch (java.io.IOException failure) { message.setText("No se pudo abrir el registro de versión."); }
    }
    private void existing() {
        var value = selected(); if (value == null) { return; }
        run(() -> api.templates(0, clientId, workId), items -> {
            var choices = items.stream().filter(t -> !value.templates().stream().anyMatch(in -> in.id().equals(t.id()))).toList();
            if (choices.isEmpty()) { message.setText("No hay otras plantillas en este ámbito. Crea una dentro del paquete borrador."); return; }
            var dialog = new ChoiceDialog<Template>(choices.getFirst(), choices); dialog.initOwner(stage); dialog.setTitle("Agregar plantilla del mismo ámbito");
            dialog.showAndWait().ifPresent(t -> {
                var ids = new ArrayList<>(value.templates().stream().map(PackageTemplate::id).toList()); ids.add(t.id());
                run(() -> api.savePackage(value, workId == null ? clientId : null, workId, value.name(), ids, false), this::replace);
            });
        });
    }
    private void editTemplate() {
        var selected = templates.getSelectionModel().getSelectedItem(); if (selected == null || selected.deleted()) { return; }
        run(() -> api.template(selected.id()), value -> {
            var dialog = new Dialog<ButtonType>(); dialog.initOwner(stage); dialog.setTitle("Editar plantilla");
            var name = new TextField(value.name()); var type = new TextField(value.documentType());
            var state = new ComboBox<TemplateState>(); state.getItems().setAll(TemplateState.values()); state.setValue(value.state());
            var error = new Label(); error.setWrapText(true); dialog.getDialogPane().setContent(new VBox(8, new Label("Nombre"), name,
                    new Label("Tipo documental"), type, new Label("Estado"), state, error));
            var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume(); if (name.getText().isBlank() || type.getText().isBlank() || state.getValue() == null) { error.setText("Completa los datos."); return; }
                String capturedName = name.getText(), capturedType = type.getText(); var capturedState = state.getValue();
                AsyncDialogSave.run(dialog, message, error, () -> api.saveTemplate(value.id(), capturedName, capturedType, capturedState), ignored -> refreshSelected());
            });
            DocumentUi.style(dialog); dialog.show();
        });
    }
    private void deleteTemplate() {
        var value = templates.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar lógicamente la plantilla «" + value.name() + "»? Sus versiones y archivos se conservarán.", ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(stage);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { run(() -> api.deleteTemplate(value.id()), ignored -> refreshSelected()); }
    }
    private void refreshSelected() {
        var value = selected(); if (value != null) { run(() -> api.documentPackage(value.id()), this::replace); }
    }
    private <T> void nameDialog(String heading, String initial, boolean withType, Function<String[], CompletableFuture<T>> operation, java.util.function.Consumer<T> success) {
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(stage); dialog.setTitle(heading);
        var name = new TextField(initial); var type = new TextField(); type.setPromptText("Por ejemplo, ACTA_INICIO"); var error = new Label(); error.setWrapText(true);
        var box = new VBox(10, new Label("Nombre"), name); if (withType) { box.getChildren().addAll(new Label("Tipo documental"), type); } box.getChildren().add(error);
        dialog.getDialogPane().setContent(box); var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); if (name.getText().isBlank() || withType && type.getText().isBlank()) { error.setText("Completa los datos."); return; }
            String[] capture = {name.getText(), type.getText()}; AsyncDialogSave.run(dialog, message, error, () -> operation.apply(capture), success);
        });
        DocumentUi.style(dialog); dialog.show();
    }
    private <T> void run(java.util.function.Supplier<CompletableFuture<T>> operation, java.util.function.Consumer<T> success) {
        message.setText("Procesando…");
        operation.get().whenComplete((value, failure) -> Platform.runLater(() -> {
            if (failure != null) { message.setText(DocumentUi.cause(failure).getMessage()); }
            else { message.setText(""); success.accept(value); }
        }));
    }
}
