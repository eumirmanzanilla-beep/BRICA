package com.brica.client.works.presentation;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.brica.client.api.ClientDto;
import com.brica.client.api.ClientsApiClient;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient;
import com.brica.client.config.ClientConfig;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class WorksController {

    private static final int PAGE_SIZE = 50;
    private static final int CLIENT_PAGE_SIZE = 200;

    private static final String CSS_PATH =
            "/com/brica/client/presentation/styles/app.css";

    private final WorksApiClient worksApi =
            new WorksApiClient(ClientConfig.serverBaseUri());

    private final ClientsApiClient clientsApi =
            new ClientsApiClient(ClientConfig.serverBaseUri());

    private final ObservableList<WorkDto> works =
            FXCollections.observableArrayList();

    private final Map<Long, String> clientNames =
            new HashMap<>();

    @FXML
    private TableView<WorkDto> worksTable;

    @FXML
    private TableColumn<WorkDto, Long> idColumn;

    @FXML
    private TableColumn<WorkDto, String> nameColumn;

    @FXML
    private TableColumn<WorkDto, String> clientColumn;

    @FXML
    private TableColumn<WorkDto, String> locationColumn;

    @FXML
    private TableColumn<WorkDto, String> startDateColumn;

    @FXML
    private TableColumn<WorkDto, String> endDateColumn;

    @FXML
    private Button newWorkButton;

    @FXML
    private Button editWorkButton;

    @FXML
    private Button deleteWorkButton;

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

    // INICIALIZACIÓN

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

        clientColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        clientDisplay(
                                cell.getValue().clientId()
                        )
                )
        );

        locationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().location()
                )
        );

        startDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        dateText(cell.getValue().startDate())
                )
        );

        endDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        dateText(cell.getValue().endDate())
                )
        );

        worksTable.setItems(works);

        worksTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        updateButtons()
                );

        updateButtons();
        loadPage(0, null);
        refreshClientNames();
    }

    // PRESENTACIÓN DEL CLIENTE

    private String clientDisplay(Long clientId) {

        String name = clientNames.get(clientId);

        if (name == null) {
            return "Cliente #" + clientId;
        }

        return name + " (#" + clientId + ")";
    }

    private String dateText(String date) {
        return date == null || date.isBlank()
                ? "—"
                : date;
    }

    // LISTADO DE OBRAS

    private void loadPage(
            int page,
            String successMessage) {

        setBusy(true);
        statusLabel.setText("Cargando obras...");

        worksApi.list(page, PAGE_SIZE)
                .whenComplete((result, error) ->
                        Platform.runLater(() -> {

                            if (error != null) {

                                setBusy(false);

                                statusLabel.setText(
                                        "No se pudieron cargar "
                                                + "las obras: "
                                                + errorMessage(error)
                                );

                                return;
                            }

                            currentPage = page;
                            hasNext = result.size() == PAGE_SIZE;

                            worksTable.getSelectionModel()
                                    .clearSelection();

                            works.setAll(result);

                            pageLabel.setText(
                                    "Página " + (page + 1)
                            );

                            String summary =
                                    "Mostrando " + result.size()
                                            + " obra(s) en esta página.";

                            statusLabel.setText(
                                    successMessage == null
                                            ? summary
                                            : successMessage + " " + summary
                            );

                            setBusy(false);
                        })
                );
    }

    @FXML
    private void onRefresh() {

        if (busy) {
            return;
        }

        refreshClientNames();
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

    // CATÁLOGO COMPLETO DE CLIENTES ACTIVOS

    private CompletableFuture<List<ClientDto>> loadAllClients(
            int page,
            List<ClientDto> accumulated) {

        return clientsApi.list(
                page,
                CLIENT_PAGE_SIZE
        ).thenCompose(batch -> {

            accumulated.addAll(batch);

            if (batch.size() < CLIENT_PAGE_SIZE) {
                return CompletableFuture.completedFuture(
                        accumulated
                );
            }

            return loadAllClients(
                    page + 1,
                    accumulated
            );
        });
    }

    private void refreshClientNames() {

        loadAllClients(
                0,
                new ArrayList<>()
        ).whenComplete((result, error) ->
                Platform.runLater(() -> {

                    if (error != null) {
                        return;
                    }

                    clientNames.clear();

                    for (ClientDto client : result) {
                        clientNames.put(
                                client.id(),
                                client.name()
                        );
                    }

                    worksTable.refresh();
                })
        );
    }

    // REGISTRAR OBRA

    @FXML
    private void onNewWork() {

        if (busy) {
            return;
        }

        setBusy(true);
        statusLabel.setText(
                "Consultando clientes disponibles..."
        );

        loadAllClients(
                0,
                new ArrayList<>()
        ).whenComplete((result, error) ->
                Platform.runLater(() -> {

                    setBusy(false);

                    if (error != null) {
                        statusLabel.setText(
                                "No se pudieron consultar los clientes."
                        );
                        showError(errorMessage(error));
                        return;
                    }

                    if (result.isEmpty()) {
                        showInfo(
                                "No hay clientes activos.",
                                "Registra un cliente antes "
                                        + "de registrar una obra."
                        );
                        return;
                    }

                    result.sort(
                            Comparator.comparing(
                                    ClientDto::name,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                    );

                    WorkFormDialog.show(
                                worksTable.getScene().getWindow(),
                                null,
                                result,
                                this::clientDisplay
                        ).ifPresent(input ->

                            executeMutation(
                                    worksApi.create(
                                            input.clientId(),
                                            input.name(),
                                            input.location(),
                                            input.description(),
                                            input.startDate(),
                                            input.endDate()
                                    ),
                                    "Obra registrada correctamente.",
                                    0
                            )
                    );
                })
        );
    }

    // CORREGIR INFORMACIÓN GENERAL

    @FXML
    private void onEditWork() {

        WorkDto selected = worksTable
                .getSelectionModel()
                .getSelectedItem();

        if (busy || selected == null) {
            return;
        }

        setBusy(true);
        statusLabel.setText(
                "Consultando la versión actual de la obra..."
        );

        worksApi.get(selected.id())
                .whenComplete((versioned, error) ->
                        Platform.runLater(() -> {

                            setBusy(false);

                            if (error != null) {

                                statusLabel.setText(
                                        "No se pudo consultar la obra."
                                );

                                showError(errorMessage(error));
                                return;
                            }

                            WorkFormDialog.show(
                                        worksTable.getScene().getWindow(),
                                        versioned.work(),
                                        List.of(),
                                        this::clientDisplay
                                ).ifPresent(input ->

                                    executeMutation(
                                            worksApi
                                                    .updateGeneralInformation(
                                                            versioned.work().id(),
                                                            versioned.etag(),
                                                            input.name(),
                                                            input.location(),
                                                            input.description()
                                                    ),
                                            "Información corregida correctamente.",
                                            currentPage
                                    )
                            );
                        })
                );
    }

    // ELIMINAR OBRA

    @FXML
    private void onDeleteWork() {

        WorkDto selected = worksTable
                .getSelectionModel()
                .getSelectedItem();

        if (busy || selected == null) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmation.setTitle("Eliminar obra");
        confirmation.setHeaderText(
                "¿Eliminar la obra \"" + selected.name() + "\"?"
        );

        confirmation.setContentText(
                "La obra dejará de aparecer en los "
                        + "listados activos. Esta operación "
                        + "realiza una eliminación lógica."
        );

        styleDialog(confirmation);

        boolean confirmed = confirmation.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .isPresent();

        if (!confirmed) {
            return;
        }

        int targetPage =
                works.size() == 1 && currentPage > 0
                        ? currentPage - 1
                        : currentPage;

        executeMutation(
                worksApi.delete(selected.id()),
                "Obra eliminada correctamente.",
                targetPage
        );
    }

    // EJECUTAR CAMBIOS Y ACTUALIZAR TABLA

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
                        statusLabel.setText(
                                "No se pudo completar la operación."
                        );

                        showError(errorMessage(error));

                        Throwable cause = unwrap(error);

                        if (cause instanceof WorksApiClient.ApiException apiError
                                && apiError.status() == 412) {

                            loadPage(
                                    currentPage,
                                    "La obra cambió en el servidor. "
                                            + "Revisa su información."
                            );
                        }

                        return;
                    }

                    loadPage(targetPage, successMessage);
                })
        );
    }

    // ESTADO DE LA INTERFAZ

    private void setBusy(boolean value) {

        busy = value;
        worksTable.setDisable(value);

        updateButtons();
    }

    private void updateButtons() {

        boolean selected = worksTable
                .getSelectionModel()
                .getSelectedItem() != null;

        newWorkButton.setDisable(busy);
        refreshButton.setDisable(busy);

        editWorkButton.setDisable(
                busy || !selected
        );

        deleteWorkButton.setDisable(
                busy || !selected
        );

        previousPageButton.setDisable(
                busy || currentPage == 0
        );

        nextPageButton.setDisable(
                busy || !hasNext
        );
    }

    // MENSAJES

    private void showInfo(
            String title,
            String message) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle("BRICA - Obras");
        alert.setHeaderText(title);
        alert.setContentText(message);

        styleDialog(alert);
        alert.showAndWait();
    }

    private void showError(String message) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle("BRICA - Obras");
        alert.setHeaderText(
                "No se pudo completar la operación"
        );
        alert.setContentText(message);

        styleDialog(alert);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {

        dialog.initOwner(
                worksTable.getScene().getWindow()
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

    private Throwable unwrap(Throwable error) {

        Throwable cause = error;

        while (cause instanceof CompletionException
                && cause.getCause() != null) {

            cause = cause.getCause();
        }

        return cause;
    }

    private String errorMessage(Throwable error) {

        Throwable cause = unwrap(error);

        if (cause instanceof HttpTimeoutException
                || cause instanceof IOException) {

            return "No se pudo establecer comunicación "
                    + "con el servidor BRICA.";
        }

        String message = cause.getMessage();

        return message == null || message.isBlank()
                ? "Ocurrió un error inesperado."
                : message;
    }
}
