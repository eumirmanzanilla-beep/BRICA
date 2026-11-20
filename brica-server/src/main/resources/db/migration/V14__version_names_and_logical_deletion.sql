ALTER TABLE template_versions ADD COLUMN name VARCHAR(150);
UPDATE template_versions SET name = 'Version_' || version_number;
ALTER TABLE template_versions ALTER COLUMN name SET NOT NULL;
ALTER TABLE template_versions ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE document_registrations ADD COLUMN version_name VARCHAR(150);
UPDATE document_registrations SET version_name = 'Version_' || COALESCE(version_number::text, 'Pendiente');
ALTER TABLE document_registrations ALTER COLUMN version_name SET NOT NULL;
ALTER TABLE work_attachments ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX template_versions_available_idx ON template_versions(template_id, version_number DESC) WHERE deleted_at IS NULL;
