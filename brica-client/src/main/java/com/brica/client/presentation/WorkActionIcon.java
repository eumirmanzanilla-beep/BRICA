package com.brica.client.presentation;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/** Fixed vector icons independent of OS emoji fonts. Coordinates use a 16px grid. */
final class WorkActionIcon {
    private WorkActionIcon() { }
    static Node create(String kind) {
        var path = new SVGPath();
        path.setContent(switch (kind) {
            case "edit" -> "M2 11 L11 2 L14 5 L5 14 L2 14 Z M9 4 L12 7";
            case "capture" -> "M3 1 L10 1 L14 5 L14 15 L3 15 Z M10 1 L10 5 L14 5 M6 8 L11 8 M6 11 L11 11";
            case "view" -> "M1 8 Q8 -2 15 8 Q8 18 1 8 Z M10 8 A2 2 0 1 1 6 8 A2 2 0 1 1 10 8";
            case "documents" -> "M1 4 L6 4 L8 6 L15 6 L15 14 L1 14 Z M3 4 L3 1 L13 1 L13 6";
            default -> throw new IllegalArgumentException("Icono desconocido.");
        });
        path.setFill(javafx.scene.paint.Color.TRANSPARENT);
        path.setStroke(javafx.scene.paint.Color.web("#326781")); path.setStrokeWidth(1.4);
        path.setMouseTransparent(true);
        return path;
    }
}
