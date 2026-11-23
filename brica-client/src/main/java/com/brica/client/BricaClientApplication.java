package com.brica.client;

import java.io.IOException;

import com.brica.client.core.http.ServerSession;
import com.brica.client.config.ClientConfig;
import com.brica.client.presentation.LoginController;
import com.brica.client.presentation.MainController;
import javafx.application.Platform;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BricaClientApplication extends Application {
    private Stage stage;
    private final ServerSession session = ServerSession.shared(ClientConfig.serverBaseUri());

    @Override
    public void start(Stage stage) throws IOException {

        this.stage = stage;
        stage.setTitle("BRICA - Gestión Constructora");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        session.onSessionEnded(() -> Platform.runLater(this::showLogin));
        showLogin();
        stage.show();
    }

    private void showLogin() {
        stage.setOnCloseRequest(null);
        try {
            var loader = loader("login-view.fxml");
            var scene = new Scene(loader.load());
            ((LoginController) loader.getController()).configure(session, this::showMain);
            stage.setScene(scene);
        } catch (IOException failure) { throw new IllegalStateException("No se pudo abrir el acceso.", failure); }
    }

    private void showMain() {
        try {
            var loader = loader("main-view.fxml");
            var scene = new Scene(loader.load());
            ((MainController) loader.getController()).configureSession(session,
                    () -> session.logout().whenComplete((ignored, error) -> Platform.runLater(this::showLogin)));
            MainController controller = loader.getController();
            stage.setOnCloseRequest(event -> {
                event.consume();
                controller.requestLeave(() -> { stage.hide(); Platform.exit(); });
            });
            stage.setScene(scene);
        } catch (IOException failure) { throw new IllegalStateException("No se pudo abrir el escritorio.", failure); }
    }

    private FXMLLoader loader(String name) {
        return new FXMLLoader(BricaClientApplication.class.getResource("/com/brica/client/presentation/" + name));
    }

    @Override
    public void stop() { session.logout(); }

    public static void main(String[] args) {
        launch(args);
    }
}
