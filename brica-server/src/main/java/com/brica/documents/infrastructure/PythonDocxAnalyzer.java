package com.brica.documents.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.brica.documents.application.DocumentException;
import com.brica.documents.application.port.DocxAnalyzer;
import com.brica.documents.domain.DocxAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PythonDocxAnalyzer implements DocxAnalyzer {
    private final String python;
    private final String script;
    private final int timeout;
    private final int maxBytes;
    private final int maxEntries;
    private final int maxExpanded;
    private final JsonMapper json;
    private final Semaphore workers = new Semaphore(2);

    public PythonDocxAnalyzer(JsonMapper json,
            @Value("${brica.documents.python:python3}") String python,
            @Value("${brica.documents.script:src/main/python/brica_docx.py}") String script,
            @Value("${brica.documents.timeout-seconds:20}") int timeout,
            @Value("${brica.documents.max-bytes:1048576}") int maxBytes,
            @Value("${brica.documents.max-entries:512}") int maxEntries,
            @Value("${brica.documents.max-expanded-bytes:20971520}") int maxExpanded) {
        this.json = json;
        this.python = python;
        this.script = Path.of(script).toAbsolutePath().toString();
        this.timeout = timeout;
        this.maxBytes = maxBytes;
        this.maxEntries = maxEntries;
        this.maxExpanded = maxExpanded;
    }

    @Override
    public DocxAnalysis analyze(byte[] content) {
        if (content == null || content.length == 0 || content.length > maxBytes) {
            throw new DocumentException(400, "INVALID_DOCX_SIZE", "El archivo está vacío o supera el límite de carga.");
        }
        if (!workers.tryAcquire()) {
            throw new DocumentException(503, "ANALYZER_BUSY", "El analizador está ocupado. Vuelve a intentar.");
        }
        Process process = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            process = new ProcessBuilder(python, "-I", script).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            var running = process;
            var exchange = executor.submit(() -> {
                try (var input = running.getOutputStream()) {
                    input.write(json.writeValueAsBytes(Map.of("content", Base64.getEncoder().encodeToString(content),
                            "maxEntries", maxEntries, "maxExpandedBytes", maxExpanded)));
                }
                try (var output = running.getInputStream()) {
                    return output.readNBytes(1024 * 1024 + 1);
                }
            });
            try {
                var output = exchange.get(timeout, TimeUnit.SECONDS);
                if (!running.waitFor(timeout, TimeUnit.SECONDS) || running.exitValue() != 0 || output.length > 1024 * 1024) {
                    throw unavailable();
                }
                return json.readValue(new String(output, StandardCharsets.UTF_8), DocxAnalysis.class);
            } finally {
                running.destroyForcibly();
                exchange.cancel(true);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw unavailable();
        } finally {
            if (process != null) { process.destroyForcibly(); }
            workers.release();
        }
    }

    private DocumentException unavailable() {
        return new DocumentException(503, "DOCX_ANALYZER_UNAVAILABLE",
                "No se pudo analizar el DOCX. Comprueba el entorno Python del servidor y vuelve a intentar.");
    }
}
