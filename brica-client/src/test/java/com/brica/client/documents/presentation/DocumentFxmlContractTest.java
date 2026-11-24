package com.brica.client.documents.presentation;

import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Headless contract check; actual JavaFX loading and Windows interaction remain separate. */
class DocumentFxmlContractTest {
    @Test void shouldResolveEveryDocumentControllerHandlerIncludeAndStylesheet() throws Exception {
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        for (var name : List.of("document-configuration-view.fxml", "reference-fields-view.fxml", "templates-view.fxml",
                "template-version-dialog.fxml", "document-packages-view.fxml", "reference-values-view.fxml", "works-view.fxml", "clients-view.fxml", "main-view.fxml")) {
            var url = getClass().getResource(resourcePath(name)); assertNotNull(url);
            var root = factory.newDocumentBuilder().parse(url.openStream()).getDocumentElement();
            var controller = Class.forName(root.getAttributeNS("http://javafx.com/fxml/1", "controller"));
            var nodes = root.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                var node = (org.w3c.dom.Element) nodes.item(i);
                if (node.hasAttribute("onAction")) {
                    var handler = node.getAttribute("onAction").substring(1);
                    assertTrue(java.util.Arrays.stream(controller.getDeclaredMethods()).anyMatch(m -> m.getName().equals(handler)), name + ": " + handler);
                }
                if (node.hasAttribute("source")) {
                    try (var ignored = new java.net.URL(url, node.getAttribute("source")).openStream()) {
                        assertNotNull(ignored);
                    }
                }
            }
        }
    }
    private static String resourcePath(String name) {
        return switch (name) {
            case "main-view.fxml", "dashboard-view.fxml" -> "/com/brica/client/shell/" + name;
            case "clients-view.fxml" -> "/com/brica/client/clients/" + name;
            case "works-view.fxml" -> "/com/brica/client/works/" + name;
            default -> "/com/brica/client/documents/" + name;
        };
    }
}
