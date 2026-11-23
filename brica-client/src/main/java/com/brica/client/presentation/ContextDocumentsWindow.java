package com.brica.client.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import com.brica.client.core.ui.AsyncDialogSave;
import com.brica.client.core.ui.UiPagination;
import com.brica.client.documents.api.DocumentDtos.*;
import com.brica.client.documents.api.DocumentDtos.Package;
import com.brica.client.documents.api.DocumentsApiClient;
import com.brica.client.shared.presentation.NameInputRules;
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
        var renameVersion = new Button("Renombrar versión"); renameVersion.setOnAction(e -> renameVersion());
        var deleteVersion = new Button("Eliminar versión"); deleteVersion.setOnAction(e -> deleteVersion());
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
            run(() -> api.savePackage(value, value.workId() == null ? value.clientId() : null, value.workId(), value.name(),
                    value.templates().stream().map(PackageTemplate::id).toList(), true), this::replace);
        });
        var remove = new Button("Retirar plantilla"); remove.setOnAction(e -> {
            var value = selected(); var template = templates.getSelectionModel().getSelectedItem();
            if (value == null || template == null) { return; }
            run(() -> api.savePackage(value, value.workId() == null ? value.clientId() : null, value.workId(), value.name(),
                    value.templates().stream().map(PackageTemplate::id).filter(id -> !id.equals(template.id())).toList(), false), this::replace);
        });
        var noPackage = packages.getSelectionModel().selectedItemProperty().isNull();
        newTemplate.disableProperty().bind(noPackage); existing.disableProperty().bind(noPackage); rename.disableProperty().bind(noPackage);
        confirm.disableProperty().bind(noPackage); discard.disableProperty().bind(noPackage);
        var noEditableTemplate = javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            var t = templates.getSelectionModel().getSelectedItem(); return t == null || t.deleted();
        }, templates.getSelectionModel().selectedItemProperty());
        newVersion.disableProperty().bind(noEditableTemplate); editTemplate.disableProperty().bind(noEditableTemplate); deleteTemplate.disableProperty().bind(noEditableTemplate);
        remove.disableProperty().bind(templates.getSelectionModel().selectedItemProperty().isNull());
        var noEditableVersion = javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            var v = versions.getSelectionModel().getSelectedItem(); return v == null || v.deleted() || noEditableTemplate.get();
        }, versions.getSelectionModel().selectedItemProperty(), noEditableTemplate);
        renameVersion.disableProperty().bind(noEditableVersion); deleteVersion.disableProperty().bind(noEditableVersion);
        packages.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            templates.getItems().setAll(value == null ? List.of() : value.templates()); versions.getItems().clear();
            title.setText(value == null ? "Elige un paquete" : value.toString());
            newTemplate.setTooltip(new Tooltip("En confirmados, registra primero una versión y después incorpora la plantilla sin reemplazar las anteriores."));
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
                new HBox(8, editTemplate, deleteTemplate), new Label("3. Versiones de la plantilla seleccionada (incluye historial eliminado)"), versions,
                new FlowPane(8, 8, newVersion, renameVersion, deleteVersion), confirm);
        VBox.setVgrow(packages, Priority.ALWAYS); VBox.setVgrow(templates, Priority.ALWAYS); VBox.setVgrow(versions, Priority.ALWAYS);
        var split = new SplitPane(left, right); split.setDividerPositions(.35);
        var root = new VBox(10, new Label(workId == null ? "Paquetes del cliente: " + name + " (incluye confirmados exclusivos de sus obras)"
                : "Paquetes de la obra: " + name + " y reutilizables de su cliente"), split, message);
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
        Long scopeWork = previousValue == null ? workId : previousValue.workId();
        Long scopeClient = previousValue == null ? clientId : previousValue.clientId();
        nameDialog(previousValue == null ? "Nuevo paquete" : "Nombre del paquete", previousValue == null ? "" : previousValue.name(), false,
                values -> api.savePackage(previousValue, scopeWork == null ? scopeClient : null, scopeWork, values[0],
                        previousValue == null ? List.of() : previousValue.templates().stream().map(PackageTemplate::id).toList(), false), this::replace);
    }
    private void createTemplate() {
        var value = selected(); if (value == null) { return; }
        nameDialog("Crear plantilla en " + value.name(), "", true, inputs -> api.createTemplate(value, inputs[0], inputs[1]), result -> {
            replace(result.documentPackage());
            if (value.state().equals("CONFIRMED")) {
                registerVersion(result.template().id(), result.template().name(), () -> {
                    run(() -> api.documentPackage(value.id()), current -> {
                        var ids = new ArrayList<>(current.templates().stream().map(PackageTemplate::id).toList());
                        if (!ids.contains(result.template().id())) { ids.add(result.template().id()); }
                        run(() -> api.savePackage(current, current.workId() == null ? current.clientId() : null, current.workId(), current.name(), ids, false), this::replace);
                    });
                });
            } else {
                templates.getSelectionModel().select(result.documentPackage().templates().stream()
                        .filter(t -> t.id().equals(result.template().id())).findFirst().orElse(null));
            }
        });
    }
    private void version() {
        var value = templates.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        registerVersion(value.id(), value.name(), () -> refreshVersions(value.id()));
    }
    private void registerVersion(Long id, String name, Runnable completed) {
        try {
            var loader = new FXMLLoader(getClass().getResource("template-version-dialog.fxml"));
            var dialog = new Stage(); dialog.initOwner(stage); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Registrar versión · " + name); dialog.setScene(new Scene(loader.load(), 760, 620));
            TemplateVersionDialogController controller = loader.getController();
            controller.configure(api, id, dialog, completed);
            dialog.show();
        } catch (java.io.IOException failure) { message.setText("No se pudo abrir el registro de versión."); }
    }
    private void existing() {
        var value = selected(); if (value == null) { return; }
        run(() -> allTemplates(value, 0, new ArrayList<>()), items -> {
            var choices = items.stream().filter(t -> !value.templates().stream().anyMatch(in -> in.id().equals(t.id()))).toList();
            if (choices.isEmpty()) { message.setText("No hay otras plantillas en este ámbito. Usa Crear plantilla."); return; }
            var dialog = new ChoiceDialog<Template>(choices.getFirst(), choices); dialog.initOwner(stage); dialog.setTitle("Agregar plantilla del mismo ámbito");
            dialog.showAndWait().ifPresent(t -> {
                Runnable append = () -> run(() -> api.documentPackage(value.id()), current -> {
                    var ids = new ArrayList<>(current.templates().stream().map(PackageTemplate::id).toList());
                    if (!ids.contains(t.id())) { ids.add(t.id()); }
                    run(() -> api.savePackage(current, current.workId() == null ? current.clientId() : null, current.workId(), current.name(), ids, false), this::replace);
                });
                if (value.state().equals("CONFIRMED") && t.latestVersion() == null) { registerVersion(t.id(), t.name(), append); }
                else { append.run(); }
            });
        });
    }
    private CompletableFuture<List<Template>> allTemplates(Package value, int page, List<Template> collected) {
        return api.templates(page, value.clientId(), value.workId()).thenCompose(items -> {
            collected.addAll(items);
            return items.size() < 50 ? CompletableFuture.completedFuture(List.copyOf(collected)) : allTemplates(value, page + 1, collected);
        });
    }
    private void refreshVersions(Long id) {
        run(() -> api.versions(id), items -> {
            var selected = templates.getSelectionModel().getSelectedItem();
            if (selected != null && selected.id().equals(id)) { versions.getItems().setAll(items); }
        });
    }
    private void renameVersion() {
        var value = versions.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        nameDialog("Nombre de la versión " + value.number(), value.name(), false,
                inputs -> api.renameVersion(value.templateId(), value.number(), inputs[0]), ignored -> refreshVersions(value.templateId()));
    }
    private void deleteVersion() {
        var value = versions.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la versión «" + value.name() + "»? Se conservarán archivo y referencias históricas. Para nuevas selecciones se usará la anterior disponible.", ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(stage);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            run(() -> api.deleteVersion(value.templateId(), value.number()), ignored -> refreshVersions(value.templateId()));
        }
    }
    private void editTemplate() {
        var selected = templates.getSelectionModel().getSelectedItem(); if (selected == null || selected.deleted()) { return; }
        run(() -> api.template(selected.id()), value -> {
            var dialog = new Dialog<ButtonType>(); dialog.initOwner(stage); dialog.setTitle("Editar plantilla");
            var name = new TextField(value.name()); var type = new TextField(value.documentType());
            NameInputRules.install(name);
            var state = new ComboBox<TemplateState>(); state.getItems().setAll(TemplateState.values()); state.setValue(value.state());
            var error = new Label(); error.setWrapText(true); dialog.getDialogPane().setContent(new VBox(8, new Label("Nombre"), name,
                    new Label("Tipo documental"), type, new Label("Estado"), state, error));
            var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume(); if (!NameInputRules.valid(name.getText(), 150) || type.getText().isBlank() || state.getValue() == null) { error.setText("Revisa el nombre y completa los datos."); return; }
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
        NameInputRules.install(name);
        var box = new VBox(10, new Label("Nombre"), name); if (withType) { box.getChildren().addAll(new Label("Tipo documental"), type); } box.getChildren().add(error);
        dialog.getDialogPane().setContent(box); var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            try { NameInputRules.require(name.getText(), 150); } catch (IllegalArgumentException failure) { error.setText(failure.getMessage()); return; }
            if (withType && type.getText().isBlank()) { error.setText("Completa el tipo documental."); return; }
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
