package com.brica.extraction.application;
import com.brica.extraction.application.port.*;
import com.brica.extraction.domain.*;
import com.brica.server.security.application.*;
import com.brica.server.security.application.port.AccountStore;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;
import com.brica.clients.domain.Client;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractionExecutionServiceTest {
    private final ExtractionStore store=mock(ExtractionStore.class);
    private final SourceEvidenceService evidence=mock(SourceEvidenceService.class);
    private final AccountStore accounts=mock(AccountStore.class);
    private final WorkStore works=mock(WorkStore.class);
    private final ExtractionExecutionService execution=new ExtractionExecutionService(store,evidence,accounts,works,tools.jackson.databind.json.JsonMapper.builder().build());
    private ExtractionJob job; private ExtractionExecutionService.Claim claim;
    @BeforeEach void prepare() {
        job=new ExtractionJob(1L,2L,UUID.randomUUID(),"a".repeat(64),"reviewer",ExtractionReview.empty("w","c"));
        UUID token=job.claim(); claim=new ExtractionExecutionService.Claim(job.getId(),token,List.of(new SourceEvidenceService.Content(7L,"source.pdf","CONTRACT","b".repeat(64),new byte[]{1})),new AtomicLong(Long.MAX_VALUE));
        when(store.find(job.getId(),false)).thenReturn(Optional.of(job)); when(store.find(job.getId(),true)).thenReturn(Optional.of(job));
        when(accounts.findAccess("reviewer")).thenReturn(Optional.of(new AccountAccess(1L,"reviewer","Reviewer",1L,"Admin",true,Set.of("WORKS_READ","WORKS_WRITE"))));
        var w=mock(Work.class); var c=mock(Client.class); when(c.getId()).thenReturn(2L); when(w.getClient()).thenReturn(c); when(works.findById(1L)).thenReturn(Optional.of(w));
    }
    @Test void omittedSourceCannotPublishSuccessfulEmptyResult() {
        execution.publish(claim,new SourceProcessor.Result(List.of(),null));
        assertEquals(ExtractionJob.State.FAILED,job.getState()); assertNotNull(job.getDiagnostic());
    }
    @Test void duplicateFragmentIdsInvalidateTheWholeWorkerProtocol() {
        var id=UUID.randomUUID(); var location=new SourceResult.Location(1,null,null,null,"pdf:text",null,null,null,null);
        var fragment=new SourceResult.Fragment(id,7,location,"x",null,null,"TEXT",false,null);
        execution.publish(claim,new SourceProcessor.Result(List.of(new SourceResult(7,"b".repeat(64),"v1",List.of(fragment,fragment),List.of())),null));
        assertEquals(ExtractionJob.State.FAILED,job.getState()); assertTrue(job.getResults().isEmpty());
    }
    @Test void resultFromAnotherAttachmentNeverBecomesEvidenceOfThisJob() {
        execution.publish(claim,new SourceProcessor.Result(List.of(new SourceResult(99,"c".repeat(64),"v1",List.of(),List.of())),null));
        assertEquals(ExtractionJob.State.FAILED,job.getState()); assertTrue(job.getResults().isEmpty());
    }
    @Test void cancellationInvalidatesTheTokenAndPreventsAnyPublication() {
        job.cancel("reviewer"); assertFalse(execution.active(claim));
        execution.publish(claim,new SourceProcessor.Result(List.of(),"late"));
        assertEquals(ExtractionJob.State.CANCELLED,job.getState()); verify(store,never()).save(any());
    }
    @Test void heartbeatChecksOwnerAndPermissionsBeforeRenewingLease() {
        claim.nextHeartbeat().set(0); when(store.heartbeat(job.getId(),claim.token())).thenReturn(true);
        assertTrue(execution.active(claim)); verify(store).heartbeat(job.getId(),claim.token());
        when(accounts.findAccess("reviewer")).thenReturn(Optional.empty()); claim.nextHeartbeat().set(0);
        assertFalse(execution.active(claim)); verify(store,times(1)).heartbeat(any(),any());
    }
}
