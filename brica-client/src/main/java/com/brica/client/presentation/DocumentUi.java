package com.brica.client.presentation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

final class DocumentUi {
    private DocumentUi() { }
    static void style(javafx.scene.control.Dialog<?> dialog) {
        var css = DocumentUi.class.getResource("styles/app.css");
        if (css != null) { dialog.getDialogPane().getStylesheets().add(css.toExternalForm()); }
        dialog.getDialogPane().getStyleClass().add("client-dialog");
    }
    static void style(javafx.scene.Scene scene) {
        var css = DocumentUi.class.getResource("styles/app.css");
        if (css != null) { scene.getStylesheets().add(css.toExternalForm()); }
    }
    static Throwable cause(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) { error = error.getCause(); }
        return error;
    }
    static <T> void run(Node pane, Label message, Supplier<CompletableFuture<T>> task, Consumer<T> success) {
        if (pane.isDisable()) { return; }
        pane.setDisable(true); message.setText("Procesando…");
        CompletableFuture<T> future;
        try { future = task.get(); }
        catch (Exception failure) { future = CompletableFuture.failedFuture(failure); }
        future.whenComplete((value, failure) -> Platform.runLater(() -> {
            pane.setDisable(false);
            if (failure != null) { message.setText(cause(failure).getMessage()); }
            else { message.setText(""); success.accept(value); }
        }));
    }
    static Long optionalId(String text) {
        if (text == null || text.isBlank()) { return null; }
        try { long id = Long.parseLong(text); if (id > 0) { return id; } }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Indica un identificador numérico positivo.");
    }
}
