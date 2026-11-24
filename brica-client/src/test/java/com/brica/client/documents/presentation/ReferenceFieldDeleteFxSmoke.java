package com.brica.client.documents.presentation;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.brica.client.documents.api.DocumentDtos.Field;
import com.brica.client.documents.api.DocumentsApiClient;
import com.brica.client.core.http.ServerSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Opt-in graphical deletion through the real confirmation and HTTP client.
 * Only deletes a fresh field created by this check, never an existing user's field.
 */
public final class ReferenceFieldDeleteFxSmoke {
    private ReferenceFieldDeleteFxSmoke() { }
    public static void main(String[] args) throws Exception {
        String configured = System.getenv("BRICA_REVIEW_URL");
        if (configured == null || !configured.matches("http://127\\.0\\.0\\.1:[0-9]+")) {
            throw new IllegalStateException("Indica explícitamente BRICA_REVIEW_URL de la base aislada.");
        }
        var session = new ServerSession(URI.create(configured), true);
        session.login("UsuarioGerencial", System.getenv("BRICA_REVIEW_PASSWORD")).get(15, TimeUnit.SECONDS);
        var api = new DocumentsApiClient(URI.create(configured), session);
        var field = api.createField("GUI_DELETE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
                "Solo prueba de borrado gráfico", "WORK_NAME").get(15, TimeUnit.SECONDS);
        var started = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); started.complete(null); });
        started.get(10, TimeUnit.SECONDS);
        var deleted = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try {
                var loader = new FXMLLoader(ReferenceFieldDeleteFxSmoke.class.getResource("reference-fields-view.fxml"));
                var stage = new Stage(); stage.setScene(new Scene(loader.load(), 800, 600)); stage.show();
                var controller = (ReferenceFieldsController) loader.getController();
                @SuppressWarnings("unchecked") var fields = (ListView<Field>) member(controller, "fields");
                var message = (Label) member(controller, "message");
                message.textProperty().addListener((o, old, text) -> {
                    if (text.equals("Campo " + field.key() + " eliminado.")) { deleted.complete(null); }
                });
                fields.getItems().addListener((javafx.collections.ListChangeListener<Field>) change -> {
                    if (!fields.getItems().contains(field)) { return; }
                    fields.getSelectionModel().select(field);
                    // Runs inside the confirmation's nested event loop, just like pressing Eliminar.
                    Platform.runLater(() -> {
                        try {
                            var dialog = Window.getWindows().stream().map(Window::getScene).filter(java.util.Objects::nonNull)
                                    .map(Scene::getRoot).filter(DialogPane.class::isInstance).map(DialogPane.class::cast)
                                    .filter(p -> p.getButtonTypes().stream().anyMatch(b -> b.getText().equals("Eliminar")))
                                    .findFirst().orElseThrow();
                            var remove = dialog.getButtonTypes().stream().filter(b -> b.getText().equals("Eliminar")).findFirst().orElseThrow();
                            ((Button) dialog.lookupButton(remove)).fire();
                        } catch (Throwable failure) { deleted.completeExceptionally(failure); }
                    });
                    try {
                        var method = controller.getClass().getDeclaredMethod("delete"); method.setAccessible(true); method.invoke(controller);
                    } catch (Throwable failure) { deleted.completeExceptionally(failure); }
                });
                controller.configure(api);
            } catch (Throwable failure) { deleted.completeExceptionally(failure); }
        });
        try {
            deleted.get(40, TimeUnit.SECONDS);
            if (api.fields(0).get(15, TimeUnit.SECONDS).stream().anyMatch(f -> f.id().equals(field.id()))) {
                throw new AssertionError("El campo sigue presente tras confirmar el borrado.");
            }
            System.out.println("Borrado JavaFX OK: selección, confirmación Eliminar, DELETE 204, recarga y mensaje de éxito.");
        } finally {
            Platform.runLater(() -> { List.copyOf(Window.getWindows()).forEach(Window::hide); Platform.exit(); });
            session.logout().get(5, TimeUnit.SECONDS);
        }
    }
    private static Object member(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
}
