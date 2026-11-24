package com.brica.client.documents.presentation;

import com.brica.client.shared.presentation.NameInputRules;
import java.util.*;
import com.brica.client.documents.api.DocumentDtos.*;
import com.brica.client.documents.api.DocumentDtos.Package;
import com.brica.client.documents.api.DocumentsApiClient;
import com.brica.client.clients.api.ClientDto;
import com.brica.client.works.api.WorkDto;
import com.brica.client.clients.api.ClientsApiClient;
import com.brica.client.works.api.WorksApiClient;
import com.brica.client.config.ClientConfig;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class DocumentPackagesController {
    @FXML private VBox pane;
    @FXML private VBox form;
    @FXML private Label dialogMessage;
    @FXML private ListView<Package> packages;
    @FXML private TextField name;
    @FXML private ComboBox<ClientDto> clientChoice;
    @FXML private ComboBox<WorkDto> workChoice;
    @FXML private javafx.scene.layout.HBox clientSelector;
    @FXML private javafx.scene.layout.HBox workSelector;
    @FXML private Label clientContext;
    @FXML private VBox templateChoices;
    @FXML private Label message;
    @FXML private Label state;
    private DocumentsApiClient api;
    private final ClientsApiClient clients = new ClientsApiClient(ClientConfig.serverBaseUri());
    private final WorksApiClient works = new WorksApiClient(ClientConfig.serverBaseUri());
    private Long selectedClientId;
    private Long selectedWorkId;
    private int clientPage;
    private int workPage;
    private Package editing;
    private final Set<Long> selected = new LinkedHashSet<>();
    private final Map<Long, Template> known = new LinkedHashMap<>();
    private boolean dirty;
    private boolean loading;
    private int templatePage;
    private int page;
    private Runnable registerTemplate = () -> { };
    private Dialog<ButtonType> dialog;
    private boolean allowClose;
    private boolean hasCapture;
    @FXML private void initialize() {
        NameInputRules.install(name);
        pane.getChildren().remove(form);
        form.disableProperty().addListener((observable, old, busy) -> pane.setDisable(busy));
        dialogMessage.textProperty().bind(message.textProperty());
        name.textProperty().addListener((o, old, value) -> { if (!loading) { dirty = true; } });
        clientChoice.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClientDto value) { return value == null ? "" : value.name() + " · #" + value.id(); }
            @Override public ClientDto fromString(String value) { return null; }
        });
        workChoice.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(WorkDto value) { return value == null ? "" : value.name() + " · #" + value.id(); }
            @Override public WorkDto fromString(String value) { return null; }
        });
        clientChoice.valueProperty().addListener((o, old, value) -> {
            if (!loading && value != null) {
                loading = true; workChoice.setValue(null); loading = false;
                selectedWorkId = null; selectedClientId = value.id(); dirty = true; clientContext.setText("Cliente: " + value.name());
            }
        });
        workChoice.valueProperty().addListener((o, old, value) -> {
            if (!loading && value != null) {
                loading = true; clientChoice.setValue(null); loading = false;
                selectedWorkId = value.id(); selectedClientId = value.clientId(); dirty = true;
                clientContext.setText("Obra: " + value.name() + " · cliente asociado #" + value.clientId());
            }
        });
    }
    @FXML private void loadClients() { DocumentUi.run(form, message, () -> clients.list(clientPage, 50), items -> clientChoice.getItems().setAll(items)); }
    @FXML private void previousClients() { if (clientPage > 0) { clientPage--; loadClients(); } }
    @FXML private void nextClients() { if (clientChoice.getItems().size() == 50) { clientPage++; loadClients(); } }
    @FXML private void loadWorks() { DocumentUi.run(form, message, () -> works.list(workPage, 50), items -> workChoice.getItems().setAll(items)); }
    @FXML private void previousWorks() { if (workPage > 0) { workPage--; loadWorks(); } }
    @FXML private void nextWorks() { if (workChoice.getItems().size() == 50) { workPage++; loadWorks(); } }
    void configure(DocumentsApiClient api, Runnable registerTemplate) {
        this.api = api; this.registerTemplate = registerTemplate; refresh();
    }
    void selectorPermissions(boolean clientsRead, boolean worksRead) {
        clientSelector.setDisable(!clientsRead); workSelector.setDisable(!worksRead);
        clientChoice.setPromptText(clientsRead ? "Selecciona cliente" : "Requiere CLIENTS_READ");
        workChoice.setPromptText(worksRead ? "Selecciona obra" : "Requiere WORKS_READ");
    }
    @FXML private void refresh() {
        DocumentUi.run(form, message, () -> api.packages(page), values -> { packages.getItems().setAll(values); loadTemplates(); });
    }
    @FXML private void previous() { if (page > 0) { page--; refresh(); } }
    @FXML private void next() { if (packages.getItems().size() == 50) { page++; refresh(); } }
    @FXML private void open() {
        var chosen = packages.getSelectionModel().getSelectedItem();
        if (chosen == null) { return; }
        leave(() -> DocumentUi.run(form, message, () -> api.documentPackage(chosen.id()), value -> { display(value); resume(); }));
    }
    @FXML private void newPackage() { leave(() -> { display(null); resume(); }); }
    @FXML private void resume() {
        if (!hasCapture) { display(null); }
        if (dialog == null) {
            dialog = new Dialog<>(); dialog.initOwner(pane.getScene().getWindow());
            dialog.setTitle("Configurar paquete documental");
            var scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(540);
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
            dialog.setResizable(true);
            dialog.setOnCloseRequest(event -> {
                if (allowClose) { return; }
                if (form.isDisable()) { event.consume(); return; }
                if (dirty) { event.consume(); leave(this::hideDialog); }
            });
        }
        dialog.show();
    }
    private void hideDialog() {
        if (dialog == null) { return; }
        allowClose = true;
        try { dialog.close(); } finally { allowClose = false; }
    }
    private void display(Package value) {
        loading = true; editing = value; hasCapture = true;
        name.setText(value == null || value.name() == null ? "" : value.name());
        selectedClientId = value == null ? null : value.clientId();
        selectedWorkId = value == null ? null : value.workId();
        clientChoice.setValue(null); workChoice.setValue(null);
        clientContext.setText(selectedClientId == null ? "Selecciona un cliente o una obra." : "Cliente del paquete · #" + selectedClientId);
        clientChoice.setDisable(value != null && "CONFIRMED".equals(value.state()));
        workChoice.setDisable(value != null && "CONFIRMED".equals(value.state()));
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
        DocumentUi.run(form, message, () -> api.templates(templatePage), values -> {
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
        try { NameInputRules.require(name.getText(), 150); }
        catch (IllegalArgumentException failure) { message.setText(failure.getMessage()); return; }
        DocumentUi.run(form, message, () -> api.savePackage(editing, selectedWorkId == null ? selectedClientId : null,
                selectedWorkId, name.getText(), List.copyOf(selected), confirm), value -> {
            display(value);
            // Opening another package may start its own request. Do not suppress it with a list refresh.
            after.run();
            if (!form.isDisable()) { refresh(); }
        });
    }
    @FXML private void discard() {
        if (editing == null) { display(null); return; }
        if (!"DRAFT".equals(editing.state())) { message.setText("Un paquete confirmado no se descarta como borrador."); return; }
        var alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Descartar el borrador guardado? No se eliminarán plantillas ni versiones.", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) { return; }
        DocumentUi.run(form, message, () -> api.discard(editing), value -> { display(null); refresh(); });
    }
    @FXML private void registerTemplate() { hideDialog(); registerTemplate.run(); }
    void leave(Runnable continuation) {
        if (form.isDisable()) { message.setText("Espera a que termine la operación."); return; }
        if (!dirty) { continuation.run(); return; }
        var save = new ButtonType("Guardar cambios"); var discard = new ButtonType("Salir sin guardar");
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Hay cambios del paquete sin guardar. Un paquete nuevo se conservará como borrador.", save, discard, ButtonType.CANCEL);
        var choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == save) { save(false, continuation); }
        else if (choice == discard) { display(editing); continuation.run(); }
    }
}
