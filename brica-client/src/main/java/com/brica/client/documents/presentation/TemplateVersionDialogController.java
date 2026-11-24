package com.brica.client.documents.presentation;

import com.brica.client.shared.presentation.NameInputRules;
import java.nio.file.Path;
import java.util.*;
import com.brica.client.documents.api.DocumentDtos.*;
import com.brica.client.documents.api.DocumentsApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TemplateVersionDialogController {
    @FXML private VBox pane;
    @FXML private Label filename;
    @FXML private Label message;
    @FXML private TextArea diagnostics;
    @FXML private VBox classifications;
    @FXML private CheckBox staticAccepted;
    @FXML private TextField confirmation;
    @FXML private TextField versionName;
    @FXML private Button confirmButton;
    private DocumentsApiClient api;
    private long templateId;
    private Stage stage;
    private Runnable completed;
    private Path file;
    private Review review;
    private UUID confirmationId;
    private Map<Long, Boolean> lastRequired;
    private boolean lastStatic;
    private String lastName;
    private boolean busy;
    private final Map<Long, ToggleGroup> choices = new LinkedHashMap<>();

    void configure(DocumentsApiClient api, long templateId, Stage stage, Runnable completed) {
        this.api = api; this.templateId = templateId; this.stage = stage; this.completed = completed;
        confirmButton.setDisable(true);
        NameInputRules.install(versionName);
        stage.setOnCloseRequest(event -> { if (busy) { event.consume(); message.setText("Espera el resultado antes de cerrar."); } });
    }
    @FXML private void choose() {
        var chooser = new FileChooser(); chooser.setTitle("Seleccionar plantilla DOCX");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plantilla DOCX", "*.docx"));
        var selected = chooser.showOpenDialog(stage);
        if (selected == null) { return; }
        file = selected.toPath(); filename.setText(selected.getName()); versionName.setText(suggestedVersionName(selected.getName()));
        review = null; confirmationId = null;
        confirmButton.setDisable(true);
        run(() -> api.analyze(templateId, file), this::showReview);
    }
    private void showReview(Review value) {
        review = value; confirmationId = null; confirmation.clear(); choices.clear(); classifications.getChildren().clear();
        var text = new StringBuilder();
        value.diagnostics().forEach(d -> text.append(d.location()).append(" · ").append(d.message()).append('\n'));
        value.occurrences().forEach(o -> text.append(o.marker()).append(" · ").append(o.location()).append('\n'));
        diagnostics.setText(text.toString());
        value.fields().forEach(field -> {
            var group = new ToggleGroup();
            var required = new RadioButton("Obligatorio"); required.setToggleGroup(group); required.setUserData(true);
            var optional = new RadioButton("Opcional"); optional.setToggleGroup(group); optional.setUserData(false);
            if (field.proposedRequired() != null) { (field.proposedRequired() ? required : optional).setSelected(true); }
            choices.put(field.id(), group);
            var label = new Label(field.key() + " · " + field.visibleName() + " · " + field.origin()); label.setWrapText(true);
            classifications.getChildren().add(new VBox(5, label, new HBox(12, required, optional)));
        });
        staticAccepted.setVisible(value.staticDocument()); staticAccepted.setManaged(value.staticDocument()); staticAccepted.setSelected(false);
        confirmButton.setDisable(!value.diagnostics().isEmpty());
        message.setText(value.staticDocument() ? "El archivo no tiene marcadores. Confirma expresamente si es estático." : "Revisa cada campo antes de confirmar.");
    }
    @FXML private void confirm() {
        if (review == null) { return; }
        try { NameInputRules.require(versionName.getText(), 150); }
        catch (IllegalArgumentException failure) { message.setText(failure.getMessage()); return; }
        var required = new TreeMap<Long, Boolean>();
        for (var entry : choices.entrySet()) {
            if (entry.getValue().getSelectedToggle() == null) { message.setText("Clasifica todos los campos."); return; }
            required.put(entry.getKey(), (Boolean) entry.getValue().getSelectedToggle().getUserData());
        }
        if (review.staticDocument() && !staticAccepted.isSelected()) { message.setText("Confirma expresamente el documento estático."); return; }
        if (confirmationId == null || !required.equals(lastRequired) || lastStatic != staticAccepted.isSelected() || !versionName.getText().equals(lastName)) {
            confirmationId = UUID.randomUUID(); lastRequired = Map.copyOf(required); lastStatic = staticAccepted.isSelected();
            lastName = versionName.getText();
        }
        confirmation.setText(confirmationId.toString());
        stage.hide();
        run(() -> api.confirm(templateId, file, confirmationId, review, required, staticAccepted.isSelected(), lastName), this::showOperation);
    }
    @FXML private void status() {
        if (confirmationId != null) { run(() -> api.registration(confirmationId.toString(), false), this::showOperation); }
    }
    @FXML private void recover() {
        if (confirmationId != null) { run(() -> api.registration(confirmationId.toString(), true), this::showOperation); }
    }
    @FXML private void newConfirmation() { confirmationId = null; confirmation.clear(); message.setText("La próxima confirmación será una operación nueva."); }
    private void showOperation(Operation operation) {
        message.setText(operation.status() + (operation.failure() == null ? "" : " · " + operation.failure()));
        if ("COMPLETED".equals(operation.status())) { completed.run(); stage.close(); }
        else { stage.show(); }
    }
    private <T> void run(java.util.function.Supplier<java.util.concurrent.CompletableFuture<T>> action, java.util.function.Consumer<T> success) {
        if (busy) { return; } busy = true; pane.setDisable(true); message.setText("Procesando…");
        java.util.concurrent.CompletableFuture<T> future;
        try { future = action.get(); } catch (Exception failure) { future = java.util.concurrent.CompletableFuture.failedFuture(failure); }
        future.whenComplete((value, error) -> Platform.runLater(() -> {
            busy = false; pane.setDisable(false);
            if (error == null) { success.accept(value); }
            else {
                var cause = DocumentUi.cause(error);
                if (cause instanceof DocumentsApiClient.DocumentApiException apiError && apiError.updatedAnalysis() != null) { showReview(apiError.updatedAnalysis()); }
                message.setText(cause.getMessage());
                if (!stage.isShowing()) { stage.show(); }
            }
        }));
    }
    static String suggestedVersionName(String filename) {
        if (filename == null || filename.isBlank()) { return ""; }
        return filename.replaceFirst("(?i)\\.docx$", "");
    }
}
