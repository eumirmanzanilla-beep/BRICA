package com.brica.client.presentation;

import com.brica.client.api.DocumentsApiClient;
import com.brica.client.api.ServerSession;
import com.brica.client.config.ClientConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class DocumentConfigurationController {
    @FXML private TabPane tabs;
    @FXML private Tab fieldsTab;
    @FXML private Tab templatesTab;
    @FXML private Tab packagesTab;
    @FXML private Tab valuesTab;
    @FXML private ReferenceFieldsController fieldsController;
    @FXML private TemplatesController templatesController;
    @FXML private DocumentPackagesController packagesController;
    @FXML private ReferenceValuesController valuesController;
    private boolean admin;

    public void configure(ServerSession session) {
        var api = new DocumentsApiClient(ClientConfig.serverBaseUri(), session);
        admin = session.user().can("DOCUMENT_CONFIG_ADMIN");
        fieldsTab.setDisable(!admin); templatesTab.setDisable(!admin); packagesTab.setDisable(!admin);
        valuesTab.setDisable(!session.user().can("DOCUMENT_VALUES_READ"));
        if (admin) {
            fieldsController.configure(api);
            templatesController.configure(api, packagesController::loadTemplates);
            packagesController.configure(api, () -> tabs.getSelectionModel().select(templatesTab));
            packagesController.selectorPermissions(session.user().can("CLIENTS_READ"), session.user().can("WORKS_READ"));
        }
        if (!valuesTab.isDisable()) { valuesController.configure(api, session.user().can("WORKS_READ")); }
        tabs.getSelectionModel().select(admin ? fieldsTab : valuesTab);
    }
    public void requestLeave(Runnable continuation) { if (admin) { packagesController.leave(continuation); } else { continuation.run(); } }
}
