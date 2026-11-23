package com.brica.client.core.ui;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;

/** Immediate local acknowledgement, never an optimistic claim of successful persistence. */
public final class AsyncDialogSave {
    private static final String PENDING = "brica.save.pending";
    private AsyncDialogSave() { }
    public static <T> void run(Dialog<?> dialog, Label progress, Label error, Supplier<CompletableFuture<T>> operation, Consumer<T> saved) {
        if (Boolean.TRUE.equals(dialog.getDialogPane().getProperties().get(PENDING))) { return; }
        dialog.getDialogPane().getProperties().put(PENDING, true);
        error.setText(""); progress.setText("Guardando…");
        long start = System.nanoTime();
        dialog.close();
        dialog.getDialogPane().getProperties().put("brica.close.nanos", System.nanoTime() - start);
        CompletableFuture<T> future;
        try { future = operation.get(); }
        catch (Exception failure) { future = CompletableFuture.failedFuture(failure); }
        future.whenComplete((value, failure) -> Platform.runLater(() -> {
            dialog.getDialogPane().getProperties().put(PENDING, false);
            if (failure != null) {
                String detail = cause(failure).getMessage();
                progress.setText("No se guardaron los cambios. Revisa la captura conservada.");
                error.setText(detail == null ? "No se pudo guardar. Puedes corregir o reintentar." : detail);
                dialog.show();
            } else { progress.setText("Guardado."); saved.accept(value); }
        }));
    }
    private static Throwable cause(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }
}
