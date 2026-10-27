package com.brica.works.application;

public class WorkNotFoundException extends RuntimeException {

    public WorkNotFoundException(Long workId) {
        super("Work not found with id: " + workId);
    }
}