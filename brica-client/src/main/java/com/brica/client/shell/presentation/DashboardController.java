package com.brica.client.shell.presentation;

import java.util.Objects;

import com.brica.client.config.ClientConfig;
import com.brica.client.documents.presentation.DocumentUi;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DashboardController {

    @FXML
    private Label dashboardServerStatusLabel;
    @FXML private Label dashboardWorksStatusLabel;
    @FXML private TableView<WorkDto> worksTable;
    @FXML private TableColumn<WorkDto, String> workIdColumn, workNameColumn, workLocationColumn, workStartDateColumn;

    private Runnable openClients;
    private Runnable openWorks;
    private Runnable checkServer;
    private boolean worksReadable;

    public void configure(
            Runnable openClients,
            Runnable openWorks,
            Runnable checkServer,
            ObservableValue<String> serverStatus,
            boolean worksReadable) {

        this.openClients = Objects.requireNonNull(openClients);
        this.openWorks = Objects.requireNonNull(openWorks);
        this.checkServer = Objects.requireNonNull(checkServer);
        this.worksReadable = worksReadable;

        dashboardServerStatusLabel.textProperty().bind(
                Objects.requireNonNull(serverStatus)
        );
        configureWorksTable();
    }

    @FXML
    private void onOpenClients() {
        openClients.run();
    }

    @FXML
    private void onOpenWorks() {
        if (worksReadable) {
            openWorks.run();
        }
    }

    @FXML
    private void onCheckServer() {
        checkServer.run();
    }

    private void configureWorksTable() {
        workIdColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(String.valueOf(value.getValue().id())));
        workNameColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        workLocationColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().location()));
        workStartDateColumn.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().startDate()));
        if (!worksReadable) {
            dashboardWorksStatusLabel.setText("No tienes permiso para consultar obras.");
            return;
        }
        dashboardWorksStatusLabel.setText("Cargando obras…");
        new WorksApiClient(ClientConfig.serverBaseUri()).list(0, 10).whenComplete((items, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                dashboardWorksStatusLabel.setText(DocumentUi.cause(failure).getMessage());
                return;
            }
            worksTable.getItems().setAll(items);
            dashboardWorksStatusLabel.setText(items.isEmpty() ? "No hay obras registradas." : items.size() + " obra(s) reciente(s).");
        }));
    }
}
