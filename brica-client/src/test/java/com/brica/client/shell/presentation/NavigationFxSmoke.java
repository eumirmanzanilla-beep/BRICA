package com.brica.client.shell.presentation;

import com.brica.client.core.ui.AsyncDialogSave;
import com.brica.client.shared.presentation.NameInputRules;
import com.brica.client.works.presentation.WorkCreationWizard;
import com.brica.client.works.presentation.WorkDatePicker;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Graphical checks of current navigation and asynchronous close/recovery. */
public final class NavigationFxSmoke {
    private NavigationFxSmoke() { }
    public static void main(String[] args) throws Exception {
        var finished = new CompletableFuture<Void>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            try {
                var loader = new FXMLLoader(NavigationFxSmoke.class.getResource("/com/brica/client/works/works-view.fxml"));
                var root = loader.<javafx.scene.Parent>load(); var owner = new Stage(); owner.setScene(new Scene(root, 1100, 700)); owner.show();
                @SuppressWarnings("unchecked") var table = (TableView<com.brica.client.works.api.WorkDto>) member(loader.getController(), "worksTable");
                check(table.getColumns().stream().map(TableColumn::getText).toList().equals(List.of("Cliente", "Nombre", "Descripción", "Acciones")), "Columnas mínimas");
                check(!((Button) member(loader.getController(), "previousPageButton")).isVisible(), "Sin paginación innecesaria");
                var sample = new com.brica.client.works.api.WorkDto(99L, 1L, "Obra_minima", null, "Descripción", null, null);
                table.getItems().add(sample); root.applyCss(); root.layout();
                check(root.lookupAll(".work-icon-button").size() >= 4, "Cuatro acciones pequeñas por obra");
                for (var control : root.lookupAll(".work-icon-button")) {
                    var button = (Button) control;
                    check(button.getGraphic() instanceof javafx.scene.shape.SVGPath && button.getTooltip() != null, "Iconos vectoriales con ayuda");
                }
                var name = new TextField(); NameInputRules.install(name);
                name.replaceText(0, 0, "Obra_Ñ1"); check(name.getText().equals("Obra_Ñ1"), "Aceptar ñ");
                name.replaceText(0, name.getLength(), "Obrá mala"); check(name.getText().equals("Obra_Ñ1"), "Rechazar pegado inválido sin transformarlo");
                name.replaceText(0, name.getLength(), "_Mal"); check(name.getText().equals("Obra_Ñ1"), "Rechazar _ inicial");
                var date = WorkDatePicker.create(null); date.setValue(java.time.LocalDate.of(2026, 2, 1));
                check(WorkDatePicker.read(date).equals(java.time.LocalDate.of(2026, 2, 1)), "Selección de calendario");
                date.getEditor().setText("31/02/2026");
                try { WorkDatePicker.read(date); throw new AssertionError("Fecha inválida aceptada"); } catch (java.time.format.DateTimeParseException expected) { }
                var information = new com.brica.client.works.api.WorksApiClient.DocumentInformation(null, null, null, null, null,
                        null, null, null, "IN_PROGRESS", java.util.Map.of());
                WorkCreationWizard.showExisting(owner, new com.brica.client.works.api.WorksApiClient(java.net.URI.create("http://127.0.0.1:9")),
                        new com.brica.client.works.api.WorksApiClient.VersionedInformation(new com.brica.client.works.api.WorksApiClient.CompleteInformation(sample, information), "\"test\""),
                        List.of(), new Label(), ignored -> { throw new AssertionError("La revisión no guarda automáticamente."); });
                var wizard = Window.getWindows().stream().filter(w -> w != owner).map(Window::getScene)
                        .filter(java.util.Objects::nonNull).map(Scene::getRoot).filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElseThrow();
                var body = (VBox) wizard.getContent(); var heading = (Label) body.getChildren().getFirst();
                check(heading.getText().startsWith("2."), "La captura de una obra existente inicia en adjuntos");
                var controls = (javafx.scene.layout.HBox) body.getChildren().getLast();
                ((Button) controls.getChildren().get(1)).fire(); check(heading.getText().startsWith("3."), "Adjuntos opcionales llevan a información");
                ((Button) controls.getChildren().get(1)).fire(); check(heading.getText().startsWith("4."), "Obra sin fechas admite revisión");
                ((Button) wizard.lookupButton(ButtonType.CLOSE)).fire();
                var dialog = new Dialog<ButtonType>(); dialog.initOwner(owner); dialog.setTitle("Prueba de captura");
                var input = new TextField("Captura conservada"); var error = new Label(); var progress = new Label();
                dialog.getDialogPane().setContent(new VBox(input, error)); dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); dialog.show();
                var pending = new CompletableFuture<String>();
                AsyncDialogSave.run(dialog, progress, error, () -> pending, ignored -> { throw new AssertionError("No confirmar un fallo."); });
                check(!dialog.isShowing(), "Cierre sin esperar al servidor");
                long nanos = (long) dialog.getDialogPane().getProperties().get("brica.close.nanos");
                check(nanos < 100_000_000L, "Cierre local inferior a 100 ms: " + nanos);
                check(progress.getText().equals("Guardando…"), "No anunciar guardado prematuro");
                pending.completeExceptionally(new IllegalArgumentException("Conflicto simulado"));
                Platform.runLater(() -> {
                    try {
                        check(dialog.isShowing(), "Restaurar el diálogo ante fallo");
                        check(input.getText().equals("Captura conservada") && error.getText().equals("Conflicto simulado"), "Conservar captura y error");
                        var retry = new CompletableFuture<String>();
                        AsyncDialogSave.run(dialog, progress, error, () -> retry, value -> {
                            try {
                                check(value.equals("ok") && progress.getText().equals("Guardado."), "Solo confirmar la respuesta correcta");
                                System.out.println("Navegación JavaFX OK; cierre local " + nanos / 1_000_000.0 + " ms; captura recuperada tras fallo.");
                                finished.complete(null);
                            } catch (Throwable failure) { finished.completeExceptionally(failure); }
                        });
                        retry.complete("ok");
                    } catch (Throwable failure) { finished.completeExceptionally(failure); }
                });
            } catch (Throwable failure) { finished.completeExceptionally(failure); }
        });
        try { finished.get(30, TimeUnit.SECONDS); }
        finally { Platform.runLater(() -> { List.copyOf(Window.getWindows()).forEach(Window::hide); Platform.exit(); }); }
    }
    private static Object member(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
