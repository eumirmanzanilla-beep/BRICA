package com.brica.extraction.infrastructure.reader;

import com.brica.extraction.domain.SourceResult;
import com.brica.extraction.domain.SourceResult.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.filesystem.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

/** Adapter used ONLY by SourceWorker inside the OS sandbox, never by an HTTP thread. */
public final class SourceReader {
    private final long source;
    private final List<Fragment> fragments=new ArrayList<>();
    private final List<String> warnings=new ArrayList<>();
    private final AtomicInteger ocrPages;
    public SourceReader(long attachmentId) { this(attachmentId,new AtomicInteger()); }
    public SourceReader(long attachmentId,AtomicInteger jobOcrPages) { source=attachmentId; ocrPages=jobOcrPages; }
    public SourceResult read(Path path,String filename,String sha256) throws Exception {
        if(Files.size(path)>10*1024*1024 || Files.size(path)==0) { throw new IOException("SOURCE_SIZE_LIMIT"); }
        String lower=filename.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".pdf")) { pdf(path); }
        else if(lower.endsWith(".xlsx")) { zip(path,"xl/workbook.xml"); excel(path,false); }
        else if(lower.endsWith(".xls")) { excel(path,true); }
        else if(lower.endsWith(".docx")) { zip(path,"word/document.xml"); docx(path); }
        else { throw new IOException("UNSUPPORTED_SOURCE_FORMAT"); }
        return new SourceResult(source,sha256,"SOURCE_V1:PDFBOX_3.0.8_POI_6.0.0",fragments,warnings);
    }
    private void add(Location location,String text,String formula,String cached,String kind,boolean hidden,String warning) throws IOException {
        if(fragments.size()>=200_000 || text!=null && text.length()>16384 || cached!=null && cached.length()>16384 || formula!=null && formula.length()>16384) { throw new IOException("SOURCE_TEXT_LIMIT"); }
        fragments.add(new Fragment(UUID.randomUUID(),source,location,text,formula,cached,kind,hidden,warning));
    }
    private void zip(Path path,String required) throws Exception {
        var names=new HashSet<String>(); long expanded=0;
        try(var zip=new ZipInputStream(Files.newInputStream(path))) {
            for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) {
                String name=entry.getName(); String lower=name.toLowerCase(Locale.ROOT);
                if(name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains("..") || !names.add(name) || names.size()>512
                        || lower.contains("vbaproject") || lower.contains("embeddings/") || lower.contains("activex")) { throw new IOException("ACTIVE_OR_INVALID_CONTAINER"); }
                var special=new ByteArrayOutputStream(); boolean inspect=lower.endsWith(".rels") || name.equals("[Content_Types].xml");
                byte[] buffer=new byte[8192]; long entrySize=0;
                for(int n;(n=zip.read(buffer))!=-1;) {
                    expanded+=n; entrySize+=n;
                    if(expanded>20*1024*1024 || entrySize>20*1024*1024) { throw new IOException("ZIP_EXPANSION_LIMIT"); }
                    if(inspect) { special.write(buffer,0,n); }
                }
                if(inspect) {
                    if(inspectPackageXml(special.toByteArray(),required.equals("xl/workbook.xml")) && !warnings.contains("EXTERNAL_FORMULA_REFERENCES_NOT_EVALUATED")) {
                        warnings.add("EXTERNAL_FORMULA_REFERENCES_NOT_EVALUATED");
                    }
                }
            }
        }
        if(!names.contains("[Content_Types].xml") || !names.contains(required)) { throw new IOException("INVALID_OFFICE_CONTAINER"); }
        ZipSecureFile.setMinInflateRatio(0.01); ZipSecureFile.setMaxEntrySize(20*1024*1024); ZipSecureFile.setMaxFileCount(512);
    }
    private static boolean inspectPackageXml(byte[] bytes,boolean allowWorkbookCache) throws Exception {
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,""); factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        var document=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        boolean externalWorkbook=false;
        var relationships=document.getElementsByTagNameNS("*","Relationship");
        for(int i=0;i<relationships.getLength();i++) {
            var relationship=(org.w3c.dom.Element)relationships.item(i);
            if(relationship.getAttribute("TargetMode").equalsIgnoreCase("External")) {
                String type=relationship.getAttribute("Type");
                if(allowWorkbookCache && (type.equals("http://schemas.openxmlformats.org/officeDocument/2006/relationships/externalLinkPath")
                        || type.equals("http://purl.oclc.org/ooxml/officeDocument/relationships/externalLinkPath"))) { externalWorkbook=true; }
                else { throw new IOException("ACTIVE_OR_EXTERNAL_CONTENT"); }
            }
        }
        var types=document.getElementsByTagNameNS("*","*");
        for(int i=0;i<types.getLength();i++) {
            String type=((org.w3c.dom.Element)types.item(i)).getAttribute("ContentType").toLowerCase(Locale.ROOT);
            if(type.contains("macroenabled") || type.contains("vbaproject") || type.contains("macrosheet") || type.contains("activex") || type.contains("oleobject")) { throw new IOException("ACTIVE_OR_EXTERNAL_CONTENT"); }
        }
        return externalWorkbook;
    }
    private void excel(Path path,boolean legacy) throws Exception {
        if(legacy) {
            try(var fs=new POIFSFileSystem(path.toFile(),true)) { inspectOle(fs.getRoot(),0,new int[]{0}); }
        }
        try(Workbook book=legacy?new HSSFWorkbook(new POIFSFileSystem(path.toFile(),true)):new XSSFWorkbook(OPCPackage.open(path.toFile(),PackageAccess.READ))) {
            if(book.getNumberOfSheets()>50) { throw new IOException("SHEET_LIMIT"); }
            var formatter=new DataFormatter(Locale.ROOT); formatter.setUseCachedValuesForFormulaCells(true);
            int rows=0,cells=0;
            for(int s=0;s<book.getNumberOfSheets();s++) {
                var sheet=book.getSheetAt(s); boolean sheetHidden=book.isSheetHidden(s)||book.isSheetVeryHidden(s);
                add(new Location(null,sheet.getSheetName(),null,null,"sheet",null,null,null,null),sheet.getSheetName(),null,null,"SHEET",sheetHidden,null);
                for(var merged:sheet.getMergedRegions()) {
                    add(new Location(null,sheet.getSheetName(),merged.getFirstRow()+1,merged.getFirstColumn()+1,"merged",null,null,null,null),merged.formatAsString(),null,null,"MERGED",sheetHidden,null);
                }
                for(var row:sheet) {
                    if(++rows>20_000) { throw new IOException("ROW_LIMIT"); }
                    for(var cell:row) {
                        if(++cells>200_000 || cell.getColumnIndex()>=256) { throw new IOException("CELL_LIMIT"); }
                        String formula=null,cached=null,warning=null;
                        if(cell.getCellType()==CellType.FORMULA) {
                            formula=cell.getCellFormula();
                            if(cell instanceof XSSFCell x) {
                                if(x.getCTCell().isSetV()) { cached=x.getRawValue(); }
                                else { warning="MISSING_FORMULA_CACHE"; }
                            } else {
                                cached=stored(cell); warning="XLS_FORMULA_CACHE_UNVERIFIED";
                            }
                        } else { cached=stored(cell); }
                        String visible=warning!=null && cached==null ? null:formatter.formatCellValue(cell);
                        // Do not let DataFormatter fabricate zero for an absent formula cache.
                        add(new Location(null,sheet.getSheetName(),row.getRowNum()+1,cell.getColumnIndex()+1,
                                "cell:"+cell.getCellStyle().getDataFormatString(),null,null,null,null),visible,formula,cached,cell.getCellType().name(),sheetHidden||row.getZeroHeight()||sheet.isColumnHidden(cell.getColumnIndex()),warning);
                    }
                }
            }
            if(book instanceof XSSFWorkbook x) { warnings.add(x.isDate1904()?"EXCEL_DATE_SYSTEM_1904":"EXCEL_DATE_SYSTEM_1900"); }
            else if(book instanceof HSSFWorkbook h) { warnings.add(h.getWorkbook().isUsing1904DateWindowing()?"EXCEL_DATE_SYSTEM_1904":"EXCEL_DATE_SYSTEM_1900"); }
        }
    }
    private static String stored(Cell cell) {
        var type=cell.getCellType()==CellType.FORMULA?cell.getCachedFormulaResultType():cell.getCellType();
        return switch(type) {
            case STRING->cell.getStringCellValue();
            case NUMERIC->cell instanceof XSSFCell x?x.getRawValue():org.apache.poi.ss.util.NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN->Boolean.toString(cell.getBooleanCellValue());
            case ERROR->"#ERROR_"+cell.getErrorCellValue();
            default->null;
        };
    }
    private static void inspectOle(DirectoryEntry directory,int depth,int[] count) throws IOException {
        if(depth>4) { throw new IOException("OLE_DEPTH_LIMIT"); }
        for(var entry:directory) {
            if(++count[0]>512) { throw new IOException("OLE_ENTRY_LIMIT"); }
            String name=entry.getName().toUpperCase(Locale.ROOT);
            if(name.contains("VBA") || name.contains("MACRO") || name.equals("OBJECTPOOL")) { throw new IOException("ACTIVE_OLE_CONTENT"); }
            if(entry instanceof DirectoryEntry child) { inspectOle(child,depth+1,count); }
        }
    }
    private void docx(Path path) throws Exception {
        try(var in=Files.newInputStream(path);var doc=new XWPFDocument(in)) {
            int[] count={0,0}; readBody(doc.getBodyElements(),"word/document.xml",count);
            int n=0;
            for(var header:doc.getHeaderList()) { readBody(header.getBodyElements(),"header:"+(++n),count); }
            n=0; for(var footer:doc.getFooterList()) { readBody(footer.getBodyElements(),"footer:"+(++n),count); }
        }
    }
    private void readBody(List<IBodyElement> body,String part,int[] count) throws IOException {
        for(var element:body) {
            if(++count[0]>20_000) { throw new IOException("DOCX_BLOCK_LIMIT"); }
            if(element instanceof XWPFParagraph p) {
                add(new Location(null,null,null,null,part+"/paragraph:"+count[0],null,null,null,null),p.getText(),null,null,"TEXT",false,null);
            } else if(element instanceof XWPFTable table) {
                if(++count[1]>200) { throw new IOException("DOCX_TABLE_LIMIT"); }
                int row=0;
                for(var tr:table.getRows()) {
                    row++; int col=0;
                    for(var cell:tr.getTableCells()) {
                        if(++count[0]>20_000) { throw new IOException("DOCX_BLOCK_LIMIT"); }
                        add(new Location(null,part+"/table:"+count[1],row,++col,part,null,null,null,null),cell.getText(),null,null,"TEXT",false,null);
                        if(!cell.getTables().isEmpty()) { warnings.add("NESTED_DOCX_TABLE_REQUIRES_REVIEW"); }
                    }
                }
            }
        }
    }
    private void pdf(Path path) throws Exception {
        try(var doc=Loader.loadPDF(path.toFile())) {
            if(doc.isEncrypted() || doc.getNumberOfPages()>40) { throw new IOException("ENCRYPTED_OR_PDF_PAGE_LIMIT"); }
            var catalog=doc.getDocumentCatalog(); var names=catalog.getNames();
            if(catalog.getOpenAction()!=null || names!=null && (names.getJavaScript()!=null || names.getEmbeddedFiles()!=null)) { throw new IOException("ACTIVE_PDF_CONTENT"); }
            for(int page=0;page<doc.getNumberOfPages();page++) {
                int number=page+1; var pd=doc.getPage(page);
                double scale=200.0/72*pd.getUserUnit(); var box=pd.getCropBox();
                double pixels=Math.ceil(box.getWidth()*scale)*Math.ceil(box.getHeight()*scale);
                if(!Double.isFinite(pixels)||pixels<=0||pixels>12_000_000) { throw new IOException("PDF_PIXEL_LIMIT"); }
                boolean image=images(pd.getResources(),0,Collections.newSetFromMap(new IdentityHashMap<>()),new int[]{0});
                var digital=new ArrayList<String>();
                var stripper=new PDFTextStripper() {
                    @Override protected void writeString(String text,List<TextPosition> positions) throws IOException {
                        if(text.isBlank()) { return; }
                        digital.add(text.strip());
                        TextPosition first=positions.getFirst(); float right=first.getXDirAdj(),bottom=first.getYDirAdj();
                        for(var p:positions) { right=Math.max(right,p.getXDirAdj()+p.getWidthDirAdj()); bottom=Math.max(bottom,p.getYDirAdj()+p.getHeightDir()); }
                        add(new Location(number,null,null,null,"pdf:text",first.getXDirAdj(),first.getYDirAdj(),right-first.getXDirAdj(),bottom-first.getYDirAdj()),text,null,null,"TEXT",false,null);
                    }
                };
                stripper.setStartPage(number); stripper.setEndPage(number); stripper.getText(doc);
                if(image || digital.isEmpty()) {
                    if(ocrPages.incrementAndGet()>10) { warnings.add("OCR_PAGE_LIMIT:"+number); continue; }
                    try { ocr(doc,page,digital); } catch(Exception failure) { warnings.add("OCR_UNAVAILABLE_OR_UNREADABLE:"+number); }
                }
            }
        }
    }
    private static boolean images(PDResources resources,int depth,Set<Object> visited,int[] count) throws IOException {
        if(resources==null || !visited.add(resources.getCOSObject())) { return false; }
        if(depth>4) { throw new IOException("PDF_RESOURCE_DEPTH_LIMIT"); }
        boolean found=false;
        for(var name:resources.getXObjectNames()) {
            if(++count[0]>200) { throw new IOException("PDF_IMAGE_LIMIT"); }
            var object=resources.getXObject(name);
            if(object instanceof PDImageXObject image) {
                if(image.getWidth()<=0 || image.getHeight()<=0 || (long)image.getWidth()*image.getHeight()>12_000_000) { throw new IOException("PDF_IMAGE_PIXEL_LIMIT"); }
                found=true;
            } else if(object instanceof PDFormXObject form) { found|=images(form.getResources(),depth+1,visited,count); }
        }
        return found;
    }
    private void ocr(PDDocument doc,int page,List<String> digital) throws Exception {
        var renderer=new PDFRenderer(doc); renderer.setSubsamplingAllowed(true);
        Path png=Files.createTempFile("/tmp".equals(System.getProperty("java.io.tmpdir"))?Path.of("/tmp"):Path.of(System.getProperty("java.io.tmpdir")),"brica-ocr-",".png");
        Path tsv=Files.createTempFile(png.getParent(),"brica-ocr-",".tsv");
        Process process=null;
        try {
            var image=renderer.renderImageWithDPI(page,200); ImageIO.write(image,"png",png.toFile()); image.flush();
            process=new ProcessBuilder("/usr/bin/tesseract",png.toString(),"stdout","-l","spa+eng","--psm","6","tsv")
                    .redirectOutput(tsv.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(20,TimeUnit.SECONDS)) { throw new IOException("OCR_TIMEOUT"); }
            if(process.exitValue()!=0 || Files.size(tsv)>1_048_576) { throw new IOException("OCR_FAILED_OR_OUTPUT_LIMIT"); }
            var lines=new LinkedHashMap<String,List<String[]>>();
            try(var input=Files.lines(tsv)) {
                input.skip(1).forEach(line->{ var p=line.split("\t",-1); if(p.length==12 && p[0].equals("5") && !p[11].isBlank()) { lines.computeIfAbsent(String.join(":",Arrays.copyOfRange(p,1,5)),ignored->new ArrayList<>()).add(p); } });
            }
            for(var words:lines.values()) {
                String text=String.join(" ",words.stream().map(p->p[11]).toList());
                if(digital.stream().anyMatch(s->s.replaceAll("\\s+"," ").equals(text))) { continue; }
                int x=Integer.MAX_VALUE,y=Integer.MAX_VALUE,right=0,bottom=0; boolean low=false;
                for(var p:words) { int left=Integer.parseInt(p[6]),top=Integer.parseInt(p[7]); x=Math.min(x,left); y=Math.min(y,top); right=Math.max(right,left+Integer.parseInt(p[8])); bottom=Math.max(bottom,top+Integer.parseInt(p[9])); low|=Double.parseDouble(p[10])<80; }
                add(new Location(page+1,null,null,null,"pdf:ocr:200dpi",(float)x,(float)y,(float)(right-x),(float)(bottom-y)),text,null,null,"OCR",false,low?"LOW_OCR_CONFIDENCE":null);
            }
            if(lines.isEmpty()) { warnings.add("NO_OCR_TEXT:"+(page+1)); }
        } finally {
            if(process!=null && process.isAlive()) { process.destroyForcibly(); process.waitFor(3,TimeUnit.SECONDS); }
            Files.deleteIfExists(png); Files.deleteIfExists(tsv);
        }
    }
}
