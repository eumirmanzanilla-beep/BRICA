package com.brica.extraction.domain;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionJobTest {
    private ExtractionJob job() { return new ExtractionJob(1L,2L,UUID.randomUUID(),"a".repeat(64),"reviewer",ExtractionReview.empty("work","client")); }
    @Test void rejectsLateResultOfExpiredAttemptAndAllowsOnlyCurrentLease() {
        var job=job(); UUID old=job.claim();
        ReflectionTestUtils.setField(job,"leaseUntil",OffsetDateTime.now().minusSeconds(1));
        UUID current=job.claim(); assertNotEquals(old,current); assertFalse(job.owns(old));
        job.finish(old,List.of(),"late"); assertEquals(ExtractionJob.State.RUNNING,job.getState()); assertNull(job.getDiagnostic());
        job.finish(current,List.of(),"No readable sources"); assertEquals(ExtractionJob.State.FAILED,job.getState()); assertEquals(2,job.getAttemptNumber());
    }
    @Test void closingOrCancellingManualReviewStopsTechnicalWorkWithoutRevertingConfirmedBlocks() {
        var closed=job(); UUID token=closed.claim(); closed.recordConfirmation(); closed.closeReview("reviewer");
        closed.finish(token,List.of(),null);
        assertEquals(ExtractionJob.State.CANCELLED,closed.getState()); assertEquals(ExtractionJob.ReviewState.CONFIRMED,closed.getReviewState()); assertEquals(1,closed.getConfirmedBlocks());
        var cancelled=job(); cancelled.claim(); cancelled.recordConfirmation(); cancelled.cancel("reviewer");
        assertEquals("reviewer",cancelled.getReviewClosedBy()); assertNotNull(cancelled.getReviewClosedAt());
        assertEquals(1,cancelled.getConfirmedBlocks()); assertThrows(com.brica.server.shared.validation.domain.InvalidDataException.class,()->cancelled.saveReview(ExtractionReview.empty("new","new")));
    }
}
