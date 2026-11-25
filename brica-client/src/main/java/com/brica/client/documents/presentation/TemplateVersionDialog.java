package com.brica.client.documents.presentation;

import javafx.fxml.FXMLLoader;

final class TemplateVersionDialog {
    private static final String RESOURCE = "/com/brica/client/documents/template-version-dialog.fxml";

    private TemplateVersionDialog() { }

    static FXMLLoader loader() {
        var resource = TemplateVersionDialog.class.getResource(RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("No se encontró el recurso del diálogo de versión.");
        }
        return new FXMLLoader(resource);
    }
}
