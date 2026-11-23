package com.brica.client.presentation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.brica.client.clients.api.ClientDto;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Explicit graphical smoke, run separately from the headless test suite; needs a display. */
public final class DocumentFxSmoke {
    private DocumentFxSmoke() { }
    public static void main(String[] args) throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            try {
                run(); result.complete(null);
            } catch (Throwable failure) { result.completeExceptionally(failure); }
            finally { List.copyOf(Window.getWindows()).forEach(Window::hide); }
        });
        try { result.get(45, TimeUnit.SECONDS); System.out.println("JavaFX smoke OK: FXML, captura, permisos visuales y cierre durante carga."); }
        finally { Platform.exit(); }
    }

    private static void run() throws Exception {
        for (String name : List.of("reference-fields-view.fxml", "templates-view.fxml", "template-version-dialog.fxml", "document-configuration-view.fxml")) {
            new FXMLLoader(DocumentFxSmoke.class.getResource(name)).load();
        }
        var loader = new FXMLLoader(DocumentFxSmoke.class.getResource("document-packages-view.fxml"));
        Parent root = loader.load(); var owner = new Stage(); owner.setScene(new Scene(root, 900, 650)); owner.show();
        Object packages = loader.getController(); invoke(packages, "newPackage");
        ((DocumentPackagesController) packages).selectorPermissions(false, false);
        check(((javafx.scene.Node) field(packages, "clientSelector")).isDisable()
                && ((javafx.scene.Node) field(packages, "workSelector")).isDisable(), "Permisos de selectores");
        ((DocumentPackagesController) packages).selectorPermissions(true, true);
        var name = (TextField) field(packages, "name"); name.setText("Captura conservada");
        @SuppressWarnings("unchecked") var client = (ComboBox<ClientDto>) field(packages, "clientChoice");
        @SuppressWarnings("unchecked") var work = (ComboBox<WorkDto>) field(packages, "workChoice");
        client.setValue(new ClientDto(11L, "Cliente A", "Representante"));
        work.setValue(new WorkDto(5L, 22L, "Obra B", "Lugar", "", "2026-01-01", null));
        check(client.getValue() == null && field(packages, "selectedClientId").equals(22L), "Contexto de obra");
        work.setValue(null);
        check(field(packages, "selectedWorkId").equals(5L), "Paginación no pierde el contexto de obra");
        invoke(packages, "registerTemplate");
        check(name.getText().equals("Captura conservada"), "No perder captura al registrar plantilla");
        invoke(packages, "resume");
        var dialog = (Dialog<?>) field(packages, "dialog");
        check(dialog.isShowing(), "Reanudar diálogo");
        var form = (VBox) field(packages, "form"); form.setDisable(true); dialog.close();
        check(dialog.isShowing() && root.isDisable(), "No cerrar ni editar durante petición");
        form.setDisable(false); set(packages, "dirty", false); dialog.close();
        check(!dialog.isShowing(), "Cerrar captura limpia");
        loader = new FXMLLoader(DocumentFxSmoke.class.getResource("reference-values-view.fxml"));
        owner.getScene().setRoot(loader.load()); Object values = loader.getController(); invoke(values, "openDialog");
        dialog = (Dialog<?>) field(values, "dialog"); form = (VBox) field(values, "form");
        form.setDisable(true); dialog.close(); check(dialog.isShowing(), "Valores no se cierra durante petición");
        form.setDisable(false); dialog.close();
        var info = new WorksApiClient.DocumentInformation(null, null, null, null, null, "2005-07-16", null, null,
                "IN_PROGRESS", Map.of("RETENIDO", "Texto histórico"));
        WorkDocumentInformationDialog.show(owner, 1, null, new WorksApiClient.VersionedDocumentInformation(info, "\"1\""),
                List.of(new com.brica.client.documents.api.DocumentDtos.Field(1L, "FECHA", "Fecha adicional", "DATE", "ADDITIONAL", "FECHA")),
                false, ignored -> { throw new AssertionError("Una consulta no puede guardar."); });
        check(Window.getWindows().size() >= 2, "Datos documentales carga en JavaFX");
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
    private static Field member(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object field(Object target, String name) throws Exception { return member(target, name).get(target); }
    private static void set(Object target, String name, Object value) throws Exception { member(target, name).set(target, value); }
    private static void invoke(Object target, String name) throws Exception {
        var method = target.getClass().getDeclaredMethod(name); method.setAccessible(true); method.invoke(target);
    }
}
