package com.brica.documents.infrastructure;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import com.brica.documents.application.DocumentException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class PythonDocxAnalyzerTest {
    private String python() { return System.getenv().getOrDefault("BRICA_DOCUMENT_PYTHON", "python3"); }
    @Test
    void shouldRunPythonRegressionSuiteAsPartOfMavenWithoutSkippingMissingDependencies() throws Exception {
        var process = new ProcessBuilder(python(), "src/test/python/test_brica_docx.py")
                .redirectErrorStream(true).redirectOutput(Path.of("target/python-tests.log").toFile()).start();
        try {
            assertTrue(process.waitFor(40, TimeUnit.SECONDS), "Python test timeout");
            assertEquals(0, process.exitValue(), () -> {
                try { return java.nio.file.Files.readString(Path.of("target/python-tests.log")); }
                catch (Exception e) { return e.toString(); }
            });
        } finally { process.destroyForcibly(); }
    }
    @Test
    void shouldAnalyzeUsingRealProcessAndReportBadZipSafely() throws Exception {
        var analyzer = new PythonDocxAnalyzer(JsonMapper.builder().build(), python(), "src/main/python/brica_docx.py", 10, 1048576, 512, 20971520);
        var content = getClass().getResourceAsStream("/documents/acta-inicio-prototipo.docx").readAllBytes();
        var valid = analyzer.analyze(content);
        assertEquals(4, valid.occurrences().size());
        assertTrue(valid.diagnostics().isEmpty());
        assertEquals("INVALID_DOCX", analyzer.analyze(new byte[]{1, 2, 3}).diagnostics().getFirst().code());
        assertThrows(DocumentException.class, () -> analyzer.analyze(new byte[0]));
    }
    @Test
    void shouldReportUnavailableInterpreterWithoutLeakingPaths() {
        var analyzer = new PythonDocxAnalyzer(JsonMapper.builder().build(), "/not/available/python", "private.py", 1, 10, 1, 10);
        var error = assertThrows(DocumentException.class, () -> analyzer.analyze(new byte[]{1}));
        assertEquals(503, error.status());
        assertFalse(error.getMessage().contains("/not/available"));
    }
    @Test
    void shouldTerminateATimedOutProcess(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var script = directory.resolve("slow.py");
        java.nio.file.Files.writeString(script, "import time\ntime.sleep(10)\n");
        var analyzer = new PythonDocxAnalyzer(JsonMapper.builder().build(), python(), script.toString(), 1, 10, 1, 10);
        long start = System.nanoTime();
        assertEquals(503, assertThrows(DocumentException.class, () -> analyzer.analyze(new byte[]{1})).status());
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 6);
    }
}
