package com.brica.works.application;

public class WorkNotFoundException extends RuntimeException {

    public WorkNotFoundException(Long workId) {
        super("Obra no encontrada con id: " + workId);
    }
}
