package com.brica.client.clients.presentation;

import java.util.regex.Pattern;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Input rules for client names, which are more legible than identifier-style names. */
final class ClientNameInputRules {
    private static final Pattern VALID_NAME = Pattern.compile("^[\\p{L}][\\p{L}\\p{N} _-]*$");

    private ClientNameInputRules() { }

    static void install(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            String text = change.getControlNewText();
            return text.isEmpty() || VALID_NAME.matcher(text).matches() ? change : null;
        }));
        field.setPromptText("Inicia con letra; admite espacios, - y _");
    }

    static boolean valid(String value, int max) {
        return value != null && value.length() <= max && VALID_NAME.matcher(value).matches();
    }
}
