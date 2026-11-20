package com.brica.extraction.application.port;

import com.brica.extraction.domain.*;
import java.util.*;

public interface ExtractionStore {
    ExtractionJob save(ExtractionJob job);
    Optional<ExtractionJob> find(UUID id, boolean lock);
    Optional<ExtractionJob> request(Long workId, String actor, UUID key);
    List<ExtractionJob> list(Long workId, int page, int size);
    List<UUID> available();
    boolean heartbeat(UUID jobId,UUID token);
    void startAttempt(ExtractionAttempt attempt);
    void finishAttempt(UUID token,ExtractionAttempt.Outcome outcome,String diagnostic,String hash,String readerVersion);
    List<ExtractionAttempt> attempts(UUID jobId,int page,int size);
    void addSource(ExtractionSource source);
    List<ExtractionSource> sources(UUID jobId);
    List<ExtractionConfirmation> confirmations(UUID jobId);
    void confirm(ExtractionConfirmation confirmation);
    List<MappingProfile> profiles(Long workId,int page,int size);
    int profileVersion(Long workId,String name);
    void profile(MappingProfile profile);
}
