package com.brica.documents.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.brica.documents.domain.*;

public interface DocumentConfigurationStore {
    DocumentTemplate template(Long id, boolean lock);
    List<DocumentTemplate> templates(int page, int size);
    java.util.Map<Long, Integer> latestNumbers(java.util.Collection<Long> templateIds);
    void add(Object entity);
    void flush();
    Optional<TemplateVersion> latest(Long templateId);
    TemplateVersion version(TemplateVersionId id);
    List<TemplateVersion> versions(Long templateId);
    List<TemplateVersionField> versionFields(TemplateVersionId id);
    DocumentFile file(Long id);
    Optional<RegistrationOperation> registration(UUID id, boolean lock);
    DocumentPackage documentPackage(Long id, boolean lock);
    List<DocumentPackage> packages(int page, int size, Long clientId, boolean confirmedOnly);
    void discard(DocumentPackage documentPackage);
    boolean activeClient(Long id);
    Optional<Long> workClient(Long workId);
}
