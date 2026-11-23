package com.brica.extraction.infrastructure.worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import com.brica.documents.application.DocumentHashes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class IsolatedSourceProcessorTest {
    @Test @DisabledOnOs(OS.WINDOWS)
    void missingDelegatedCgroupFailsBeforeStartingAnyReaderOrInterpreter() {
        var processor=new IsolatedSourceProcessor(JsonMapper.builder().build(),"/must-not-execute/python","/must-not-read/script.py","");
        var error=assertThrows(java.io.IOException.class,()->processor.process(List.of(),()->true));
        assertEquals("ISOLATION_NOT_CONFIGURED",error.getMessage());
    }
    @Test @EnabledOnOs(OS.WINDOWS)
    void nativeWindowsDoesNotRunTheWorkerFromAnUnpackagedClasspath() {
        var processor=new IsolatedSourceProcessor(JsonMapper.builder().build(),"/must-not-execute/python",
                "/must-not-read/script.py","/sys/fs/cgroup/fixture");
        var error=assertThrows(java.io.IOException.class,()->processor.process(List.of(),()->true));
        assertEquals("WINDOWS_WORKER_REQUIRES_PACKAGED_SERVER",error.getMessage());
    }
    @Test @EnabledOnOs(OS.WINDOWS)
    @EnabledIfSystemProperty(named="brica.test.windows.packaged",matches="true")
    void packagedWindowsWorkerReadsContract(@TempDir Path stage) throws Exception {
        var source=stage.resolve("source-0.bin");
        try(var doc=new org.apache.poi.xwpf.usermodel.XWPFDocument();var out=Files.newOutputStream(source)) {
            doc.createParagraph().createRun().setText("Número de contrato: PRUEBA-001");
            doc.createTable(1,1).getRow(0).getCell(0).setText("Contratista: Ejemplo");
            doc.write(out);
        }
        var json=JsonMapper.builder().build();
        var input=new SourceWorker.Input(1,"contrato.docx",DocumentHashes.sha256(Files.readAllBytes(source)),"source-0.bin");
        Files.writeString(stage.resolve("inputs.json"),json.writeValueAsString(List.of(input)));
        var builder=WindowsSourceLaunch.prepare(stage,Path.of("target/brica-server-0.1.0-SNAPSHOT.jar").toAbsolutePath(),
                System.getenv("SystemRoot"),System.getenv("LOCALAPPDATA"));
        var process=builder.start();
        SourceWorker.Output output;
        var pipes=Executors.newVirtualThreadPerTaskExecutor();
        try {
            var stdout=pipes.submit(()->process.getInputStream().readNBytes(1_048_577));
            var stderr=pipes.submit(()->process.getErrorStream().readNBytes(65_537));
            assertTrue(process.waitFor(40,TimeUnit.SECONDS));
            String diagnostic=new String(stderr.get(5,TimeUnit.SECONDS),StandardCharsets.UTF_8);
            assertEquals(0,process.exitValue(),diagnostic);
            output=json.readValue(stdout.get(5,TimeUnit.SECONDS),SourceWorker.Output.class);
        } finally {
            if(process.isAlive()) { process.destroyForcibly(); process.waitFor(5,TimeUnit.SECONDS); }
            pipes.close();
        }
        assertTrue(output.diagnostics().isEmpty(),output.diagnostics().toString());
        var fragments=output.sources().getFirst().fragments();
        assertTrue(fragments.stream().anyMatch(f->f.original().equals("Número de contrato: PRUEBA-001")));
        assertTrue(fragments.stream().anyMatch(f->f.original().equals("Contratista: Ejemplo") && f.location().row()==1));
    }
}
