package com.brica.documents.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import com.brica.documents.application.DocumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ServerDocumentFileStoreTest {
    @TempDir Path directory;
    @Test
    void shouldPublishImmutableBytesAndReadThroughANewAdapter() {
        var files = new ServerDocumentFileStore(directory.toString());
        var id = UUID.randomUUID();
        files.put(id, new byte[]{1, 2});
        files.put(id, new byte[]{1, 2});
        assertArrayEquals(new byte[]{1, 2}, new ServerDocumentFileStore(directory.toString()).read(files.relativePath(id)));
        assertThrows(DocumentException.class, () -> files.put(id, new byte[]{3}));
        assertArrayEquals(new byte[]{1, 2}, files.read(files.relativePath(id)));
    }
    @Test
    void shouldRejectTraversalAndSymlinks() throws Exception {
        var files = new ServerDocumentFileStore(directory.resolve("root").toString());
        assertThrows(DocumentException.class, () -> files.read("../../outside"));
        assertThrows(DocumentException.class, () -> files.read(directory.resolve("outside").toString()));
        Files.createDirectories(directory.resolve("root"));
        Files.createSymbolicLink(directory.resolve("root/registrations"), directory);
        assertThrows(DocumentException.class, () -> files.put(UUID.randomUUID(), new byte[]{1}));
    }
}
