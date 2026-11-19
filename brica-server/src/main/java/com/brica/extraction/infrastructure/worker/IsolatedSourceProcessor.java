package com.brica.extraction.infrastructure.worker;

import com.brica.extraction.application.SourceEvidenceService.Content;
import com.brica.extraction.application.port.SourceProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

@Component
public class IsolatedSourceProcessor implements SourceProcessor {
    private final JsonMapper json; private final String python,script,cgroup;
    public IsolatedSourceProcessor(JsonMapper json,@Value("${brica.extraction.python:python3}") String python,
            @Value("${brica.extraction.script:src/main/python/brica_extraction.py}") String script,
            @Value("${brica.extraction.cgroup-root:}") String cgroup) { this.json=json; this.python=python; this.script=script; this.cgroup=cgroup; }
    @Override public Result process(List<Content> sources,BooleanSupplier active) throws Exception {
        if(cgroup.isBlank() || !System.getProperty("os.name").equals("Linux")) { throw new IOException("ISOLATION_NOT_CONFIGURED"); }
        Path stage=Files.createTempDirectory("brica-source-"); Process process=null;
        var tasks=Executors.newVirtualThreadPerTaskExecutor();
        try {
            var inputs=new ArrayList<SourceWorker.Input>();
            for(int i=0;i<sources.size();i++) {
                var source=sources.get(i); String name="source-"+i+".bin"; Files.write(stage.resolve(name),source.bytes(),StandardOpenOption.CREATE_NEW);
                inputs.add(new SourceWorker.Input(source.id(),source.name(),source.sha256(),name));
            }
            var paths=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(p->Path.of(p).toAbsolutePath().normalize().toString()).toList();
            boolean boot=paths.size()==1 && paths.getFirst().endsWith(".jar");
            var manifest=new LinkedHashMap<String,Object>(); manifest.put("inputs",inputs); manifest.put("classpath",paths);
            manifest.put("javaHome",System.getProperty("java.home")); manifest.put("boot",boot);
            Path request=stage.resolve("manifest.json"); Files.writeString(request,json.writeValueAsString(manifest));
            var builder=new ProcessBuilder(python,"-I",Path.of(script).toAbsolutePath().toString(),"--input",stage.toString(),"--cgroup-root",cgroup);
            // Python's standard library and the runtime loader need PATH, not session/DB credentials.
            builder.environment().clear(); builder.environment().put("PATH","/usr/bin:/bin"); builder.environment().put("LANG","C.UTF-8");
            process=builder.start(); Process running=process;
            var stdout=tasks.submit(()->bounded(running.getInputStream(),1_048_576));
            var stderr=tasks.submit(()->bounded(running.getErrorStream(),65_536));
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(125);
            while(!process.waitFor(250,TimeUnit.MILLISECONDS)) {
                if(System.nanoTime()>deadline || !active.getAsBoolean() || stdout.isDone() && stdout.state()==Future.State.FAILED || stderr.isDone() && stderr.state()==Future.State.FAILED) {
                    throw new IOException("READER_CANCELLED_OR_LIMIT");
                }
            }
            byte[] output=stdout.get(3,TimeUnit.SECONDS); stderr.get(3,TimeUnit.SECONDS);
            if(process.exitValue()!=0) { throw new IOException("READER_SANDBOX_FAILED"); }
            var value=json.readValue(output,SourceWorker.Output.class);
            if(value.diagnostics()==null || value.sources()==null || value.diagnostics().size()>4 || value.diagnostics().stream().anyMatch(d->d==null || !d.matches("SOURCE_UNPROCESSABLE:[1-9][0-9]{0,18}:[A-Z_]{1,80}"))) { throw new IOException("INVALID_READER_PROTOCOL"); }
            String diagnostic=value.diagnostics().isEmpty() ? null:"Fuentes no procesables (archivo y código): "+String.join(", ",value.diagnostics())+". Conserva la evidencia o captura manualmente.";
            if(value.sources().stream().anyMatch(s->s.warnings().stream().anyMatch(w->!w.startsWith("EXCEL_DATE_SYSTEM")))) { diagnostic="Hay páginas o estructuras que requieren revisión manual."; }
            return new Result(value.sources(),diagnostic);
        } finally {
            if(process!=null && process.isAlive()) { process.destroyForcibly(); process.waitFor(5,TimeUnit.SECONDS); }
            tasks.shutdownNow(); tasks.close();
            // Only this invocation's private scratch, never a document-store path or caller supplied directory.
            try(var paths=Files.walk(stage)) { for(var path:paths.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(path); } }
        }
    }
    private static byte[] bounded(InputStream stream,int limit) throws IOException {
        try(stream) { byte[] bytes=stream.readNBytes(limit+1); if(bytes.length>limit) { throw new IOException("READER_OUTPUT_LIMIT"); } return bytes; }
    }
}
