package com.brica.extraction.application.port;
import com.brica.extraction.application.SourceEvidenceService.Content;
import com.brica.extraction.domain.SourceResult;
import java.util.*;
import java.util.function.BooleanSupplier;

public interface SourceProcessor {
    record Result(List<SourceResult> sources,String diagnostic) { }
    Result process(List<Content> sources,BooleanSupplier active) throws Exception;
}
