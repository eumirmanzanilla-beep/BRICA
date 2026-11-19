package com.brica.extraction.infrastructure;
import com.brica.extraction.infrastructure.reader.SourceReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SourceReaderTest {
    @TempDir Path dir;
    private String hash(Path path) throws Exception { return com.brica.documents.application.DocumentHashes.sha256(Files.readAllBytes(path)); }
    @Test void readsDigitalPdfWithPageAndCoordinatesAndRejectsOversizedPageBeforeRendering() throws Exception {
        Path path=dir.resolve("digital.pdf");
        try(var doc=new PDDocument()) {
            var page=new PDPage(); doc.addPage(page);
            try(var content=new PDPageContentStream(doc,page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                content.newLineAtOffset(50,700); content.showText("Numero de contrato: PRUEBA_01"); content.endText();
            }
            doc.save(path.toFile());
        }
        var result=new SourceReader(8).read(path,"digital.pdf",hash(path));
        assertTrue(result.fragments().stream().anyMatch(f->f.original().contains("PRUEBA_01") && f.location().page()==1 && f.location().x()!=null));
        try(var doc=new PDDocument()) { doc.addPage(new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(100000,100000))); doc.save(path.toFile()); }
        var failure=assertThrows(java.io.IOException.class,()->new SourceReader(8).read(path,"digital.pdf",hash(path)));
        assertEquals("PDF_PIXEL_LIMIT",failure.getMessage());
    }
    @Test void readsWordContentWithoutMarkersAndPreservesFullDescriptionAndMissingPrice() throws Exception {
        Path path=dir.resolve("source.docx"); String description="Descripción completa con acentos Ñ\nContinuación del mismo concepto";
        try(var doc=new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Denominación de obra: Prueba Ñ");
            var table=doc.createTable(1,3); table.getRow(0).getCell(0).setText("0012"); table.getRow(0).getCell(1).setText(description); table.getRow(0).getCell(2).setText("");
            try(var out=Files.newOutputStream(path)) { doc.write(out); }
        }
        var result=new SourceReader(9).read(path,"source.docx",hash(path));
        assertTrue(result.fragments().stream().anyMatch(f->f.original().equals(description)));
        assertTrue(result.fragments().stream().anyMatch(f->f.location().sheet()!=null && f.location().column()==3 && f.original().isEmpty()));
        assertTrue(result.fragments().stream().allMatch(f->f.location().page()==null));
    }
    @Test void readsLegacyXlsAndUsesWorkbookDateSystemWithoutInventingDecimalPrecision() throws Exception {
        Path path=dir.resolve("source.xls");
        try(var book=new HSSFWorkbook()) { book.createSheet("Parte").createRow(0).createCell(0).setCellValue("0007"); try(var out=Files.newOutputStream(path)) { book.write(out); } }
        var result=new SourceReader(10).read(path,"source.xls",hash(path));
        assertTrue(result.fragments().stream().anyMatch(f->"0007".equals(f.original())));
        assertTrue(result.warnings().contains("EXCEL_DATE_SYSTEM_1900"));
    }
    @Test void readsExternalFormulaCacheWithoutOpeningOrEvaluatingTheExternalWorkbook() throws Exception {
        Path path=dir.resolve("external-cache.xlsx");
        try(var external=new XSSFWorkbook();var book=new XSSFWorkbook()) {
            external.createSheet("External").createRow(0).createCell(0).setCellValue(99);
            int link=book.linkExternalWorkbook("never-open-this-workbook.xlsx",external);
            var cell=book.createSheet("Part").createRow(0).createCell(0);
            cell.setCellFormula("'["+link+"]External'!A1"); cell.getCTCell().setV("77.50");
            try(var out=Files.newOutputStream(path)) { book.write(out); }
        }
        var result=new SourceReader(11).read(path,"external-cache.xlsx",hash(path));
        var formula=result.fragments().stream().filter(f->f.formula()!=null).findFirst().orElseThrow();
        assertEquals("77.50",formula.storedValue()); assertNotEquals("99",formula.storedValue());
        assertTrue(result.warnings().contains("EXTERNAL_FORMULA_REFERENCES_NOT_EVALUATED"));
    }
    @Test void readsRealXlsxReaderWithMissingCacheZeroAndRepeatedTextualCodesWithoutEvaluating() throws Exception {
        Path path=dir.resolve("source.xlsx");
        try(var book=new XSSFWorkbook()) {
            var row=book.createSheet("Parte A").createRow(0); row.createCell(0).setCellValue("0012");
            var absent=row.createCell(1); absent.setCellFormula("1+1"); if(absent.getCTCell().isSetV()) { absent.getCTCell().unsetV(); }
            var zero=row.createCell(2); zero.setCellFormula("1+1"); zero.getCTCell().setV("0");
            try(var out=Files.newOutputStream(path)) { book.write(out); }
        }
        byte[] before=Files.readAllBytes(path);
        var result=new SourceReader(7L).read(path,"source.xlsx",com.brica.documents.application.DocumentHashes.sha256(before));
        var cells=result.fragments().stream().filter(f->f.location().row()!=null).toList();
        assertEquals("0012",cells.get(0).original());
        assertNull(cells.get(1).storedValue()); assertNull(cells.get(1).original()); assertEquals("MISSING_FORMULA_CACHE",cells.get(1).warning());
        assertEquals("0",cells.get(2).storedValue()); assertEquals("1+1",cells.get(2).formula());
        assertArrayEquals(before,Files.readAllBytes(path));
    }
}
