package com.brica.client.presentation;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.brica.client.core.http.ServerHealthClient;
import com.brica.client.core.http.ServerSession;
import com.brica.client.config.ClientConfig;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {
    private ServerSession session;
    private Parent documentView;
    private DocumentConfigurationController documentController;
    private Runnable logout = () -> { };

    public void configureSession(ServerSession session, Runnable logout) {
        this.session = session;
        this.logout = logout;
        var user = session.user();
        clientsNavButton.setDisable(!user.can("CLIENTS_READ"));
        worksNavButton.setDisable(!user.can("WORKS_READ"));
        contractsNavButton.setDisable(!user.can("CONTRACTS_READ") && !user.can("CONTRACTS_WRITE"));
        documentsNavButton.setDisable(!user.can("DOCUMENT_CONFIG_ADMIN"));
    }

    @FXML
    private void onLogout() { requestLeave(logout); }

    public void requestLeave(Runnable continuation) {
        if (documentController != null) { documentController.requestLeave(continuation); }
        else { continuation.run(); }
    }

    private static final String VIEW_BASE_PATH =
            "/com/brica/client/presentation/";

    private final ServerHealthClient healthClient =
            new ServerHealthClient(ClientConfig.serverBaseUri());

    // Estado del servidor
    @FXML
    private Label serverStatusLabel;

    @FXML
    private Button checkServerButton;

    // Navegación
    @FXML
    private Button homeNavButton;

    @FXML
    private Button clientsNavButton;

    @FXML
    private Button worksNavButton;

    @FXML
    private Button contractsNavButton;
    @FXML private Button documentsNavButton;

    // Encabezado y contenido principal
    @FXML
    private Label sectionTitleLabel;

    @FXML
    private Label sectionSubtitleLabel;

    @FXML
    private StackPane contentContainer;

    private enum Section {

        HOME(
                "Panel de inicio",
                "Gestión administrativa y documental",
                "dashboard-view.fxml",
                "Aquí aparecerá el dashboard principal."
        ),

        CLIENTS(
                "Clientes",
                "Administración de clientes",
                "clients-view.fxml",
                "Aquí aparecerá el módulo de clientes."
        ),

        WORKS(
                "Obras",
                "Administración de obras",
                "works-view.fxml",
                "Aquí aparecerá el módulo de obras."
        ),

        DOCUMENTS("Catálogo de campos", "Definición de campos reutilizables", "reference-fields-view.fxml", "Catálogo de campos"),

        CONTRACTS(
                "Contratos",
                "Registro de contratos",
                "contracts-view.fxml",
                "Aquí aparecerá el registro de contratos."
        );

        private final String title;
        private final String subtitle;
        private final String fxmlFile;
        private final String placeholderMessage;

        Section(
                String title,
                String subtitle,
                String fxmlFile,
                String placeholderMessage) {

            this.title = title;
            this.subtitle = subtitle;
            this.fxmlFile = fxmlFile;
            this.placeholderMessage = placeholderMessage;
        }
    }

    @FXML
    private void initialize() {
        navigateTo(Section.HOME);
    }

    // ACCIONES DEL MENÚ LATERAL

    @FXML
    private void onNavigateHome() {
        navigateTo(Section.HOME);
    }

    @FXML
    private void onNavigateClients() {
        navigateTo(Section.CLIENTS);
    }

    @FXML
    private void onNavigateWorks() {
        navigateTo(Section.WORKS);
    }

    @FXML
    private void onNavigateContracts() {
        navigateTo(Section.CONTRACTS);
    }
    @FXML private void onNavigateDocuments() { navigateTo(Section.DOCUMENTS); }

    // NAVEGACIÓN CENTRALIZADA

    private void navigateTo(Section section) {
        if (documentController != null && section != Section.DOCUMENTS) {
            documentController.requestLeave(() -> displaySection(section));
        } else { displaySection(section); }
    }

    private void displaySection(Section section) {

        Parent view;

        try {
            view = loadView(section);

        } catch (IOException exception) {

            exception.printStackTrace();

            view = createMessageView(
                    "No se pudo abrir " + section.title,
                    "Ocurrió un error al cargar esta sección."
            );
        }

        contentContainer.getChildren().setAll(view);

        sectionTitleLabel.setText(section.title);
        sectionSubtitleLabel.setText(section.subtitle);

        updateSelectedNavigation(section);
    }

    // CARGA DE ARCHIVOS FXML

    private Parent loadView(Section section)
            throws IOException {
        if (section == Section.DOCUMENTS && documentView != null) { return documentView; }

        URL resource = getClass().getResource(
                VIEW_BASE_PATH + section.fxmlFile
        );

        if (resource == null) {

            return createMessageView(
                    section.title,
                    section.placeholderMessage
            );
        }

        FXMLLoader loader = new FXMLLoader(resource);

        Parent view = loader.load();
        if (section == Section.DOCUMENTS) {
            ReferenceFieldsController controller = loader.getController();
            controller.configure(new com.brica.client.documents.api.DocumentsApiClient(ClientConfig.serverBaseUri(), session));
            documentView = view;
        }

        // NUEVO:
        // Conectar el dashboard con la navegación
        // y el estado del servidor.
        if (section == Section.HOME) {

            DashboardController dashboardController =
                    loader.getController();

            dashboardController.configure(
                    () -> navigateTo(Section.CLIENTS),
                    () -> navigateTo(Section.WORKS),
                    () -> navigateTo(Section.CONTRACTS),
                    this::onCheckServer,
                    serverStatusLabel.textProperty()
            );
        }

        return view;
    }

    // VISTAS TEMPORALES

    private Parent createMessageView(
            String title,
            String message) {

        VBox container = new VBox(12);

        container.getStyleClass().add("content-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("content-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("content-description");
        messageLabel.setWrapText(true);

        container.getChildren().addAll(
                titleLabel,
                messageLabel
        );

        return container;
    }

    // INDICADOR DE NAVEGACIÓN ACTIVA

    private void updateSelectedNavigation(Section section) {

        List<Button> buttons = List.of(
                homeNavButton,
                clientsNavButton,
                worksNavButton,
                contractsNavButton,
                documentsNavButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("nav-active");
        }

        Button selectedButton = switch (section) {
            case HOME -> homeNavButton;
            case CLIENTS -> clientsNavButton;
            case WORKS -> worksNavButton;
            case CONTRACTS -> contractsNavButton;
            case DOCUMENTS -> documentsNavButton;
        };

        selectedButton.getStyleClass().add("nav-active");
    }

    // ESTADO DEL SERVIDOR

    @FXML
    private void onCheckServer() {

        checkServerButton.setDisable(true);
        serverStatusLabel.setText(
                "Comprobando conexión..."
        );

        healthClient.isAvailable().whenComplete(
                (available, error) -> Platform.runLater(() -> {

                    if (error != null) {

                        serverStatusLabel.setText(
                                "Servidor no disponible"
                        );

                    } else if (Boolean.TRUE.equals(available)) {

                        serverStatusLabel.setText(
                                "Servidor BRICA disponible"
                        );

                    } else {

                        serverStatusLabel.setText(
                                "El servidor respondió con un estado inesperado"
                        );
                    }

                    checkServerButton.setDisable(false);
                })
        );
    }
}
