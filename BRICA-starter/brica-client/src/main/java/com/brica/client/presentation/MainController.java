package com.brica.client.presentation;

import com.brica.client.api.ServerHealthClient;
import com.brica.client.config.ClientConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {

    private final ServerHealthClient healthClient =
            new ServerHealthClient(ClientConfig.serverBaseUri());

    @FXML
    private Label serverStatusLabel;

    @FXML
    private Button checkServerButton;

    @FXML
    private void onCheckServer() {
        checkServerButton.setDisable(true);
        serverStatusLabel.setText("Comprobando conexión...");

        healthClient.isAvailable().whenComplete((available, error) ->
                Platform.runLater(() -> {
                    if (error != null) {
                        serverStatusLabel.setText("Servidor no disponible");
                    } else if (Boolean.TRUE.equals(available)) {
                        serverStatusLabel.setText("Servidor BRICA disponible");
                    } else {
                        serverStatusLabel.setText("El servidor respondió con un estado inesperado");
                    }

                    checkServerButton.setDisable(false);
                }));
    }
}
