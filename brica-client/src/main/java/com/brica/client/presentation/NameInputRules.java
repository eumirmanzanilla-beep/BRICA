package com.brica.client.presentation;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Reject invalid edits/pastes rather than silently changing the user's intended name. */
final class NameInputRules {
    private NameInputRules() { }
    static void install(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) { return change; }
            String text = change.getControlNewText();
            return text.matches("[A-Za-zÑñ0-9_]*") && !text.startsWith("_") ? change : null;
        }));
        field.setPromptText("Letras, números y _; al menos una letra");
    }
    static boolean valid(String text, int max) {
        return text != null && text.length() <= max && text.matches("(?=.*[A-Za-zÑñ])[A-Za-zÑñ0-9][A-Za-zÑñ0-9_]*");
    }
    static void require(String text, int max) {
        if (!valid(text, max)) {
            throw new IllegalArgumentException("Nombre: solo letras sin acentos (ñ permitida), números y _, al menos una letra y sin _ inicial. Máximo " + max + " caracteres.");
        }
    }
}
