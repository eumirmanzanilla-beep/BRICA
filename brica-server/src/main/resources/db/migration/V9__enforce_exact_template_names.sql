-- Exact, case-sensitive comparison. Existing duplicates cause migration to fail
-- rather than being silently renamed or deleted.
CREATE UNIQUE INDEX uq_document_templates_exact_name
    ON document_templates (name COLLATE "C");
