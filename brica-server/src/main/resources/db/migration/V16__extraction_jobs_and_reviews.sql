ALTER TABLE works ADD CONSTRAINT uq_works_client_identity UNIQUE (id_work,client_id);
ALTER TABLE work_attachments ADD CONSTRAINT uq_attachment_work_identity UNIQUE (id_attachment,work_id);
CREATE TABLE extraction_jobs (
    id UUID PRIMARY KEY, revision BIGINT NOT NULL DEFAULT 0,
    work_id BIGINT NOT NULL, client_id BIGINT NOT NULL,
    request_key UUID NOT NULL, request_hash VARCHAR(64) NOT NULL, actor VARCHAR(200) NOT NULL,
    state VARCHAR(20) NOT NULL CHECK (state IN ('QUEUED','RUNNING','COMPLETED','PARTIAL','FAILED','CANCELLED')),
    review_state VARCHAR(20) NOT NULL CHECK (review_state IN ('DRAFT','CONFIRMED','CANCELLED')),
    review_data JSONB NOT NULL CHECK (jsonb_typeof(review_data)='object' AND octet_length(review_data::text)<=1048576),
    result_data JSONB NOT NULL CHECK (jsonb_typeof(result_data)='array' AND octet_length(result_data::text)<=1048576),
    diagnostic VARCHAR(1000), attempt_token UUID, attempt_number INTEGER NOT NULL DEFAULT 0,
    confirmed_blocks INTEGER NOT NULL DEFAULT 0 CHECK(confirmed_blocks BETWEEN 0 AND 22),
    lease_until TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_extraction_work_client FOREIGN KEY (work_id,client_id) REFERENCES works(id_work,client_id),
    CONSTRAINT uq_extraction_work UNIQUE(id,work_id),
    CONSTRAINT uq_extraction_request UNIQUE(work_id,actor,request_key)
);
CREATE INDEX ix_extraction_jobs_work_created ON extraction_jobs(work_id,created_at DESC,id);
CREATE INDEX ix_extraction_jobs_queue ON extraction_jobs(state,lease_until,created_at) WHERE review_state='DRAFT';
CREATE TABLE extraction_sources (
    id UUID PRIMARY KEY, job_id UUID NOT NULL, attachment_id BIGINT NOT NULL,
    work_id BIGINT NOT NULL, source_order INTEGER NOT NULL CHECK(source_order BETWEEN 0 AND 3), sha256 VARCHAR(64) NOT NULL,
    UNIQUE(job_id,source_order), UNIQUE(job_id,attachment_id),
    FOREIGN KEY(job_id,work_id) REFERENCES extraction_jobs(id,work_id),
    FOREIGN KEY(attachment_id,work_id) REFERENCES work_attachments(id_attachment,work_id)
);
CREATE TABLE extraction_confirmations (
    id UUID PRIMARY KEY, job_id UUID NOT NULL REFERENCES extraction_jobs(id),
    operation_key UUID NOT NULL, request_hash VARCHAR(64) NOT NULL, block_key VARCHAR(100) NOT NULL,
    actor VARCHAR(200) NOT NULL, result VARCHAR(1000) NOT NULL,
    audit_json TEXT NOT NULL CHECK(octet_length(audit_json)<=2097152), confirmed_at TIMESTAMPTZ NOT NULL,
    UNIQUE(job_id,operation_key), UNIQUE(job_id,block_key), UNIQUE(id,job_id)
);
CREATE TABLE extraction_mapping_profiles (
    id UUID PRIMARY KEY, work_id BIGINT NOT NULL REFERENCES works(id_work),
    name VARCHAR(100) NOT NULL, version INTEGER NOT NULL CHECK(version>0),
    mapping JSONB NOT NULL CHECK(jsonb_typeof(mapping)='object' AND octet_length(mapping::text)<=16384),
    actor VARCHAR(200) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(work_id,name,version)
);
CREATE FUNCTION extraction_immutable_record() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Confirmed evidence and revisions are immutable'; END $$;
CREATE TRIGGER immutable_extraction_confirmation BEFORE UPDATE OR DELETE ON extraction_confirmations
FOR EACH ROW EXECUTE FUNCTION extraction_immutable_record();
CREATE TRIGGER immutable_extraction_source BEFORE UPDATE OR DELETE ON extraction_sources
FOR EACH ROW EXECUTE FUNCTION extraction_immutable_record();
CREATE TRIGGER immutable_mapping_profile BEFORE UPDATE OR DELETE ON extraction_mapping_profiles
FOR EACH ROW EXECUTE FUNCTION extraction_immutable_record();
