
package com.brica.client.clients.presentation;

import com.brica.client.core.ui.UiPagination;
import java.net.ConnectException;
import java.net.URL;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.brica.client.clients.api.ClientDto;
import com.brica.client.clients.api.ClientsApiClient;
import com.brica.client.config.ClientConfig;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public final class ClientsController {

    private static final int PAGE_SIZE = 50;

    private static final String CSS_PATH =
            "/com/brica/client/styles/app.css";

    private final ClientsApiClient api =
            new ClientsApiClient(ClientConfig.serverBaseUri());

    private final ObservableList<ClientDto> clients =
            FXCollections.observableArrayList();

    @FXML
    private TableView<ClientDto> clientsTable;

    @FXML
    private TableColumn<ClientDto, Long> idColumn;

    @FXML
    private TableColumn<ClientDto, String> nameColumn;

    @FXML
    private TableColumn<ClientDto, String> representativeColumn;

    @FXML
    private Button newClientButton;

    @FXML
    private Button editClientButton;

    @FXML
    private Button deleteClientButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button previousPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private Label pageLabel;

    @FXML
    private Label statusLabel;

    private int currentPage = 0;
    private boolean hasNext = false;
    private boolean busy = false;

    // INICIALIZACIÓN DE LA PANTALLA

    @FXML
    private void initialize() {

        idColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(
                        cell.getValue().id()
                )
        );

        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().name()
                )
        );

        representativeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().legalRepresentative()
                )
        );

        clientsTable.setItems(clients);

        clientsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        updateButtons()
                );

        updateButtons();
        loadPage(0, null);
    }

    // LISTADO Y PAGINACIÓN

    private void loadPage(int page, String successMessage) {

        setBusy(true);
        statusLabel.setText("Cargando clientes...");

        api.list(page, PAGE_SIZE)
                .whenComplete((result, error) ->
                        Platform.runLater(() -> {

                            if (error != null) {

                                setBusy(false);

                                statusLabel.setText(
                                        "No se pudieron cargar "
                                                + "los clientes: "
                                                + errorMessage(error)
                                );

                                return;
                            }

                            currentPage = page;
                            hasNext = false;
                            api.list(page + 1, PAGE_SIZE).whenComplete((following, failure) -> Platform.runLater(() -> {
                                if (failure == null && currentPage == page) { hasNext = !following.isEmpty(); updateButtons(); }
                            }));

                            clients.setAll(result);

                            pageLabel.setText(
                                    "Página " + (currentPage + 1)
                            );

                            String summary =
                                    "Mostrando " + result.size()
                                            + " cliente(s) en esta página.";

                            if (successMessage == null) {
                                statusLabel.setText(summary);
                            } else {
                                statusLabel.setText(
                                        successMessage + " " + summary
                                );
                            }

                            setBusy(false);
                        })
                );
    }

    @FXML
    private void onRefresh() {
        loadPage(currentPage, null);
    }

    @FXML
    private void onPreviousPage() {

        if (!busy && currentPage > 0) {
            loadPage(currentPage - 1, null);
        }
    }

    @FXML
    private void onNextPage() {

        if (!busy && hasNext) {
            loadPage(currentPage + 1, null);
        }
    }

    // REGISTRAR CLIENTE

    @FXML
    private void onNewClient() {

        if (busy) {
            return;
        }

        showClientDialog(null).ifPresent(input ->

                executeMutation(
                        api.create(
                                input.name(),
                                input.legalRepresentative()
                        ),
                        "Cliente registrado correctamente.",
                        0
                )
        );
    }

    // EDITAR CLIENTE

    @FXML
    private void onEditClient() {

        ClientDto selected = clientsTable
                .getSelectionModel()
                .getSelectedItem();

        if (busy || selected == null) {
            return;
        }

        showClientDialog(selected).ifPresent(input ->

                executeMutation(
                        api.update(
                                selected.id(),
                                selected.revision(),
                                input.name(),
                                input.legalRepresentative()
                        ),
                        "Cliente actualizado correctamente.",
                        currentPage
                )
        );
    }

    // ELIMINAR CLIENTE

    @FXML
    private void onDeleteClient() {

        ClientDto selected = clientsTable
                .getSelectionModel()
                .getSelectedItem();

        if (busy || selected == null) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Eliminar cliente");

        confirmation.setHeaderText(
                "¿Eliminar a " + selected.name() + "?"
        );

        confirmation.setContentText(
                "El cliente dejará de aparecer en los "
                        + "listados activos. La eliminación "
                        + "es lógica y conserva el historial."
        );

        styleDialog(confirmation);

        boolean confirmed = confirmation.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .isPresent();

        if (!confirmed) {
            return;
        }

        int targetPage =
                clients.size() == 1 && currentPage > 0
                        ? currentPage - 1
                        : currentPage;

        executeMutation(
                api.delete(selected.id(), selected.revision()),
                "Cliente eliminado correctamente.",
                targetPage
        );
    }

    // OPERACIONES ASÍNCRONAS DE ESCRITURA

    private void executeMutation(
            CompletableFuture<?> operation,
            String successMessage,
            int targetPage) {

        setBusy(true);
        statusLabel.setText("Guardando cambios...");

        operation.whenComplete((result, error) ->
                Platform.runLater(() -> {

                    if (error != null) {

                        setBusy(false);

                        String message = errorMessage(error);

                        statusLabel.setText(
                                "No se pudo completar la operación."
                        );

                        showError(message);

                        return;
                    }

                    loadPage(targetPage, successMessage);
                })
        );
    }

    // FORMULARIO DE ALTA Y EDICIÓN

    private java.util.Optional<ClientInput> showClientDialog(
            ClientDto selected) {

        boolean editing = selected != null;

        Dialog<ClientInput> dialog = new Dialog<>();

        dialog.setTitle(
                editing ? "Editar cliente" : "Nuevo cliente"
        );

        dialog.setHeaderText(
                editing
                        ? "Modificar información del cliente"
                        : "Registrar nuevo cliente"
        );

        TextField nameField = new TextField();
        nameField.setPrefColumnCount(28);
        nameField.getStyleClass().add("form-field");

        TextField representativeField = new TextField();
        representativeField.setPromptText(
                "Nombre del representante legal"
        );
        representativeField.setPrefColumnCount(28);
        representativeField.getStyleClass().add("form-field");

        if (editing) {
            nameField.setText(selected.name());

            representativeField.setText(
                    selected.legalRepresentative()
            );
        }
        ClientNameInputRules.install(nameField);

        Label nameLabel = new Label("Cliente");
        nameLabel.getStyleClass().add("form-label");

        Label representativeLabel =
                new Label("Representante legal");
        representativeLabel.getStyleClass().add("form-label");

        Label helpLabel = new Label(
                "Ambos campos son obligatorios. "
                        + "Máximo 255 caracteres por campo."
        );

        helpLabel.setWrapText(true);
        helpLabel.getStyleClass().add("content-note");

        GridPane form = new GridPane();

        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(12));

        form.add(nameLabel, 0, 0);
        form.add(nameField, 1, 0);

        form.add(representativeLabel, 0, 1);
        form.add(representativeField, 1, 1);

        form.add(helpLabel, 0, 2, 2, 1);

        GridPane.setHgrow(
                nameField,
                Priority.ALWAYS
        );

        GridPane.setHgrow(
                representativeField,
                Priority.ALWAYS
        );

        dialog.getDialogPane().setContent(form);

        ButtonType saveType = new ButtonType(
                editing ? "Guardar cambios" : "Registrar",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                saveType,
                ButtonType.CANCEL
        );

        styleDialog(dialog);

        Node saveButton = dialog.getDialogPane()
                .lookupButton(saveType);

        saveButton.getStyleClass().add("primary-button");

        saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !ClientNameInputRules.valid(nameField.getText(), 255)
                                || !validText(
                                        representativeField.getText()
                                ),
                        nameField.textProperty(),
                        representativeField.textProperty()
                )
        );

        dialog.setResultConverter(button -> {

            if (button != saveType) {
                return null;
            }

            return new ClientInput(
                    nameField.getText(),
                    representativeField.getText().trim()
            );
        });

        return dialog.showAndWait();
    }

    // VALIDACIONES

    private boolean validText(String value) {
        return value != null
                && !value.isBlank()
                && value.trim().length() <= 255;
    }

    // ESTADO DE LOS BOTONES

    private void setBusy(boolean value) {
        busy = value;

        clientsTable.setDisable(value);

        updateButtons();
    }

    private void updateButtons() {

        var user = com.brica.client.core.http.ServerSession.shared(ClientConfig.serverBaseUri()).user();
        boolean canWrite = user != null && user.can("CLIENTS_WRITE");

        boolean selected = clientsTable
                .getSelectionModel()
                .getSelectedItem() != null;

        newClientButton.setDisable(busy || !canWrite);
        refreshButton.setDisable(busy);
        UiPagination.update(previousPageButton, nextPageButton, pageLabel, currentPage, hasNext);

        editClientButton.setDisable(
                busy || !selected || !canWrite
        );

        deleteClientButton.setDisable(
                busy || !selected || !canWrite
        );

        previousPageButton.setDisable(
                busy || currentPage == 0
        );

        nextPageButton.setDisable(
                busy || !hasNext
        );
    }
    // MENSAJES DE ERROR

    private void showError(String message) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle("Error en Clientes");
        alert.setHeaderText(
                "No se pudo completar la operación"
        );
        alert.setContentText(message);

        styleDialog(alert);
        alert.showAndWait();
    }

    private String errorMessage(Throwable error) {

        Throwable cause = error;

        while (cause instanceof CompletionException
                && cause.getCause() != null) {

            cause = cause.getCause();
        }

        if (cause instanceof ConnectException
                || cause instanceof HttpTimeoutException) {

            return "No se pudo establecer conexión "
                    + "con el servidor BRICA.";
        }

        String message = cause.getMessage();

        return message == null || message.isBlank()
                ? "Ocurrió un error inesperado."
                : message;
    }

    // ESTILOS DE LOS CUADROS DE DIÁLOGO

    private void styleDialog(Dialog<?> dialog) {

        dialog.initOwner(
                clientsTable.getScene().getWindow()
        );

        URL stylesheet = getClass().getResource(
                CSS_PATH
        );

        if (stylesheet != null) {

            dialog.getDialogPane()
                    .getStylesheets()
                    .add(stylesheet.toExternalForm());
        }

        dialog.getDialogPane()
                .getStyleClass()
                .add("client-dialog");
    }

    private record ClientInput(
            String name,
            String legalRepresentative
    ) {
    }
}
