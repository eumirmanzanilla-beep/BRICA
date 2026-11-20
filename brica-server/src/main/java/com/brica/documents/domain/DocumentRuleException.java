package com.brica.documents.domain;

/** A business conflict, independent of HTTP and infrastructure. */
public class DocumentRuleException extends RuntimeException {
    private final String code;
    public DocumentRuleException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
