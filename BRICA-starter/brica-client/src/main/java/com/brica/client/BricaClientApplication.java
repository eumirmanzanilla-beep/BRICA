package com.brica.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BricaClientApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        var loader = new FXMLLoader(
                BricaClientApplication.class.getResource("/com/brica/client/presentation/main-view.fxml"));

        var scene = new Scene(loader.load());

        stage.setTitle("BRICA");
        stage.setScene(scene);
        stage.setMinWidth(460);
        stage.setMinHeight(280);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
