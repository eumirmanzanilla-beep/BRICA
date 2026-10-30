package com.brica.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class BricaClientApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        var loader = new FXMLLoader(
                BricaClientApplication.class.getResource(
                        "/com/brica/client/presentation/main-view.fxml"
                )
        );

        // Especificamos el tipo Parent requerido por Scene.
        Parent root = loader.load();

        // Obtener el espacio disponible de la pantalla.
        var screenBounds = Screen.getPrimary().getVisualBounds();

        // Calcular un tamaño inicial adaptable.
        double initialWidth = Math.min(
                1100,
                screenBounds.getWidth() * 0.90
        );

        double initialHeight = Math.min(
                700,
                screenBounds.getHeight() * 0.90
        );

        // Crear la escena.
        var scene = new Scene(
                root,
                initialWidth,
                initialHeight
        );

        stage.setTitle("BRICA - Gestión Constructora");
        stage.setScene(scene);

        // Permitir redimensionar la ventana.
        stage.setResizable(true);

        // Definir dimensiones mínimas adaptables.
        stage.setMinWidth(
                Math.min(760, screenBounds.getWidth() * 0.70)
        );

        stage.setMinHeight(
                Math.min(460, screenBounds.getHeight() * 0.70)
        );

        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
