package com.brica.documents.application.port;

import com.brica.documents.domain.DocxAnalysis;

public interface DocxAnalyzer {
    DocxAnalysis analyze(byte[] content);
}
