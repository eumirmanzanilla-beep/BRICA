package com.brica.client.presentation;

import javafx.scene.Node;

final class UiPagination {
    private UiPagination() { }
    static void show(Node control, boolean needed) { control.setVisible(needed); control.setManaged(needed); }
    static void update(Node previous, Node next, Node label, int page, boolean hasNext) {
        show(previous, page > 0); show(next, hasNext); show(label, page > 0 || hasNext);
    }
}
