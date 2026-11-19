package com.brica.client.presentation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.brica.client.api.*;
import com.brica.client.config.ClientConfig;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class WorksController {
    private static final int PAGE_SIZE = 50;
    private final WorksApiClient api = new WorksApiClient(ClientConfig.serverBaseUri());
    private final ClientsApiClient clientsApi = new ClientsApiClient(ClientConfig.serverBaseUri());
    private final ObservableList<WorkDto> works = FXCollections.observableArrayList();
    private final ObservableList<ClientDto> clients = FXCollections.observableArrayList();
    private final Map<Integer, List<WorkDto>> cache = new HashMap<>();
    private CompletableFuture<List<DocumentDtos.Field>> fields;
    @FXML private TableView<WorkDto> worksTable;
    @FXML private TableColumn<WorkDto, String> nameColumn, clientColumn, descriptionColumn;
    @FXML private TableColumn<WorkDto, Void> actionsColumn;
    @FXML private Button newWorkButton, deleteWorkButton, refreshButton, previousPageButton, nextPageButton;
    @FXML private Label pageLabel, statusLabel;
    private int page;
    private long generation;
    private boolean busy, hasNext;

    @FXML private void initialize() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        clientColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(clientName(c.getValue().clientId())));
        descriptionColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().description()));
        worksTable.setItems(works); worksTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> buttons());
        worksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button edit = icon("edit", "Editar toda la información", () -> detail(getTableRow().getItem(), 0));
            private final Button capture = icon("capture", "Documentos, información y revisión", () -> detail(getTableRow().getItem(), 1));
            private final Button view = icon("view", "Ver información completa", () -> detail(getTableRow().getItem(), 2));
            private final Button documents = icon("documents", "Paquetes, plantillas y versiones", () -> {
                var value = getTableRow().getItem(); if (value != null) { ContextDocumentsWindow.show(worksTable.getScene().getWindow(), value.clientId(), value.id(), value.name()); }
            });
            private final Button extract = new Button("Extraer datos");
            { extract.setOnAction(e -> { var value=getTableRow().getItem(); if(value!=null) { com.brica.client.presentation.extraction.ExtractionWindow.show(worksTable.getScene().getWindow(),value.id(),value.name(),null); } }); }
            private final HBox box = new HBox(5, edit, capture, view, documents, extract);
            { box.setAlignment(javafx.geometry.Pos.CENTER); }
            @Override protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty); setGraphic(empty ? null : box);
                edit.setDisable(!can("WORKS_WRITE")); capture.setDisable(!can("WORKS_WRITE")); documents.setDisable(!can("DOCUMENT_CONFIG_ADMIN"));
                extract.setDisable(!can("WORKS_READ"));
            }
        });
        buttons(); load(0, false); loadClients(0);
    }
    private Button icon(String text, String tip, Runnable action) {
        var button = new Button(); button.setGraphic(WorkActionIcon.create(text)); button.setTooltip(new Tooltip(tip)); button.setAccessibleText(tip);
        button.getStyleClass().add("work-icon-button"); button.setOnAction(e -> action.run()); return button;
    }
    private boolean can(String permission) {
        var user = ServerSession.shared(ClientConfig.serverBaseUri()).user(); return user != null && user.can(permission);
    }
    private String clientName(Long id) { return clients.stream().filter(c -> c.id().equals(id)).map(ClientDto::name).findFirst().orElse("Cliente #" + id); }
    private void loadClients(int p) {
        if (!can("CLIENTS_READ")) { return; }
        clientsApi.list(p, 200).whenComplete((items, error) -> Platform.runLater(() -> {
            if (error != null) { statusLabel.setText(DocumentUi.cause(error).getMessage()); return; }
            if (p == 0) { clients.clear(); } clients.addAll(items); worksTable.refresh(); if (items.size() == 200) { loadClients(p + 1); }
        }));
    }
    private void load(int target, boolean force) {
        long epoch = ++generation; page = target;
        hasNext = cache.containsKey(target + 1) && !cache.get(target + 1).isEmpty();
        if (!force && cache.containsKey(target)) { display(cache.get(target)); }
        else { busy = true; buttons(); statusLabel.setText("Cargando obras…"); }
        api.list(target, PAGE_SIZE).whenComplete((items, error) -> Platform.runLater(() -> {
            if (epoch != generation) { return; }
            busy = false;
            if (error != null) { statusLabel.setText(DocumentUi.cause(error).getMessage()); buttons(); return; }
            cache.put(target, items); display(items);
        }));
        api.list(target + 1, PAGE_SIZE).whenComplete((items, error) -> Platform.runLater(() -> {
            if (epoch != generation || error != null) { return; }
            cache.put(target + 1, items); hasNext = !items.isEmpty(); buttons();
        }));
    }
    private void display(List<WorkDto> items) {
        works.setAll(items); busy = false; pageLabel.setText("Página " + (page + 1));
        statusLabel.setText(items.size() + " obra(s)"); buttons();
    }
    private void buttons() {
        newWorkButton.setDisable(!can("WORKS_WRITE")); deleteWorkButton.setDisable(busy || !can("WORKS_WRITE") || worksTable.getSelectionModel().getSelectedItem() == null);
        refreshButton.setDisable(busy); previousPageButton.setDisable(busy); nextPageButton.setDisable(busy);
        UiPagination.update(previousPageButton, nextPageButton, pageLabel, page, hasNext);
    }
    @FXML private void onRefresh() { fields = null; cache.clear(); load(page, true); loadClients(0); }
    @FXML private void onPreviousPage() { if (page > 0) { load(page - 1, false); } }
    @FXML private void onNextPage() { if (hasNext) { load(page + 1, false); } }
    @FXML private void onNewWork() {
        var dialog = new Dialog<ButtonType>(); dialog.initOwner(worksTable.getScene().getWindow()); dialog.setTitle("Nueva obra");
        var client = new ComboBox<ClientDto>(clients); client.setPromptText("Selecciona un cliente");
        client.setConverter(new javafx.util.StringConverter<>() {
            public String toString(ClientDto value) { return value == null ? "" : value.name(); }
            public ClientDto fromString(String value) { return null; }
        });
        var name = new TextField(); var description = new TextArea(); description.setPrefRowCount(3); var error = new Label(); error.setWrapText(true);
        NameInputRules.install(name);
        dialog.getDialogPane().setContent(new VBox(10, new Label("Cliente"), client, new Label("Nombre"), name, new Label("Descripción"), description, error));
        var save = new ButtonType("Crear obra", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            if (client.getValue() == null) { error.setText("Selecciona un cliente."); return; }
            try { NameInputRules.require(name.getText(), 255); } catch (IllegalArgumentException failure) { error.setText(failure.getMessage()); return; }
            long id = client.getValue().id(); String capturedName = name.getText(), capturedDescription = description.getText();
            AsyncDialogSave.run(dialog, statusLabel, error, () -> api.create(id, capturedName, null, capturedDescription, null, null), value -> {
                cache.clear(); works.add(value); worksTable.getSelectionModel().select(value); worksTable.scrollTo(value);
            });
        });
        DocumentUi.style(dialog); dialog.show();
    }
    private CompletableFuture<List<DocumentDtos.Field>> fields(int p, List<DocumentDtos.Field> result) {
        if (!can("DOCUMENT_CONFIG_ADMIN")) { return CompletableFuture.completedFuture(List.of()); }
        return new DocumentsApiClient(ClientConfig.serverBaseUri()).fields(p).thenCompose(items -> {
            result.addAll(items); return items.size() < 50 ? CompletableFuture.completedFuture(List.copyOf(result)) : fields(p + 1, result);
        });
    }
    private void detail(WorkDto work, int mode) {
        if (work == null) { return; }
        var loading = new Dialog<ButtonType>(); loading.initOwner(worksTable.getScene().getWindow()); loading.setTitle(work.name());
        loading.getDialogPane().setContent(new HBox(10, new ProgressIndicator(), new Label("Cargando información…")));
        loading.getDialogPane().getButtonTypes().add(ButtonType.CANCEL); DocumentUi.style(loading); loading.show();
        if (fields == null) { fields = fields(0, new ArrayList<>()); }
        api.getInformation(work.id()).thenCombine(fields, (versioned, definitions) -> {
            Platform.runLater(() -> {
                if (!loading.isShowing()) { return; } loading.close();
                if (mode == 1) { WorkCaptureDialog.show(worksTable.getScene().getWindow(), api, versioned, definitions, statusLabel, this::updated); }
                else { WorkDocumentInformationDialog.show(worksTable.getScene().getWindow(), versioned.information().work(), api,
                        new WorksApiClient.VersionedDocumentInformation(versioned.information().documentInformation(), versioned.etag()),
                        definitions, mode == 0 && can("WORKS_WRITE"), statusLabel, this::updated); }
            }); return null;
        }).exceptionally(error -> { Platform.runLater(() -> { loading.close(); statusLabel.setText(DocumentUi.cause(error).getMessage()); fields = null; }); return null; });
    }
    private void updated(WorksApiClient.CompleteInformation value) {
        cache.clear(); for (int i = 0; i < works.size(); i++) { if (works.get(i).id().equals(value.work().id())) { works.set(i, value.work()); return; } }
    }
    @FXML private void onDeleteWork() {
        var value = worksTable.getSelectionModel().getSelectedItem(); if (value == null) { return; }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar la obra «" + value.name() + "»? Se conservará su historial.", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(worksTable.getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) { return; }
        statusLabel.setText("Eliminando…"); busy = true; buttons();
        api.delete(value.id()).whenComplete((ignored, error) -> Platform.runLater(() -> {
            busy = false; buttons();
            if (error != null) { statusLabel.setText(DocumentUi.cause(error).getMessage()); return; }
            works.remove(value); cache.clear(); statusLabel.setText("Obra eliminada."); if (works.isEmpty() && page > 0) { load(page - 1, true); }
        }));
    }
}
