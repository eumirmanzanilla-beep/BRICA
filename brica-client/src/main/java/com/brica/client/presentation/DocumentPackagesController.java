package com.brica.client.presentation;

import java.util.*;
import com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentDtos.Package;
import com.brica.client.api.DocumentsApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class DocumentPackagesController {
    @FXML private VBox pane;
    @FXML private ListView<Package> packages;
    @FXML private TextField name;
    @FXML private TextField clientId;
    @FXML private VBox templateChoices;
    @FXML private Label message;
    @FXML private Label state;
    private DocumentsApiClient api;
    private Package editing;
    private final Set<Long> selected = new LinkedHashSet<>();
    private final Map<Long, Template> known = new LinkedHashMap<>();
    private boolean dirty;
    private boolean loading;
    private int templatePage;
    private int page;
    private Runnable registerTemplate = () -> { };
    @FXML private void initialize() {
        name.textProperty().addListener((o, old, value) -> { if (!loading) { dirty = true; } });
        clientId.textProperty().addListener((o, old, value) -> { if (!loading) { dirty = true; } });
    }
    void configure(DocumentsApiClient api, Runnable registerTemplate) {
        this.api = api; this.registerTemplate = registerTemplate; refresh();
    }
    @FXML private void refresh() {
        DocumentUi.run(pane, message, () -> api.packages(page), values -> { packages.getItems().setAll(values); loadTemplates(); });
    }
    @FXML private void previous() { if (page > 0) { page--; refresh(); } }
    @FXML private void next() { if (packages.getItems().size() == 50) { page++; refresh(); } }
    @FXML private void open() {
        var chosen = packages.getSelectionModel().getSelectedItem();
        if (chosen == null) { return; }
        leave(() -> DocumentUi.run(pane, message, () -> api.documentPackage(chosen.id()), this::display));
    }
    @FXML private void newPackage() { leave(() -> display(null)); }
    private void display(Package value) {
        loading = true; editing = value;
        name.setText(value == null || value.name() == null ? "" : value.name());
        clientId.setText(value == null || value.clientId() == null ? "" : value.clientId().toString());
        clientId.setDisable(value != null && "CONFIRMED".equals(value.state()));
        selected.clear();
        if (value != null) {
            value.templates().forEach(t -> {
                selected.add(t.id());
                known.putIfAbsent(t.id(), new Template(t.id(), t.name(), "", t.state(), t.deleted(), null));
            });
        }
        state.setText(value == null ? "Borrador nuevo" : value.state() + " · revisión " + value.revision());
        renderTemplates(); loading = false; dirty = false;
    }
    void loadTemplates() {
        if (api == null) { return; }
        DocumentUi.run(pane, message, () -> api.templates(templatePage), values -> {
            values.forEach(t -> known.put(t.id(), t)); renderTemplates();
        });
    }
    @FXML private void moreTemplates() { templatePage++; loadTemplates(); }
    private void renderTemplates() {
        templateChoices.getChildren().clear();
        known.values().stream().sorted(Comparator.comparing(Template::name).thenComparing(Template::id)).forEach(t -> {
            if (t.deleted() && !selected.contains(t.id())) { return; }
            var box = new CheckBox(t.toString()); box.setWrapText(true); box.setSelected(selected.contains(t.id()));
            box.selectedProperty().addListener((o, old, checked) -> { if (checked) { selected.add(t.id()); } else { selected.remove(t.id()); } dirty = true; });
            templateChoices.getChildren().add(box);
        });
    }
    @FXML private void saveDraft() { save(false, () -> { }); }
    @FXML private void confirm() { save(true, () -> { }); }
    private void save(boolean confirm, Runnable after) {
        DocumentUi.run(pane, message, () -> api.savePackage(editing, DocumentUi.optionalId(clientId.getText()), name.getText(), List.copyOf(selected), confirm), value -> {
            display(value);
            // Opening another package may start its own request. Do not suppress it with a list refresh.
            after.run();
            if (!pane.isDisable()) { refresh(); }
        });
    }
    @FXML private void discard() {
        if (editing == null) { display(null); return; }
        if (!"DRAFT".equals(editing.state())) { message.setText("Un paquete confirmado no se descarta como borrador."); return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Descartar el borrador guardado? No se eliminarán plantillas ni versiones.", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) { return; }
        DocumentUi.run(pane, message, () -> api.discard(editing), value -> { display(null); refresh(); });
    }
    @FXML private void registerTemplate() { registerTemplate.run(); }
    void leave(Runnable continuation) {
        if (pane.isDisable()) { message.setText("Espera a que termine la operación."); return; }
        if (!dirty) { continuation.run(); return; }
        var save = new ButtonType("Guardar cambios"); var discard = new ButtonType("Salir sin guardar");
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Hay cambios del paquete sin guardar. Un paquete nuevo se conservará como borrador.", save, discard, ButtonType.CANCEL);
        var choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == save) { save(false, continuation); }
        else if (choice == discard) { display(editing); continuation.run(); }
    }
}
