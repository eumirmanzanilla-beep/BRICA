package com.brica.works.application;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.brica.documents.application.DocumentException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttachmentFormatTest {
    @Test void recognizesSupportedContainersAndRejectsMismatchesAndUnsafeArchives() throws Exception {
        assertEquals("application/pdf", AttachmentFormat.mime("base.pdf", "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(AttachmentFormat.mime("base.xlsx", zip("[Content_Types].xml", "xl/workbook.xml")).endsWith("spreadsheetml.sheet"));
        assertTrue(AttachmentFormat.mime("contrato.docx", zip("[Content_Types].xml", "word/document.xml")).endsWith("wordprocessingml.document"));
        assertThrows(DocumentException.class, () -> AttachmentFormat.mime("base.xlsx", zip("[Content_Types].xml", "word/document.xml")));
        assertThrows(DocumentException.class, () -> AttachmentFormat.mime("contrato.docx", zip("[Content_Types].xml", "word/document.xml", "word/vbaProject.bin")));
        assertThrows(DocumentException.class, () -> AttachmentFormat.mime("base.xlsx", zip("[Content_Types].xml", "xl/workbook.xml", "../escape")));
        assertThrows(DocumentException.class, () -> AttachmentFormat.mime("base.xls", new byte[10]));
        assertThrows(DocumentException.class, () -> AttachmentFormat.mime("fake.pdf", new byte[10]));
    }
    private byte[] zip(String... entries) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            for (String name : entries) { zip.putNextEntry(new ZipEntry(name)); zip.write("<root/>".getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        return output.toByteArray();
    }
}
