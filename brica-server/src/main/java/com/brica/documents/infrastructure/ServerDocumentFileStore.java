package com.brica.documents.infrastructure;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;
import com.brica.documents.application.DocumentException;
import com.brica.documents.application.port.DocumentFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Isolated server filesystem adapter. No client-supplied names are used as paths. */
@Component
public class ServerDocumentFileStore implements DocumentFileStore {
    private final Path root;
    private final int maxBytes;
    public ServerDocumentFileStore(String root) { this(root, 1048576); }
    @org.springframework.beans.factory.annotation.Autowired
    public ServerDocumentFileStore(@Value("${brica.documents.storage-root:./var/documents}") String root,
            @Value("${brica.documents.storage-max-bytes:10485760}") int maxBytes) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }
    @Override public String relativePath(UUID id) { return "registrations/" + id + ".docx"; }
    @Override public void put(UUID id, byte[] content) {
        Path temporary = null;
        try {
            Path target = safe(relativePath(id));
            Files.createDirectories(target.getParent());
            safe(relativePath(id));
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Arrays.equals(read(relativePath(id)), content)) { throw unavailable(); }
                return;
            }
            temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var bytes = java.nio.ByteBuffer.wrap(content);
                while (bytes.hasRemaining()) { channel.write(bytes); }
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw unavailable();
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* no broad cleanup */ }
            }
        }
    }
    @Override public byte[] read(String relativePath) {
        try {
            Path path = safe(relativePath);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > maxBytes) { throw unavailable(); }
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                var bytes = input.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) { throw unavailable(); }
                return bytes;
            }
        } catch (IOException exception) { throw unavailable(); }
    }
    private Path safe(String relative) {
        Path path = root.resolve(relative).normalize();
        if (Path.of(relative).isAbsolute() || !path.startsWith(root)) { throw unavailable(); }
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) { throw unavailable(); }
        }
        return path;
    }
    private DocumentException unavailable() {
        return new DocumentException(503, "DOCUMENT_STORAGE_UNAVAILABLE", "El archivo no está disponible o no se pudo guardar de forma segura.");
    }
}
