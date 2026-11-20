package com.brica.extraction.infrastructure.worker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceWorkerTest {
    @Test void distinguishesSafeFailureCodesWithoutReturningPathsOrBusinessText() {
        assertEquals("DAMAGED_CONTAINER",SourceWorker.diagnostic(new java.util.zip.ZipException("private-path")));
        assertEquals("PDF_PIXEL_LIMIT",SourceWorker.diagnostic(new java.io.IOException("PDF_PIXEL_LIMIT")));
        assertEquals("UNREADABLE_SOURCE",SourceWorker.diagnostic(new java.io.IOException("/private/customer.xlsx document text")));
    }
    @Test void distinguishesAnActuallyEncryptedPdf(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var file=directory.resolve("private-name.pdf");
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument()) {
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            doc.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner-test","user-test",new org.apache.pdfbox.pdmodel.encryption.AccessPermission()));
            doc.save(file.toFile());
        }
        var failure=assertThrows(org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException.class,()->{
            try(var ignored=org.apache.pdfbox.Loader.loadPDF(file.toFile())) { fail("Password protected source was opened"); }
        });
        assertEquals("ENCRYPTED_PDF",SourceWorker.diagnostic(failure));
    }
    @Test void publishesPartialSourcesAndSafeDiagnosticsWithoutReplacingAnyBusinessData(@org.junit.jupiter.api.io.TempDir java.nio.file.Path root) throws Exception {
        var good=root.resolve("source-0.bin");
        try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            book.createSheet("Part").createRow(0).createCell(0).setCellValue("0012");
            try(var out=java.nio.file.Files.newOutputStream(good)) { book.write(out); }
        }
        var bad=root.resolve("source-1.bin"); java.nio.file.Files.writeString(bad,"%PDF-1.7\ntruncated synthetic input");
        String goodHash=com.brica.documents.application.DocumentHashes.sha256(java.nio.file.Files.readAllBytes(good));
        String badHash=com.brica.documents.application.DocumentHashes.sha256(java.nio.file.Files.readAllBytes(bad));
        var result=SourceWorker.readInputs(new SourceWorker.Input[]{new SourceWorker.Input(1,"source.xlsx",goodHash,"source-0.bin"),new SourceWorker.Input(2,"broken.pdf",badHash,"source-1.bin")},root);
        assertEquals(1,result.sources().size()); assertEquals(1,result.diagnostics().size());
        assertEquals("SOURCE_UNPROCESSABLE:2:UNREADABLE_SOURCE",result.diagnostics().getFirst());
        assertTrue(result.sources().getFirst().fragments().stream().anyMatch(f->"0012".equals(f.original())));
    }
}
