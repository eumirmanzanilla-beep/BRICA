package com.brica.client.works.presentation;

import com.brica.client.clients.api.ClientDto;
import com.brica.client.core.ui.UiPagination;
import com.brica.client.documents.api.DocumentDtos.Field;
import com.brica.client.documents.presentation.DocumentUi;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Guided, in-memory creation flow. Closing it only discards local selections not yet sent to the server. */
public final class WorkCreationWizard {
    private static final List<String> STEPS = List.of(
            "Datos iniciales", "Adjuntos opcionales", "Información de la obra", "Revisión y guardado");

    private final Window owner;
    private final WorksApiClient api;
    private final List<ClientDto> clients;
    private final List<Field> fields;
    private final Label pageStatus;
    private final Consumer<WorksApiClient.CompleteInformation> saved;
    private final WorkCreationDraft draft;
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final Label heading = new Label();
    private final Label detail = new Label();
    private final Label message = new Label();
    private final StackPane content = new StackPane();
    private final Button back = new Button("Volver");
    private final Button next = new Button("Continuar");
    private final Button finish = new Button("Guardar información");
    private int step;
    private WorkInformationForm form;

    private WorkCreationWizard(
            Window owner,
            WorksApiClient api,
            List<ClientDto> clients,
            List<Field> fields,
            Label pageStatus,
            Consumer<WorksApiClient.CompleteInformation> saved,
            WorkCreationDraft draft,
            int initialStep) {
        this.owner = owner;
        this.api = api;
        this.clients = List.copyOf(clients);
        this.fields = List.copyOf(fields);
        this.pageStatus = pageStatus;
        this.saved = saved;
        this.draft = draft;
        this.step = initialStep;
        configureDialog();
    }

    public static void show(
            Window owner,
            WorksApiClient api,
            List<ClientDto> clients,
            List<Field> fields,
            Label pageStatus,
            Consumer<WorksApiClient.CompleteInformation> saved) {
        new WorkCreationWizard(owner, api, clients, fields, pageStatus, saved, new WorkCreationDraft(), 0).open();
    }

    /** Preserves the existing capture action for works that were already registered. */
    public static void showExisting(
            Window owner,
            WorksApiClient api,
            WorksApiClient.VersionedInformation information,
            List<Field> fields,
            Label pageStatus,
            Consumer<WorksApiClient.CompleteInformation> saved) {
        var draft = new WorkCreationDraft();
        draft.work(information.information().work());
        draft.information(information);
        new WorkCreationWizard(owner, api, List.of(), fields, pageStatus, saved, draft, 1).open();
    }

    private void configureDialog() {
        dialog.initOwner(owner);
        dialog.setTitle("Nueva obra");
        dialog.setResizable(true);
        heading.getStyleClass().add("work-wizard-heading");
        detail.getStyleClass().add("work-wizard-progress");
        detail.setWrapText(true);
        message.getStyleClass().add("work-wizard-message");
        message.setWrapText(true);
        content.setPrefHeight(465);
        VBox.setVgrow(content, Priority.ALWAYS);
        var controls = new HBox(10, back, next, finish);
        controls.getStyleClass().add("work-wizard-actions");
        dialog.getDialogPane().setContent(new VBox(12, heading, detail, content, message, controls));
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
        back.setOnAction(event -> { step--; message.setText(""); render(); });
        next.setOnAction(event -> continueStep());
        finish.setOnAction(event -> save());
        DocumentUi.style(dialog);
    }

    private void open() {
        render();
        dialog.show();
    }

    private void render() {
        heading.setText((step + 1) + ". " + STEPS.get(step));
        detail.setText(step == 0
                ? "Elige el cliente vigente e identifica la obra. Podrás completar los demás datos después."
                : "El progreso vive solo mientras esta ventana está abierta.");
        if (step == 0) { content.getChildren().setAll(initialPane()); }
        if (step == 1) { content.getChildren().setAll(attachmentsPane()); }
        if (step == 2) { content.getChildren().setAll(informationPane()); }
        if (step == 3) { content.getChildren().setAll(reviewPane()); }
        UiPagination.show(back, step > 0);
        UiPagination.show(next, step < 3);
        UiPagination.show(finish, step == 3);
    }

