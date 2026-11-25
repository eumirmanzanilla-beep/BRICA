package com.brica.client.documents.presentation;

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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Context never comes from an editable ID: it is fixed by the selected work. */
public final class ContextDocumentsWindow {
    private final long workId;
    private final DocumentsApiClient api = new DocumentsApiClient(ClientConfig.serverBaseUri());
    private final ListView<Package> packages = new ListView<>();
    private final ListView<PackageTemplate> templates = new ListView<>();
    private final ListView<Version> versions = new ListView<>();
    private final Label message = new Label(), title = new Label("Elige un paquete o crea uno nuevo.");
    private final Stage stage = new Stage();
    private final Button previous = new Button("Anterior"), next = new Button("Siguiente");
    private int page;

    private ContextDocumentsWindow(Window owner, long workId, String name) {
        this.workId = workId;
        stage.initOwner(owner); stage.initModality(Modality.WINDOW_MODAL); stage.setTitle("Paquetes documentales · " + name);
        var newPackage = new Button("Nuevo paquete"); newPackage.setOnAction(e -> packageName(null));
        var rename = new Button("Cambiar nombre"); rename.setOnAction(e -> { if (selected() != null) { packageName(selected()); } });
        var refresh = new Button("Actualizar"); refresh.setOnAction(e -> load());
        var newTemplate = new Button("Crear plantilla"); newTemplate.setOnAction(e -> createTemplate());
        var newVersion = new Button("Registrar versión"); newVersion.setOnAction(e -> version());
        var renameVersion = new Button("Renombrar versión"); renameVersion.setOnAction(e -> renameVersion());
        var deleteVersion = new Button("Dar de baja versión"); deleteVersion.setOnAction(e -> deleteVersion());
        var editTemplate = new Button("Editar plantilla"); editTemplate.setOnAction(e -> editTemplate());
        var deleteTemplate = new Button("Dar de baja plantilla"); deleteTemplate.setOnAction(e -> deleteTemplate());
        newPackage.getStyleClass().add("primary-button");
        newTemplate.getStyleClass().add("primary-button");
        newVersion.getStyleClass().add("primary-button");
        deleteTemplate.getStyleClass().add("danger-button");
        deleteVersion.getStyleClass().add("danger-button");
        var discard = new Button("Eliminar paquete"); discard.setOnAction(e -> {
            var value = selected(); if (value == null) { return; }
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar el paquete? No se eliminarán plantillas, versiones ni archivos.", ButtonType.OK, ButtonType.CANCEL);
            alert.initOwner(stage);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { run(() -> api.discard(value), ignored -> packages.getItems().removeIf(p -> p.id().equals(value.id()))); }
        });
        discard.getStyleClass().add("danger-button");
        var templateState = new Button("Cambiar vigencia de plantilla");
        templateState.setOnAction(e -> {
            var selected = templates.getSelectionModel().getSelectedItem(); if (selected == null) { return; }
            run(() -> api.template(selected.id()), t -> run(() -> api.saveTemplate(t.id(), t.name(), t.documentType(),
                    t.state() == TemplateState.CURRENT ? TemplateState.UNAVAILABLE : TemplateState.CURRENT), ignored -> refreshSelected()));
        });
        var versionState = new Button("Cambiar vigencia de versión");
        versionState.setOnAction(e -> {
            var v = versions.getSelectionModel().getSelectedItem(); if (v == null) { return; }
            run(() -> api.setVersionCurrent(v.templateId(), v.number(), !v.current()), ignored -> refreshSelected());
        });
        var noPackage = packages.getSelectionModel().selectedItemProperty().isNull();
        var confirm = new Button("Confirmar paquete");
        confirm.getStyleClass().add("primary-button");
        confirm.disableProperty().bind(noPackage);
        confirm.setOnAction(e -> { var value = selected(); if (value != null) {
            run(() -> api.confirmPackage(value), result -> { replace(result); message.setText("Paquete confirmado y listo."); });
        } });
        newTemplate.disableProperty().bind(noPackage); rename.disableProperty().bind(noPackage);
        discard.disableProperty().bind(noPackage);
        var noEditableTemplate = javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            var t = templates.getSelectionModel().getSelectedItem(); return t == null || t.deleted();
        }, templates.getSelectionModel().selectedItemProperty());
        newVersion.disableProperty().bind(noEditableTemplate); editTemplate.disableProperty().bind(noEditableTemplate); deleteTemplate.disableProperty().bind(noEditableTemplate);
        templateState.disableProperty().bind(noEditableTemplate);
        var noEditableVersion = javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            var v = versions.getSelectionModel().getSelectedItem(); return v == null || v.deleted() || noEditableTemplate.get();
        }, versions.getSelectionModel().selectedItemProperty(), noEditableTemplate);
        renameVersion.disableProperty().bind(noEditableVersion); deleteVersion.disableProperty().bind(noEditableVersion);
        versionState.disableProperty().bind(noEditableVersion);
        packages.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            templates.getItems().setAll(value == null ? List.of() : value.templates()); versions.getItems().clear();
            title.setText(value == null ? "Elige un paquete" : value.toString());
        });
        templates.setCellFactory(view -> new ListCell<>() { @Override protected void updateItem(PackageTemplate t, boolean empty) {
            super.updateItem(t, empty); setText(empty || t == null ? null : t.name() + " · " + t.state());
        }});
        templates.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            templateState.setText(value == null ? "Selecciona plantilla" : value.state() == TemplateState.CURRENT ? "Vigente" : "No vigente");
            versions.getItems().clear(); if (value == null) { return; }
            api.versions(value.id()).whenComplete((items, failure) -> Platform.runLater(() -> {
                if (templates.getSelectionModel().getSelectedItem() != value) { return; }
                if (failure == null) { versions.getItems().setAll(items.stream().filter(v -> !v.deleted()).toList()); } else { message.setText(DocumentUi.cause(failure).getMessage()); }
            }));
        });
        versions.getSelectionModel().selectedItemProperty().addListener((o, old, value) ->
                versionState.setText(value == null ? "Selecciona versión" : value.current() ? "Vigente" : "No vigente"));
        versions.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2 || versions.getSelectionModel().getSelectedItem() == null) { return; }
            var v = versions.getSelectionModel().getSelectedItem();
            var detail = new Alert(Alert.AlertType.INFORMATION); detail.initOwner(stage); detail.setHeaderText("Versión " + v.number() + " · " + v.originalName());
            var text = new TextArea("SHA-256: " + v.sha256() + "\n" + v.fields().stream().map(f -> f.marker() + (f.required() ? " · obligatorio" : " · opcional")).collect(java.util.stream.Collectors.joining("\n")));
            text.setEditable(false); detail.getDialogPane().setContent(text); detail.show();
        });
        previous.setOnAction(e -> { page--; load(); }); next.setOnAction(e -> { page++; load(); });
        UiPagination.show(previous, false); UiPagination.show(next, false);
        var packageHeading = sectionHeading("Paquete");
        var templateHeading = sectionHeading("Plantillas");
        var versionHeading = sectionHeading("Versiones");
        var packageActions = new HBox(8, newPackage, refresh); packageActions.getStyleClass().add("package-action-row");
        var templateActions = new HBox(8, newTemplate); templateActions.getStyleClass().add("package-action-row");
        var templateMaintenance = new HBox(8, editTemplate, deleteTemplate, templateState); templateMaintenance.getStyleClass().add("package-action-row");
        var versionActions = new FlowPane(8, 8, newVersion, renameVersion, deleteVersion, versionState); versionActions.getStyleClass().add("package-action-row");
        var left = new VBox(10, packageHeading, new Label("Selecciona el expediente documental de esta obra."), packages,
                packageActions, discard, new HBox(8, previous, next));
        var right = new VBox(10, title, rename, templateHeading, new Label("Administra las plantillas incluidas en el paquete."),
                templates, templateActions, templateMaintenance, versionHeading,
                new Label("Versiones de la plantilla seleccionada."), versions, versionActions, confirm);
        left.getStyleClass().add("package-step-pane"); right.getStyleClass().add("package-step-pane");
        title.getStyleClass().add("package-selection-title");
        VBox.setVgrow(packages, Priority.ALWAYS); VBox.setVgrow(templates, Priority.ALWAYS); VBox.setVgrow(versions, Priority.ALWAYS);
        var split = new SplitPane(left, right); split.setDividerPositions(.35);
        var context = new Label("Paquetes vinculados a la obra " + name
                + ". El servidor determina su cliente vigente.");
        context.setWrapText(true); context.getStyleClass().add("package-context");
        message.getStyleClass().add("package-feedback");
        message.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.managedProperty().bind(message.visibleProperty());
        var root = new VBox(14, context, split, message);
        root.getStyleClass().addAll("app-root", "package-context-root");
        root.setPadding(new javafx.geometry.Insets(16)); VBox.setVgrow(split, Priority.ALWAYS); message.setWrapText(true);
        stage.setScene(new Scene(root, 1050, 700)); DocumentUi.style(stage.getScene()); stage.show(); load();
    }
    /** Opens the work-scoped flow without trusting a client identifier supplied by the desktop application. */
    public static void showForWork(Window owner, long workId, String name) {
        new ContextDocumentsWindow(owner, workId, name);
    }
    private Package selected() { return packages.getSelectionModel().getSelectedItem(); }
    private static Label sectionHeading(String text) {
        var heading = new Label(text);
        heading.getStyleClass().add("package-section-title");
        return heading;
    }
    private void load() {
        run(() -> api.packages(page, null, workId), values -> {
            packages.getItems().setAll(values); UiPagination.show(previous, page > 0); UiPagination.show(next, false);
            int loaded = page;
            api.packages(page + 1, null, workId).whenComplete((following, failure) -> Platform.runLater(() -> {
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
        nameDialog(previousValue == null ? "Nuevo paquete" : "Nombre del paquete", previousValue == null ? "" : previousValue.name(),
                values -> api.savePackage(previousValue, null, workId, values[0],
                        previousValue == null ? List.of() : previousValue.templates().stream().map(PackageTemplate::id).toList(), false), this::replace);
    }
    private void createTemplate() {
        var value = selected(); if (value == null) { return; }
        nameDialog("Crear plantilla en " + value.name(), "", inputs -> api.createDocumentalTemplate(value, inputs[0]), result -> {
            replace(result.documentPackage());
            templates.getSelectionModel().select(result.documentPackage().templates().stream()
                    .filter(t -> t.id().equals(result.template().id())).findFirst().orElse(null));
        });
    }
    private void version() {
        var value = templates.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        registerVersion(value.id(), value.name(), () -> refreshVersions(value.id()));
    }
    private void registerVersion(Long id, String name, Runnable completed) {
        try {
            var loader = TemplateVersionDialog.loader();
            var dialog = new Stage(); dialog.initOwner(stage); dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Registrar versión · " + name); dialog.setScene(new Scene(loader.load(), 760, 620));
            TemplateVersionDialogController controller = loader.getController();
            controller.configure(api, id, dialog, completed);
            dialog.show();
        } catch (java.io.IOException failure) { message.setText("No se pudo abrir el registro de versión."); }
    }
    private void refreshVersions(Long id) {
        run(() -> api.versions(id), items -> {
            var selected = templates.getSelectionModel().getSelectedItem();
            if (selected != null && selected.id().equals(id)) { versions.getItems().setAll(items.stream().filter(v -> !v.deleted()).toList()); }
        });
    }
    private void renameVersion() {
        var value = versions.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        nameDialog("Nombre de la versión " + value.number(), value.name(),
                inputs -> api.renameVersion(value.templateId(), value.number(), inputs[0]), ignored -> refreshVersions(value.templateId()));
    }
    private void deleteVersion() {
        var value = versions.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Dar de baja la versión «" + value.name() + "»? Se conservarán archivo y referencias históricas. Para nuevas selecciones se usará la anterior disponible.", ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(stage);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            run(() -> api.deleteVersion(value.templateId(), value.number()), ignored -> refreshVersions(value.templateId()));
        }
    }
    private void editTemplate() {
        var selected = templates.getSelectionModel().getSelectedItem(); if (selected == null || selected.deleted()) { return; }
        run(() -> api.template(selected.id()), value -> {
            var dialog = new Dialog<ButtonType>(); dialog.initOwner(stage); dialog.setTitle("Editar plantilla");
            var name = new TextField(value.name());
            NameInputRules.install(name);
            var state = new ComboBox<TemplateState>(); state.getItems().setAll(TemplateState.values()); state.setValue(value.state());
            var error = new Label(); error.setWrapText(true); dialog.getDialogPane().setContent(new VBox(8, new Label("Nombre"), name,
                    new Label("Tipo"), new Label(value.documentType()), new Label("Estado"), state, error));
            var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume(); if (!NameInputRules.valid(name.getText(), 150) || state.getValue() == null) { error.setText("Revisa el nombre y completa los datos."); return; }
                String capturedName = name.getText(); var capturedState = state.getValue();
                AsyncDialogSave.run(dialog, message, error, () -> api.saveTemplate(value.id(), capturedName, value.documentType(), capturedState), ignored -> refreshSelected());
            });
            DocumentUi.style(dialog); dialog.show();
        });
    }
    private void deleteTemplate() {
        var value = templates.getSelectionModel().getSelectedItem(); if (value == null || value.deleted()) { return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Dar de baja la plantilla «" + value.name() + "»? Sus versiones y archivos se conservarán como historial.", ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(stage);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) { run(() -> api.deleteTemplate(value.id()), ignored -> refreshSelected()); }
    }
    private void refreshSelected() {
        var value = selected(); if (value != null) { run(() -> api.documentPackage(value.id()), this::replace); }
    }
    private <T> void nameDialog(String heading, String initial, Function<String[], CompletableFuture<T>> operation, java.util.function.Consumer<T> success) {
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(stage); dialog.setTitle(heading);
        var name = new TextField(initial); var error = new Label(); error.setWrapText(true);
        NameInputRules.install(name);
        var box = new VBox(10, new Label("Nombre"), name, error);
        dialog.getDialogPane().setContent(box); var save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            try { NameInputRules.require(name.getText(), 150); } catch (IllegalArgumentException failure) { error.setText(failure.getMessage()); return; }
            String[] capture = {name.getText()}; AsyncDialogSave.run(dialog, message, error, () -> operation.apply(capture), success);
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
