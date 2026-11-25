package com.brica.extraction.infrastructure.worker;
import com.brica.extraction.application.ExtractionExecutionService;
import com.brica.extraction.application.port.SourceProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import java.util.List;

@Configuration @EnableScheduling
public class ExtractionDispatcher {
    private final ExtractionExecutionService execution; private final SourceProcessor processor; private final boolean enabled;
    public ExtractionDispatcher(ExtractionExecutionService execution,SourceProcessor processor,@Value("${brica.extraction.worker-enabled:true}") boolean enabled) { this.execution=execution; this.processor=processor; this.enabled=enabled; }
    @Scheduled(fixedDelayString="${brica.extraction.poll-millis:1000}")
    public void dispatch() {
        if(!enabled) { return; }
        var claim=execution.claim(); if(claim==null) { return; }
        SourceProcessor.Result result;
        try { result=processor.process(claim.sources(),()->execution.active(claim)); }
        catch(Exception failure) {
            result=new SourceProcessor.Result(List.of(),diagnostic(System.getProperty("os.name"),failure));
        }
        execution.publish(claim,result);
    }
    static String diagnostic(String os,Exception failure) {
        if ("ISOLATION_NOT_CONFIGURED".equals(failure.getMessage())) {
            return "No se configuró el aislamiento obligatorio del lector. En Linux se requiere un cgroup v2 delegado; el análisis no se ejecutó. Conserva el borrador y consulta la guía de extracción.";
        }
        if (os.startsWith("Windows")) {
            return "WINDOWS_WORKER_REQUIRES_PACKAGED_SERVER".equals(failure.getMessage())
                    ? "Para extraer en Windows, ejecuta el servidor desde su JAR empaquetado. El análisis no se ejecutó y el borrador se conserva."
                    : "Windows no pudo iniciar o completar el lector aislado. El borrador se conserva; consulta la guía de extracción.";
        }
        return "No se pudo ejecutar el lector aislado. Revisa cgroup, Bubblewrap, OCR y la configuración del servidor; puedes conservar el borrador y capturar manualmente.";
    }
}