    private VBox initialPane() {
        var client = new ComboBox<ClientDto>();
        client.getItems().setAll(clients);
        client.setValue(draft.client());
        client.setPromptText("Selecciona un cliente vigente");
        client.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClientDto value) { return value == null ? "" : value.name(); }
            @Override public ClientDto fromString(String value) { return null; }
        });
        var name = new TextField(draft.name());
        name.setPromptText("Ej. Torre_3_Ampliacion");
        var nameError = new Label();
        nameError.getStyleClass().add("work-wizard-message");
        name.textProperty().addListener((observable, previous, value) -> nameError.setText(WorkNameInputRules.error(value)));
        nameError.setText(WorkNameInputRules.error(name.getText()));
        var description = new TextArea(draft.description());
        description.setWrapText(true);
        description.setPrefRowCount(4);
        var pane = new VBox(9,
                new Label("Cliente"), client,
                new Label("Nombre de la obra"), name, nameError,
                new Label("Descripción (opcional)"), description);
        pane.getStyleClass().add("work-wizard-step");
        next.setUserData(new InitialInput(client, name, description));
        return pane;
    }

    private VBox attachmentsPane() {
        var contract = attachmentRow(
                "Contrato (opcional)", "Puedes continuar sin archivo.",
                "CONTRACT", draft.contractAttachment(), path -> draft.contractAttachment(path));
        var budget = attachmentRow(
                "Presupuesto base (opcional)", "No inicia extracción automática.",
                "BASE_BUDGET", draft.baseBudgetAttachment(), path -> draft.baseBudgetAttachment(path));
        var uploaded = new ListView<WorksApiClient.Attachment>();
        uploaded.setPrefHeight(150);
        var delete = new Button("Eliminar seleccionado");
        delete.disableProperty().bind(uploaded.getSelectionModel().selectedItemProperty().isNull());
        delete.setOnAction(event -> deleteAttachment(uploaded));
        if (draft.uploadedContract() != null) { uploaded.getItems().add(draft.uploadedContract()); }
        if (draft.uploadedBaseBudget() != null) { uploaded.getItems().add(draft.uploadedBaseBudget()); }
        var pane = new VBox(12, contract, budget, new Label("Adjuntos cargados en esta sesión"), uploaded, delete);
        pane.getStyleClass().add("work-wizard-step");
        next.setUserData(uploaded);
        return pane;
    }

    private VBox attachmentRow(String title, String note, String kind, Path current, Consumer<Path> selected) {
        var value = new Label(fileName(current));
        value.setWrapText(true);
        var choose = new Button("Elegir archivo");
        choose.setOnAction(event -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Documentos de obra", "*.docx", "*.pdf", "*.xls", "*.xlsx"));
            var file = chooser.showOpenDialog(owner);
            if (file != null) {
                uploadAttachment(kind, file.toPath(), selected, choose, value);
            }
        });
        return new VBox(4, new Label(title), new Label(note), new HBox(10, choose, value));
    }

    private javafx.scene.Node informationPane() {
        if (form == null) {
            var information = draft.information();
            form = new WorkInformationForm(information.information().work(), information.information().documentInformation(), fields, true);
            if (draft.client() != null) {
                form.description.setText(draft.description());
            }
        }
        var scroll = new ScrollPane(form.node);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private javafx.scene.Node reviewPane() {
        var review = new TextArea(form.preview());
        review.setEditable(false);
        review.setWrapText(true);
        review.setPrefRowCount(18);
        var note = new Label("Revisa los valores. Volver conserva lo capturado en esta ventana; Guardar aplica los cambios con la versión actual de la obra.");
        note.setWrapText(true);
        return new VBox(9, note, review);
    }

    private void continueStep() {
        try {
            if (step == 0) {
                createWork((InitialInput) next.getUserData());
                return;
            }
            if (step == 1) {
                step++;
                message.setText("");
                render();
                return;
            }
            if (step == 2) { form.information(); }
            step++;
            message.setText("");
            render();
        } catch (RuntimeException failure) {
            message.setText("Revisa los datos: " + failure.getMessage());
        }
    }

    private void createWork(InitialInput input) {
        var selected = input.client().getValue();
        if (selected == null) { message.setText("Selecciona un cliente vigente."); return; }
        String name = input.name().getText();
        try {
            WorkNameInputRules.require(name);
        } catch (IllegalArgumentException failure) {
            message.setText(failure.getMessage());
            return;
        }
        draft.client(selected);
        draft.name(name);
        draft.description(input.description().getText());
        setBusy(true, "Creando obra…");
        api.create(selected.id(), name, null, draft.description(), null, null)
                .thenCompose(work -> {
                    draft.work(work);
                    return api.getInformation(work.id());
                })
                .whenComplete((information, failure) -> Platform.runLater(() -> {
                    setBusy(false, "");
                    if (failure != null) {
                        message.setText(DocumentUi.cause(failure).getMessage());
                        return;
                    }
                    draft.information(information);
                    step = 1;
                    render();
                }));
    }

    private void uploadAttachment(
            String kind, Path file, Consumer<Path> selected, Button choose, Label value) {
        if (draft.work() == null) {
            message.setText("Crea la obra antes de adjuntar un archivo.");
            return;
        }
        var previous = "CONTRACT".equals(kind) ? draft.uploadedContract() : draft.uploadedBaseBudget();
        choose.setDisable(true);
        value.setText("Validando y cargando " + fileName(file) + "…");
        message.setText("");
        api.upload(draft.work().id(), kind, file).whenComplete((attachment, failure) -> Platform.runLater(() -> {
            choose.setDisable(false);
            if (failure != null) {
                value.setText(fileName("CONTRACT".equals(kind) ? draft.contractAttachment() : draft.baseBudgetAttachment()));
                message.setText(DocumentUi.cause(failure).getMessage());
                return;
            }
            selected.accept(file);
            if ("CONTRACT".equals(kind)) {
                draft.uploadedContract(attachment);
            } else {
                draft.uploadedBaseBudget(attachment);
            }
            value.setText(fileName(file));
            if (previous != null) {
                api.deleteAttachment(draft.work().id(), previous.id()).whenComplete((ignored, deletionFailure) -> {
                    if (deletionFailure != null) {
                        Platform.runLater(() -> message.setText("El nuevo adjunto se cargó, pero no se pudo retirar el anterior: "
                                + DocumentUi.cause(deletionFailure).getMessage()));
                    }
                });
            }
            message.setText("Adjunto cargado.");
            render();
        }));
    }

    private void deleteAttachment(ListView<WorksApiClient.Attachment> uploaded) {
        var selected = uploaded.getSelectionModel().getSelectedItem();
        if (selected == null || draft.work() == null) {
            return;
        }
        DocumentUi.run(uploaded, message, () -> api.deleteAttachment(draft.work().id(), selected.id()), ignored -> {
            draft.clearAttachment(selected);
            uploaded.getItems().remove(selected);
            message.setText("Adjunto eliminado.");
        });
    }

    private void save() {
        try {
            var information = form.information();
            setBusy(true, "Guardando información…");
            api.updateInformation(
                    draft.work().id(),
                    draft.information().etag(),
                    form.name.getText(),
                    form.location.getText(),
                    form.description.getText(),
                    information
            ).whenComplete((value, failure) -> Platform.runLater(() -> {
                setBusy(false, "");
                if (failure != null) {
                    message.setText(DocumentUi.cause(failure).getMessage());
                    return;
                }
                pageStatus.setText("Obra guardada.");
                saved.accept(value);
                dialog.close();
            }));
        } catch (RuntimeException failure) {
            message.setText("Revisa los datos: " + failure.getMessage());
        }
    }

    private void setBusy(boolean busy, String text) {
        back.setDisable(busy);
        next.setDisable(busy);
        finish.setDisable(busy);
        message.setText(text);
    }

    private static String fileName(Path file) {
        return file == null ? "Sin archivo seleccionado" : file.getFileName().toString();
    }

    private record InitialInput(ComboBox<ClientDto> client, TextField name, TextArea description) { }
}
