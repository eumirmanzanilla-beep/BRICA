package com.brica.documents.application;

public class DuplicateReferenceKeyException extends RuntimeException {
    public DuplicateReferenceKeyException(String key) {
        super("La clave de referencia ya está registrada: " + key);
    }
}
