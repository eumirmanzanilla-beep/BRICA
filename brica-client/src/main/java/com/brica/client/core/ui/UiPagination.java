package com.brica.client.core.ui;

import javafx.scene.Node;

public final class UiPagination {
    private UiPagination() { }
    public static void show(Node control, boolean needed) { control.setVisible(needed); control.setManaged(needed); }
    public static void update(Node previous, Node next, Node label, int page, boolean hasNext) {
        show(previous, page > 0); show(next, hasNext); show(label, page > 0 || hasNext);
    }
}
