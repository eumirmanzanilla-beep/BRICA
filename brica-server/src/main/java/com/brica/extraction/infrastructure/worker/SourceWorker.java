package com.brica.extraction.infrastructure.worker;

import com.brica.documents.application.DocumentHashes;
import com.brica.extraction.domain.SourceResult;
import com.brica.extraction.infrastructure.reader.SourceReader;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Separate JVM main: deliberately does not start Spring, JPA, sessions or the HTTP server. */
public final class SourceWorker {
    public record Input(long attachmentId,String name,String sha256,String file) { }
    public record Output(List<SourceResult> sources,List<String> diagnostics) { }
    public static void main(String[] args) throws Exception {
        var logging=SourceWorker.class.getResource("/extraction-logback.xml");
        if(logging!=null) { System.setProperty("logback.configurationFile",logging.toExternalForm()); }
        if(System.getProperty("os.name").startsWith("Windows")) {
            readWindowsOffice(args);
            return;
        }
        if(!"1".equals(System.getenv("BRICA_SOURCE_SANDBOX"))) { throw new IllegalStateException("SANDBOX_REQUIRED"); }
        String status=Files.readString(Path.of("/proc/self/status"));
        if(!status.contains("CapEff:\t0000000000000000") || !status.contains("NoNewPrivs:\t1") || !status.contains("Seccomp:\t2")) { throw new IllegalStateException("SANDBOX_REQUIRED"); }
        var json=JsonMapper.builder().build();
        byte[] request=System.in.readNBytes(16385);
        writeResult(json,request,Path.of("/input"));
    }
    private static void readWindowsOffice(String[] args) throws Exception {
        String profile=System.getenv("BRICA_WINDOWS_READER_PROFILE");
        if(args.length!=0 || profile==null || !"1".equals(System.getenv("BRICA_WINDOWS_SOURCE_SANDBOX"))) {
            throw new IllegalStateException("SANDBOX_REQUIRED");
        }
        Path root=Path.of("").toAbsolutePath();
        requireReadOnly(root);
        requireReadOnly(Path.of(profile));
        try(var manifest=Files.newInputStream(root.resolve("inputs.json"))) {
            writeResult(JsonMapper.builder().build(),manifest.readNBytes(16385),root,input->{
                String name=input.name().toLowerCase(Locale.ROOT);
                return name.endsWith(".docx") || name.endsWith(".xlsx") || name.endsWith(".xls");
            });
        }
    }
    private static void requireReadOnly(Path directory) throws java.io.IOException {
        Path probe;
        try { probe=Files.createTempFile(directory,"write-check-",".tmp"); }
        catch(AccessDeniedException confined) { return; }
        Files.deleteIfExists(probe);
        throw new IllegalStateException("SANDBOX_WRITE_NOT_DENIED");
    }
    private static void writeResult(JsonMapper json,byte[] request,Path root) throws Exception {
        writeResult(json,request,root,ignored->true);
    }
    private static void writeResult(JsonMapper json,byte[] request,Path root,java.util.function.Predicate<Input> supported) throws Exception {
        if(request.length>16384) { throw new IllegalArgumentException("MANIFEST_LIMIT"); }
        var inputs=json.readValue(request,Input[].class);
        var result=readInputs(inputs,root,supported);
        byte[] output=json.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        if(output.length>900_000) { throw new IllegalArgumentException("RESULT_LIMIT"); }
        System.out.write(output);
    }
    static Output readInputs(Input[] inputs,Path root) throws Exception {
        return readInputs(inputs,root,ignored->true);
    }
    private static Output readInputs(Input[] inputs,Path root,java.util.function.Predicate<Input> supported) throws Exception {
        if(inputs==null || inputs.length==0 || inputs.length>4) { throw new IllegalArgumentException("SOURCE_COUNT_LIMIT"); }
        var results=new ArrayList<SourceResult>(); var diagnostics=new ArrayList<String>();
        var ocrPages=new AtomicInteger();
        for(var input:inputs) {
            try {
                if(!supported.test(input)) { throw new IllegalArgumentException("UNSUPPORTED_SOURCE_FORMAT"); }
                if(!input.file().matches("source-[0-3]\\.bin")) { throw new IllegalArgumentException("INVALID_SOURCE_PATH"); }
                Path path=root.resolve(input.file());
                if(Files.size(path)>10*1024*1024 || !DocumentHashes.sha256(Files.readAllBytes(path)).equals(input.sha256())) { throw new IllegalArgumentException("SOURCE_INTEGRITY"); }
                results.add(new SourceReader(input.attachmentId(),ocrPages).read(path,input.name(),input.sha256()));
            } catch(Exception failure) { diagnostics.add("SOURCE_UNPROCESSABLE:"+input.attachmentId()+":"+diagnostic(failure)); }
        }
        return new Output(List.copyOf(results),List.copyOf(diagnostics));
    }
    static String diagnostic(Exception failure) {
        if(failure instanceof org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException) { return "ENCRYPTED_PDF"; }
        if(failure instanceof org.apache.poi.EncryptedDocumentException) { return "ENCRYPTED_OFFICE"; }
        if(failure instanceof java.util.zip.ZipException) { return "DAMAGED_CONTAINER"; }
        if(failure instanceof org.xml.sax.SAXException) { return "INVALID_OR_UNSAFE_OFFICE_XML"; }
        String value=failure.getMessage();
        return value!=null && Set.of("SOURCE_SIZE_LIMIT","SOURCE_INTEGRITY","INVALID_SOURCE_PATH","UNSUPPORTED_SOURCE_FORMAT", "SOURCE_TEXT_LIMIT",
                "ACTIVE_OR_INVALID_CONTAINER","ZIP_EXPANSION_LIMIT","ACTIVE_OR_EXTERNAL_CONTENT","INVALID_OFFICE_CONTAINER","SHEET_LIMIT","ROW_LIMIT","CELL_LIMIT",
                "OLE_DEPTH_LIMIT","OLE_ENTRY_LIMIT","ACTIVE_OLE_CONTENT","DOCX_BLOCK_LIMIT","DOCX_TABLE_LIMIT","ENCRYPTED_OR_PDF_PAGE_LIMIT","ACTIVE_PDF_CONTENT",
                "PDF_PIXEL_LIMIT","PDF_RESOURCE_DEPTH_LIMIT","PDF_IMAGE_LIMIT","PDF_IMAGE_PIXEL_LIMIT").contains(value) ? value:"UNREADABLE_SOURCE";
    }
}
