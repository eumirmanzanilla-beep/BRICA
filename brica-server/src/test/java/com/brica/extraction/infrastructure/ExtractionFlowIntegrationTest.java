package com.brica.extraction.infrastructure;

import com.brica.budgets.application.BudgetService;
import com.brica.budgets.domain.BudgetDraft;
import com.brica.clients.application.*;
import com.brica.extraction.application.ExtractionService;
import com.brica.extraction.application.port.SourceProcessor;
import com.brica.extraction.domain.*;
import com.brica.extraction.infrastructure.worker.ExtractionDispatcher;
import com.brica.server.security.application.*;
import com.brica.works.application.*;
import com.brica.works.domain.WorkAttachment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.nio.file.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties="brica.extraction.worker-enabled=false") @Testcontainers
class ExtractionFlowIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:18-alpine");
    @TempDir static Path files;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",postgres::getJdbcUrl); registry.add("spring.datasource.username",postgres::getUsername);
        registry.add("spring.datasource.password",postgres::getPassword); registry.add("brica.documents.storage-root",()->files.toString());
    }
    @MockitoBean AccessPolicy access;
    @MockitoBean com.brica.server.security.application.port.AccountStore accounts;
    @Autowired RegisterClientService clients;
    @Autowired RegisterWorkService works;
    @Autowired WorkAttachmentService attachments;
    @Autowired ExtractionService extraction;
    @Autowired BudgetService budgets;
    @Autowired ConsultWorkService consultWork;
    @Autowired UpdateClientService updateClient;
    @Autowired ConsultClientService consultClient;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.brica.extraction.application.ExtractionExecutionService execution;
    @Autowired SourceProcessor processor;
    private Fixture activeFixture;
    @BeforeEach void permissions() {
        when(access.hasPermission(anyString())).thenReturn(true);
        var actor=new AccountAccess(1L,"reviewer","Reviewer",1L,"Gerencial",true,Set.of("WORKS_READ","WORKS_WRITE","CLIENTS_READ","CLIENTS_WRITE"));
        when(access.current()).thenReturn(actor);
        when(accounts.findAccess("reviewer")).thenReturn(Optional.of(actor));
    }
    @AfterEach void cancelPendingFixture() {
        permissions();
        if(activeFixture!=null) {
            var current=extraction.get(activeFixture.job().job().id());
            if(current.job().reviewState().equals("DRAFT")) { extraction.cancel(current.job().id(),current.job().revision()); }
        }
    }
    private record Fixture(Long work,Long client,ExtractionService.View job) { }
    private Fixture fixture() {
        return fixture("%PDF-1.7\nSynthetic manual evidence".getBytes(java.nio.charset.StandardCharsets.US_ASCII),"manual.pdf",WorkAttachment.Kind.CONTRACT);
    }
    private Fixture fixture(byte[] bytes,String name,WorkAttachment.Kind kind) {
        var c=clients.registerClient("Cliente_"+UUID.randomUUID().toString().substring(0,8),"Representante original");
        var w=works.registerWork(c.getId(),"Obra_"+UUID.randomUUID().toString().substring(0,8),null,null,null,null);
        var a=attachments.upload(w.getId(),kind,name,bytes);
        UUID key=UUID.randomUUID(); var request=new ExtractionService.Create(key,List.of(a.id()));
        var job=extraction.create(w.getId(),request);
        assertEquals(job.job().id(),extraction.create(w.getId(),request).job().id());
        activeFixture=new Fixture(w.getId(),c.getId(),job); return activeFixture;
    }
    @Test void publishesWordParagraphsAndTablesForReview() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        try(var doc=new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            doc.createParagraph().createRun().setText("Número de contrato: SINTETICO-01");
            doc.createTable(1,1).getRow(0).getCell(0).setText("Concepto sintético");
            doc.write(bytes);
        }
        var fragments=readAndPublish(fixture(bytes.toByteArray(),"contrato.docx",WorkAttachment.Kind.CONTRACT));
        assertTrue(fragments.stream().anyMatch(f->f.original().contains("SINTETICO-01") && f.location().part().contains("/paragraph:")));
        assertTrue(fragments.stream().anyMatch(f->f.original().equals("Concepto sintético") && f.location().row()==1 && f.location().page()==null));
    }
    @Test @EnabledOnOs(OS.WINDOWS)
    @EnabledIfSystemProperty(named="brica.test.windows.packaged",matches="true")
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    @Timeout(value=45,threadMode=Timeout.ThreadMode.SEPARATE_THREAD)
    void windowsDispatcherPublishesContractEvidenceAndSuggestionsWithoutApplyingThem() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        try(var doc=new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            doc.createParagraph().createRun().setText("Número de contrato: 0012");
            var row=doc.createTable(1,2).getRow(0);
            row.getCell(0).setText("Contratista:");
            row.getCell(1).setText("Empresa de prueba");
            doc.write(bytes);
        }
        var f=fixture(bytes.toByteArray(),"contrato.docx",WorkAttachment.Kind.CONTRACT);
        var id=f.job().job().id();
        var jar=Path.of("target/brica-server-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar));
        String previous=System.getProperty("java.class.path");
        try {
            System.setProperty("java.class.path",jar.toString());
            new ExtractionDispatcher(execution,processor,true).dispatch();
        } finally { System.setProperty("java.class.path",previous); }
        var completed=extraction.get(id);
        assertEquals("COMPLETED",completed.job().state(),completed.job().diagnostic());
        assertEquals("DRAFT",completed.job().reviewState());
        assertTrue(completed.review().decisions().isEmpty());
        var fragments=extraction.fragments(id,0,10);
        var suggestions=extraction.suggestions(id,0,10);
        assertTrue(suggestions.stream().anyMatch(s->s.target().equals("WORK_CONTRACT_NUMBER") && s.proposedValue().equals("0012")));
        var contractor=suggestions.stream().filter(s->s.target().equals("WORK_CONTRACTOR")).findFirst().orElseThrow();
        assertEquals("Empresa de prueba",contractor.proposedValue());
        assertTrue(fragments.stream().anyMatch(frag->frag.id().equals(contractor.fragmentId()) && frag.location().column()==2));
        assertNull(consultWork.get(f.work()).getContractNumber());
        assertNull(consultWork.get(f.work()).getContractor());
    }
    @Test void publishesExcelSheetCellsAndIdentifiesEvidence() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            workbook.createSheet("Parte A").createRow(0).createCell(0).setCellValue("0012");
            workbook.write(bytes);
        }
        var fragments=readAndPublish(fixture(bytes.toByteArray(),"presupuesto.xlsx",WorkAttachment.Kind.BASE_BUDGET));
        assertTrue(fragments.stream().anyMatch(f->f.kind().equals("SHEET") && f.original().equals("Parte A")));
        assertTrue(fragments.stream().anyMatch(f->f.original().equals("0012") && f.location().row()==1 && f.location().column()==1));
    }
    private List<SourceResult.Fragment> readAndPublish(Fixture fixture) throws Exception {
        var claim=execution.claim();
        assertNotNull(claim);
        assertEquals(fixture.job().job().id(),claim.jobId());
        var source=claim.sources().getFirst();
        // Synthetic inputs only: exercise the adapter and durable flow, not OS isolation.
        var path=Files.createTempFile(files,"synthetic-source-",".bin");
        try {
            Files.write(path,source.bytes());
            var result=new com.brica.extraction.infrastructure.reader.SourceReader(source.id()).read(path,source.name(),source.sha256());
            execution.publish(claim,new com.brica.extraction.application.port.SourceProcessor.Result(List.of(result),null));
        } finally { Files.deleteIfExists(path); }
        var current=extraction.get(claim.jobId());
        assertEquals("COMPLETED",current.job().state());
        assertEquals("DRAFT",current.job().reviewState());
        assertEquals(0,current.job().confirmedBlocks());
        assertNull(consultWork.get(fixture.work()).getDocumentName());
        return extraction.fragments(claim.jobId(),0,10);
    }
    @Test void savesResumesAndConfirmsWorkWithoutOverwritingTechnicalNameOrClient() {
        var f=fixture(); String originalName=consultWork.get(f.work()).getName();
        var decision=new ExtractionReview.Decision("WORK_DOCUMENT_NAME","Construcción de fachada — Proyecto Ñ",null,"Captura manual cotejada con la fuente",false);
        var saved=extraction.save(f.job().job().id(),f.job().job().revision(),new ExtractionReview(f.job().review().workRevision(),f.job().review().clientRevision(),List.of(decision),List.of()));
        assertEquals(saved.review(),extraction.get(saved.job().id()).review());
        assertNull(consultWork.get(f.work()).getDocumentName());
        var request=new ExtractionService.Confirm(UUID.randomUUID(),"WORK",null);
        var confirmed=extraction.confirm(saved.job().id(),saved.job().revision(),request);
        assertEquals(confirmed.id(),extraction.confirm(saved.job().id(),saved.job().revision(),request).id());
        assertEquals(originalName,consultWork.get(f.work()).getName());
        assertEquals(decision.value(),consultWork.get(f.work()).getDocumentName());
        assertNull(consultClient.get(f.client()).getDocumentName());
        var current=extraction.get(saved.job().id());
        assertEquals(1,current.job().confirmedBlocks());
        var cancelled=extraction.cancel(current.job().id(),current.job().revision());
        assertEquals("CANCELLED",cancelled.job().reviewState());
        assertEquals(decision.value(),consultWork.get(f.work()).getDocumentName());
    }
    @Test void confirmsIndependentImmutableBudgetWithRepeatedCodesMissingPricesAndExplicitPrincipal() {
        var f=fixture();
        var items=List.of(new BudgetDraft.Item(UUID.randomUUID(),0,null,"0012","Concepto completo sin precio","pieza",new BigDecimal("2"),null,null,null,"2 piezas"),
                new BudgetDraft.Item(UUID.randomUUID(),1,null,"0012","Otra fila independiente",null,null,null,null,null,null));
        var draft=new BudgetDraft(UUID.randomUUID(),null,null,"Presupuesto de prueba",null,List.of(),items,List.of("Sin IVA declarado"),null,null,null,"Captura manual verificada con documento");
        var review=new ExtractionReview(f.job().review().workRevision(),f.job().review().clientRevision(),List.of(),List.of(draft));
        var saved=extraction.save(f.job().job().id(),f.job().job().revision(),review);
        var key=new ExtractionService.Confirm(UUID.randomUUID(),"BUDGET",draft.blockId());
        var result=extraction.confirm(saved.job().id(),saved.job().revision(),key);
        assertEquals(result.id(),extraction.confirm(saved.job().id(),saved.job().revision(),key).id());
        var budget=budgets.list(f.work(),0,10).getFirst(); assertNull(budget.principalRevisionId());
        var revision=budgets.revisions(budget.id(),0,10).getFirst();
        var confirmed=budgets.items(revision.id(),0,10); assertEquals(2,confirmed.size());
        assertEquals("0012",confirmed.get(0).code()); assertEquals("0012",confirmed.get(1).code());
        assertNotEquals(confirmed.get(0).id(),confirmed.get(1).id()); assertNull(confirmed.getFirst().price());
        assertEquals(revision.id(),budgets.principal(budget.id(),revision.id(),budget.revision()).principalRevisionId());
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update budget_items set price=0 where id=?",confirmed.getFirst().id()));
        assertNull(budgets.items(revision.id(),0,10).getFirst().price());
    }
    @Test void detectsConcurrentClientWriterAndDoesNotConfirmStaleExtraction() {
        var f=fixture();
        var decision=new ExtractionReview.Decision("CLIENT_DOCUMENT_NAME","Cliente documental propuesto",null,"Captura manual cotejada con contrato",false);
        var saved=extraction.save(f.job().job().id(),f.job().job().revision(),new ExtractionReview(f.job().review().workRevision(),f.job().review().clientRevision(),List.of(decision),List.of()));
        var c=consultClient.get(f.client()); updateClient.update(c.getId(),ClientRevision.etag(c),c.getName(),"Representante cambiado por otro escritor");
        var failure=assertThrows(com.brica.documents.application.DocumentException.class,()->extraction.confirm(saved.job().id(),saved.job().revision(),new ExtractionService.Confirm(UUID.randomUUID(),"CLIENT",null)));
        assertEquals(412,failure.status()); assertNull(consultClient.get(f.client()).getDocumentName());
        assertEquals(0,extraction.get(saved.job().id()).job().confirmedBlocks());
    }
    @Test void persistsTechnicalAttemptHeartbeatAndReanalysisWithoutConfirmingBusinessData() {
        var f=fixture(); var claim=execution.claim(); assertNotNull(claim); assertEquals(f.job().job().id(),claim.jobId());
        String etag=extraction.get(claim.jobId()).job().revision(); claim.nextHeartbeat().set(0);
        assertTrue(execution.active(claim)); assertEquals(etag,extraction.get(claim.jobId()).job().revision(),"A lease heartbeat must not invalidate a saved review ETag");
        var source=claim.sources().getFirst();
        var fragment=new SourceResult.Fragment(UUID.randomUUID(),source.id(),new SourceResult.Location(1,null,null,null,"pdf:text",1f,2f,20f,5f),"Propuesta no confirmada",null,null,"TEXT",false,null);
        execution.publish(claim,new com.brica.extraction.application.port.SourceProcessor.Result(List.of(new SourceResult(source.id(),source.sha256(),"TEST_READER_V1",List.of(fragment),List.of())),null));
        var history=extraction.attempts(claim.jobId(),0,10); assertEquals(1,history.size());
        assertEquals("COMPLETED",history.getFirst().outcome()); assertNotNull(history.getFirst().resultHash()); assertFalse(history.getFirst().legacyImported());
        assertNull(consultWork.get(f.work()).getDocumentName());
        var next=extraction.reanalyze(claim.jobId(),UUID.randomUUID());
        assertEquals(claim.jobId(),next.job().parentJobId()); assertEquals("QUEUED",next.job().state());
        assertTrue(next.review().decisions().isEmpty()); assertEquals(1,extraction.fragments(claim.jobId(),0,10).size());
        extraction.cancel(next.job().id(),next.job().revision());
        assertEquals("COMPLETED",extraction.attempts(claim.jobId(),0,10).getFirst().outcome());
    }
}
