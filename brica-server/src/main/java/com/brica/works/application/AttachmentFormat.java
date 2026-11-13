package com.brica.works.application;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import com.brica.documents.application.DocumentException;

/** Inspect format without extraction, macros, execution or interpreting document contents. */
public final class AttachmentFormat {
    private AttachmentFormat() { }
    public static String mime(String name, byte[] bytes) {
        if (name == null || name.isBlank() || name.length() > 255 || name.matches(".*[\\p{Cntrl}].*")) { throw invalid(); }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf") && bytes.length >= 5 && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
            return "application/pdf";
        }
        if (!lower.endsWith(".docx") && !lower.endsWith(".xlsx")) { throw invalid(); }
        var entries = new HashSet<String>(); long expanded = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[8192];
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                String path = entry.getName();
                if (path.startsWith("/") || path.contains("\\") || java.util.Arrays.asList(path.split("/")).contains("..")
                        || !entries.add(path) || entries.size() > 4096 || path.toLowerCase(Locale.ROOT).contains("vbaproject")) { throw invalid(); }
                for (int n; (n = zip.read(buffer)) != -1;) {
                    expanded += n;
                    if (expanded > 80L * 1024 * 1024) { throw invalid(); }
                }
            }
        } catch (java.io.IOException failure) { throw invalid(); }
        if (!entries.contains("[Content_Types].xml")) { throw invalid(); }
        if (lower.endsWith(".docx") && entries.contains("word/document.xml")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".xlsx") && entries.contains("xl/workbook.xml")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        throw invalid();
    }
    private static DocumentException invalid() {
        return new DocumentException(400, "INVALID_WORK_ATTACHMENT", "Adjunta un archivo DOCX, PDF o XLSX válido, sin macros.");
    }
}
