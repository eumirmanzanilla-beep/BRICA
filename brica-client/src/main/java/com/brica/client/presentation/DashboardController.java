package com.brica.client.presentation;

import java.util.Objects;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label dashboardServerStatusLabel;

    private Runnable openClients;
    private Runnable openWorks;
    private Runnable openContracts;
    private Runnable checkServer;

    public void configure(
            Runnable openClients,
            Runnable openWorks,
            Runnable openContracts,
            Runnable checkServer,
            ObservableValue<String> serverStatus) {

        this.openClients = Objects.requireNonNull(openClients);
        this.openWorks = Objects.requireNonNull(openWorks);
        this.openContracts = Objects.requireNonNull(openContracts);
        this.checkServer = Objects.requireNonNull(checkServer);

        dashboardServerStatusLabel.textProperty().bind(
                Objects.requireNonNull(serverStatus)
        );
    }

    @FXML
    private void onOpenClients() {
        openClients.run();
    }

    @FXML
    private void onOpenWorks() {
        openWorks.run();
    }

    @FXML
    private void onOpenContracts() {
        openContracts.run();
    }

    @FXML
    private void onCheckServer() {
        checkServer.run();
    }
}
