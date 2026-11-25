package com.brica.extraction.infrastructure.worker;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionDispatcherDiagnosticTest {
    @Test void missingIsolationProvidesActionableMessageWithoutRunningUnisolated() {
        var message = ExtractionDispatcher.diagnostic("Linux", new IOException("ISOLATION_NOT_CONFIGURED"));
        assertTrue(message.contains("cgroup v2 delegado"));
        assertTrue(message.contains("no se ejecutó"));
    }

    @Test void windowsDevelopmentClasspathExplainsPackagedServerRequirement() {
        var message = ExtractionDispatcher.diagnostic("Windows 11", new IOException("WINDOWS_WORKER_REQUIRES_PACKAGED_SERVER"));
        assertTrue(message.contains("JAR empaquetado"));
        assertTrue(message.contains("borrador"));
    }

    @Test void unexpectedErrorsDoNotLeakPathsOrContent() {
        var message = ExtractionDispatcher.diagnostic("Linux", new IOException("/private/customer.xlsx"));
        assertFalse(message.contains("/private"));
        assertTrue(message.contains("lector aislado"));
    }
}
