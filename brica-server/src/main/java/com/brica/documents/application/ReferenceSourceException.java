package com.brica.documents.application;

public class ReferenceSourceException extends RuntimeException {
    public ReferenceSourceException(Throwable cause) {
        super("No se pudieron consultar los datos de la obra y su cliente. Vuelve a consultar.", cause);
    }
}
