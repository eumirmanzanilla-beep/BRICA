ALTER TABLE template_versions ADD COLUMN current_version BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE document_packages ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

-- Preserve incomplete legacy captures until they are edited with a valid context.
UPDATE document_packages SET state = 'CONFIRMED'
WHERE state = 'DRAFT' AND client_id IS NOT NULL AND name IS NOT NULL AND length(btrim(name)) > 0;

DROP INDEX uq_package_client_name;
DROP INDEX uq_package_work_name;
CREATE UNIQUE INDEX uq_package_client_name ON document_packages(client_id, name)
    WHERE work_id IS NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_package_work_name ON document_packages(work_id, name)
    WHERE work_id IS NOT NULL AND deleted_at IS NULL;
