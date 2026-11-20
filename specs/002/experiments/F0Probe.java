import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/** Isolated F0 experiment, NOT a production reader or an application entry point. */
public class F0Probe {
    private static final int MAX_PAGES = 40;
    private static final long MAX_PIXELS = 12_000_000;

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[1]);
        if (args[0].equals("build")) {
            Files.createDirectories(dir);
            build(dir);
        } else if (args[0].equals("read")) {
            read(dir);
        } else {
            throw new IllegalArgumentException("build | read");
        }
    }

    private static void build(Path dir) throws Exception {
        try (var doc = new PDDocument()) {
            addTextPage(doc, "Proyecto de Prueba Ñ");
            addTextPage(doc, "0012 pieza 2 5.25 10.50   Total 1234.56");
            doc.save(dir.resolve("S01-digital.pdf").toFile());
        }
        for (boolean mixed : new boolean[]{false, true}) {
            try (var doc = new PDDocument()) {
                var page = new PDPage();
                doc.addPage(page);
                var image = new BufferedImage(1500, 500, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font("DejaVu Sans", Font.PLAIN, 52));
                graphics.drawString("Contrato PRUEBA_01", 40, 100);
                graphics.drawString("Fecha 16/07/2025", 40, 200);
                graphics.drawString("0012 pieza 2 5.25 10.50", 40, 300);
                graphics.dispose();
                try (var content = new PDPageContentStream(doc, page)) {
                    if (mixed) {
                        text(content, "Encabezado digital", 50, 750);
                    }
                    content.drawImage(LosslessFactory.createFromImage(doc, image), 50, 450, 480, 160);
                }
                doc.save(dir.resolve(mixed ? "S03-mixed.pdf" : "S02-scan.pdf").toFile());
            }
        }
        try (var doc = new PDDocument()) {
            addTextPage(doc, "Fuente protegida sintetica");
            doc.protect(new StandardProtectionPolicy("owner-synthetic", "user-synthetic", new AccessPermission()));
            doc.save(dir.resolve("S04-encrypted.pdf").toFile());
        }
        Files.writeString(dir.resolve("S04-damaged.pdf"), "%PDF-1.7\ntruncated", StandardCharsets.US_ASCII);
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage(new PDRectangle(100_000, 100_000)));
            doc.save(dir.resolve("oversized-page.pdf").toFile());
        }
        try (var doc = new PDDocument()) {
            for (int i = 0; i <= MAX_PAGES; i++) doc.addPage(new PDPage());
            doc.save(dir.resolve("too-many-pages.pdf").toFile());
        }
        try (var book = new XSSFWorkbook()) {
            for (String name : new String[]{"ParteA", "ParteB", "Resumen", "Alternativa"}) {
                var sheet = book.createSheet(name);
                var header = sheet.createRow(0);
                String[] labels = {"CLAVE", "DESCRIPCIÓN", "UNIDAD", "CANTIDAD", "PRECIO", "IMPORTE"};
                for (int i = 0; i < labels.length; i++) header.createCell(i).setCellValue(labels[i]);
                var row = sheet.createRow(1);
                row.createCell(0).setCellValue("0012");
                row.createCell(1).setCellValue("Concepto sintético de prueba sin datos reales");
                row.createCell(2).setCellValue("pieza");
                row.createCell(3).setCellValue(2);
                row.createCell(4).setCellValue(5.25);
                var formula = row.createCell(5);
                formula.setCellFormula("D2*E2");
                // Manufacture caches explicitly, never invoke a formula evaluator, even in fixtures.
                if (name.equals("ParteA")) formula.getCTCell().setV("10.50");
                else if (name.equals("ParteB")) {
                    if (formula.getCTCell().isSetV()) formula.getCTCell().unsetV();
                }
                else if (name.equals("Resumen")) formula.getCTCell().setV("99.00");
                else formula.getCTCell().setV("0");
            }
            book.getSheetAt(1).getRow(1).setZeroHeight(true);
            book.setSheetHidden(3, true);
            book.getSheetAt(0).addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 2));
            try (var out = Files.newOutputStream(dir.resolve("S05-S06.xlsx"))) { book.write(out); }
        }
        try (var book = new HSSFWorkbook()) {
            var row = book.createSheet("Parte").createRow(0);
            row.createCell(0).setCellValue("0007");
            row.createCell(1).setCellValue(12.5);
            var style = book.createCellStyle();
            style.setDataFormat(book.createDataFormat().getFormat("0.000"));
            row.getCell(1).setCellStyle(style);
            try (var out = Files.newOutputStream(dir.resolve("S07.xls"))) { book.write(out); }
        }
        try (var doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Denominación de Prueba Ñ");
            var table = doc.createTable(1, 3);
            table.getRow(0).getCell(0).setText("0012");
            table.getRow(0).getCell(1).setText("Descripción completa sin precio");
            table.getRow(0).getCell(2).setText("");
            try (var out = Files.newOutputStream(dir.resolve("S08.docx"))) { doc.write(out); }
        }
        try (var zip = new ZipOutputStream(Files.newOutputStream(dir.resolve("expansion.zip")))) {
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            byte[] block = new byte[4096];
            Arrays.fill(block, (byte) 'A');
            for (int i = 0; i < 512; i++) zip.write(block);
            zip.closeEntry();
        }
    }

    private static void read(Path dir) throws Exception {
        try (var doc = Loader.loadPDF(dir.resolve("S01-digital.pdf").toFile())) {
            var stripper = new PositionStripper();
            String result = stripper.getText(doc);
            check(result.contains("Proyecto de Prueba Ñ") && result.contains("0012 pieza 2 5.25 10.50"), "PDF digital text");
            check(stripper.positionCount > 30 && stripper.validCoordinates, "PDF text coordinates");
        }
        for (String name : new String[]{"S02-scan.pdf", "S03-mixed.pdf"}) {
            try (var doc = Loader.loadPDF(dir.resolve(name).toFile())) {
                String digital = new PDFTextStripper().getText(doc).trim();
                check(name.startsWith("S02") ? digital.isEmpty() : digital.equals("Encabezado digital"), name + " digital layer");
                // Deliberately OCR the image-bearing page even if it has a digital header.
                check(doc.getPage(0).getResources().getXObjectNames().iterator().hasNext(), name + " image region detected");
                validatePage(doc, 0);
                var renderer = new PDFRenderer(doc);
                renderer.setSubsamplingAllowed(true);
                Path png = Path.of("/tmp", name + ".png");
                var rendered = renderer.renderImageWithDPI(0, 200);
                ImageIO.write(rendered, "png", png.toFile());
                rendered.flush();
                var process = new ProcessBuilder("/ocr/usr/bin/tesseract", png.toString(), "stdout",
                        "--tessdata-dir", "/ocr/usr/share/tesseract-ocr/5/tessdata", "-l", "spa+eng", "--psm", "6", "tsv")
                        .redirectError(ProcessBuilder.Redirect.INHERIT).start();
                String tsv = new String(process.getInputStream().readNBytes(1_048_577), StandardCharsets.UTF_8);
                check(tsv.length() <= 1_048_576 && process.waitFor() == 0, name + " OCR bounded fixture result");
                var words = tsv.lines().skip(1).map(s -> s.split("\t", -1)).filter(p -> p.length == 12 && p[0].equals("5")).toList();
                String joined = String.join(" ", words.stream().map(p -> p[11]).toList());
                check(joined.contains("PRUEBA_01") && joined.contains("16/07/2025")
                        && joined.contains("0012") && joined.contains("10.50"), name + " OCR 4 annotated tokens");
                check(words.stream().allMatch(p -> Integer.parseInt(p[8]) > 0 && Integer.parseInt(p[9]) > 0), name + " OCR bounding boxes");
            }
        }
        try {
            try (var ignored = Loader.loadPDF(dir.resolve("S04-encrypted.pdf").toFile())) {
                throw new AssertionError("Encrypted PDF accepted");
            }
        } catch (InvalidPasswordException expected) { pass("PDF encrypted diagnosed"); }
        try {
            try (var ignored = Loader.loadPDF(dir.resolve("S04-damaged.pdf").toFile())) {
                throw new AssertionError("Damaged PDF accepted");
            }
        } catch (IOException expected) { pass("PDF damaged diagnosed"); }
        for (String name : new String[]{"oversized-page.pdf", "too-many-pages.pdf"}) {
            try (var doc = Loader.loadPDF(dir.resolve(name).toFile())) {
                try {
                    validatePage(doc, 0);
                    throw new AssertionError("Unbounded PDF accepted");
                } catch (IllegalArgumentException expected) { pass(name + " rejected before rendering"); }
            }
        }
        var formatter = new DataFormatter(java.util.Locale.ROOT);
        formatter.setUseCachedValuesForFormulaCells(true);
        try (var book = new XSSFWorkbook(OPCPackage.open(dir.resolve("S05-S06.xlsx").toFile(), PackageAccess.READ))) {
            check(book.getNumberOfSheets() == 4 && book.isSheetHidden(3), "XLSX sheet identities and hidden flag");
            check(book.getSheetAt(0).getNumMergedRegions() == 1 && book.getSheetAt(1).getRow(1).getZeroHeight(), "XLSX merged and hidden row flags");
            check(book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue().equals("0012"), "XLSX literal code");
            var cached = book.getSheetAt(0).getRow(1).getCell(5);
            check(cached.getCellType() == CellType.FORMULA && cached.getRawValue().equals("10.50"), "XLSX formula raw decimal cache");
            var missing = book.getSheetAt(1).getRow(1).getCell(5);
            check(missing.getRawValue() == null && !missing.getCTCell().isSetV(), "XLSX missing cache distinguished from zero");
            System.out.println("OBSERVATION absent-cache numeric getter=" + missing.getNumericCellValue() + "; do not use without presence check");
            var stale = book.getSheetAt(2).getRow(1).getCell(5);
            check(stale.getRawValue().equals("99.00") && !new BigDecimal(stale.getRawValue()).equals(new BigDecimal("10.50")), "XLSX stale declared cache preserved");
            check(book.getSheetAt(3).getRow(1).getCell(5).getRawValue().equals("0"), "XLSX explicit zero cache preserved");
        }
        try (var book = WorkbookFactory.create(dir.resolve("S07.xls").toFile(), null, true)) {
            var row = book.getSheetAt(0).getRow(0);
            check(book instanceof HSSFWorkbook && row.getCell(0).getStringCellValue().equals("0007"), "XLS binary literal code");
            check(formatter.formatCellValue(row.getCell(1)).equals("12.500"), "XLS format retained independently from numeric value");
        }
        try (var in = Files.newInputStream(dir.resolve("S08.docx")); var doc = new XWPFDocument(in)) {
            check(doc.getParagraphs().getFirst().getText().equals("Denominación de Prueba Ñ"), "DOCX source no markers");
            check(doc.getTables().getFirst().getRow(0).getCell(1).getText().equals("Descripción completa sin precio")
                    && doc.getTables().getFirst().getRow(0).getCell(2).getText().isEmpty(), "DOCX table description and missing price");
        }
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(20L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(512);
        try (var zip = new ZipSecureFile(dir.resolve("expansion.zip").toFile())) {
            try (var in = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))) {
                try {
                    in.transferTo(java.io.OutputStream.nullOutputStream());
                    throw new AssertionError("ZIP expansion accepted");
                } catch (IOException expected) { pass("POI compressed entry ratio rejected"); }
            }
        }
    }

    private static void validatePage(PDDocument doc, int page) {
        var box = doc.getPage(page).getCropBox();
        double width = Math.ceil(box.getWidth() * 200.0 / 72);
        double height = Math.ceil(box.getHeight() * 200.0 / 72);
        if (doc.getNumberOfPages() > MAX_PAGES || !Double.isFinite(width * height)
                || width <= 0 || height <= 0 || width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("PDF_RESOURCE_LIMIT");
        }
    }

    private static void addTextPage(PDDocument doc, String value) throws IOException {
        var page = new PDPage();
        doc.addPage(page);
        try (var content = new PDPageContentStream(doc, page)) { text(content, value, 50, 700); }
    }

    private static void text(PDPageContentStream content, String value, int x, int y) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        pass(label);
    }

    private static void pass(String label) { System.out.println("PASS " + label); }

    private static class PositionStripper extends PDFTextStripper {
        int positionCount;
        boolean validCoordinates = true;

        PositionStripper() throws IOException { }

        @Override
        protected void processTextPosition(TextPosition position) {
            positionCount++;
            validCoordinates &= position.getXDirAdj() >= 0 && position.getYDirAdj() >= 0
                    && position.getWidthDirAdj() >= 0;
            super.processTextPosition(position);
        }
    }
}
