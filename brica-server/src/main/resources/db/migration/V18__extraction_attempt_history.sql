ALTER TABLE extraction_jobs ADD COLUMN parent_job_id UUID;
ALTER TABLE extraction_jobs ADD COLUMN review_closed_by VARCHAR(200);
ALTER TABLE extraction_jobs ADD COLUMN review_closed_at TIMESTAMPTZ;
ALTER TABLE extraction_jobs ADD CONSTRAINT fk_extraction_parent_work
    FOREIGN KEY(parent_job_id,work_id) REFERENCES extraction_jobs(id,work_id);
ALTER TABLE extraction_jobs ADD CONSTRAINT ck_extraction_not_own_parent CHECK(parent_job_id IS NULL OR parent_job_id<>id);

CREATE TABLE extraction_attempts (
    id UUID PRIMARY KEY, job_id UUID NOT NULL REFERENCES extraction_jobs(id),
    attempt_number INTEGER NOT NULL CHECK(attempt_number>0),
    outcome VARCHAR(20) NOT NULL CHECK(outcome IN ('RUNNING','COMPLETED','PARTIAL','FAILED','CANCELLED','EXPIRED','LEGACY_UNKNOWN')),
    started_at TIMESTAMPTZ, heartbeat_at TIMESTAMPTZ, finished_at TIMESTAMPTZ,
    diagnostic VARCHAR(1000), result_hash VARCHAR(64), reader_version VARCHAR(400),
    legacy_imported BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(job_id,attempt_number), CHECK(legacy_imported OR started_at IS NOT NULL),
    CHECK(legacy_imported OR outcome='RUNNING' OR finished_at IS NOT NULL)
);
-- Preserve pre-history attempts without inventing timestamps or lost retry outcomes.
INSERT INTO extraction_attempts(id,job_id,attempt_number,outcome,diagnostic,reader_version,legacy_imported)
SELECT coalesce(attempt_token,gen_random_uuid()),id,attempt_number,
       CASE WHEN state='QUEUED' THEN 'LEGACY_UNKNOWN' ELSE state END,
       diagnostic,CASE WHEN length(result_data->0->>'readerVersion')<=400 THEN result_data->0->>'readerVersion' ELSE NULL END,TRUE
FROM extraction_jobs WHERE attempt_number>0;

CREATE FUNCTION extraction_attempt_history_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' OR OLD.outcome<>'RUNNING' THEN
        RAISE EXCEPTION 'Finished extraction attempt history is immutable';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_extraction_attempt_history BEFORE UPDATE OR DELETE ON extraction_attempts
FOR EACH ROW EXECUTE FUNCTION extraction_attempt_history_guard();
