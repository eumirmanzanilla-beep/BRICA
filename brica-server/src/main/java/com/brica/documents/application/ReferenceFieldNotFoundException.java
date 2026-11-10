package com.brica.documents.application;

public class ReferenceFieldNotFoundException extends RuntimeException {
    public ReferenceFieldNotFoundException(Long id) {
        super("No existe el campo de referencia solicitado: " + id);
    }
}
