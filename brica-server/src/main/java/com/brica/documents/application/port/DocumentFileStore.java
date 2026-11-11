package com.brica.documents.application.port;

import java.util.UUID;

public interface DocumentFileStore {
    void put(UUID id, byte[] content);
    byte[] read(String relativePath);
    String relativePath(UUID id);
}
