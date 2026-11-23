package com.brica.client.presentation;

import com.brica.client.core.http.ServerSession;
import com.brica.client.auth.api.LoginException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private Label usernameLabel;
    @FXML private Label passwordLabel;
    private ServerSession session;
    private Runnable onSuccess;

    @FXML
    private void initialize() {
        usernameLabel.setLabelFor(usernameField);
        passwordLabel.setLabelFor(passwordField);
    }

    public void configure(ServerSession session, Runnable onSuccess) {
        this.session = session;
        this.onSuccess = onSuccess;
    }

    @FXML
    private void onLogin() {
        if (loginButton.isDisable()) { return; }
        String username = usernameField.getText();
        String password = passwordField.getText();
        passwordField.clear();
        loginButton.setDisable(true);
        statusLabel.setText("Comprobando acceso…");
        session.login(username, password).whenComplete((user, failure) -> Platform.runLater(() -> {
            loginButton.setDisable(false);
            if (failure == null) { onSuccess.run(); }
            else { statusLabel.setText(LoginException.messageFor(failure)); }
        }));
    }
}
