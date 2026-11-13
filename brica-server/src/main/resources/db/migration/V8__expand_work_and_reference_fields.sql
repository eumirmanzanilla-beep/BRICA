ALTER TABLE works
    ADD COLUMN contract_number VARCHAR(50),
    ADD COLUMN contractor VARCHAR(200),
    ADD COLUMN contracted_amount NUMERIC,
    ADD COLUMN amount_before_tax NUMERIC,
    ADD COLUMN tax_amount NUMERIC,
    ADD COLUMN actual_end_date DATE,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN additional_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT ck_works_status CHECK (status IN ('IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
    ADD CONSTRAINT ck_works_additional_values CHECK (jsonb_typeof(additional_values) = 'object');

ALTER TABLE reference_fields DROP CONSTRAINT ck_reference_fields_source;
ALTER TABLE reference_fields ADD CONSTRAINT ck_reference_fields_source CHECK (
    (origin = 'CLIENT' AND data_type = 'TEXT' AND source_path IN ('id', 'name', 'legalRepresentative')) OR
    (origin = 'WORK' AND (
        (data_type = 'TEXT' AND source_path IN ('id', 'client.id', 'name', 'contractNumber', 'contractor', 'location', 'description', 'status')) OR
        (data_type = 'DATE' AND source_path IN ('startDate', 'endDate', 'actualEndDate')) OR
        (data_type = 'AMOUNT' AND source_path IN ('contractedAmount', 'amountBeforeTax', 'taxAmount'))
    )) OR
    (origin = 'ADDITIONAL' AND data_type IN ('TEXT', 'DATE', 'AMOUNT') AND source_path ~ '^[A-Z0-9_]{1,50}$')
);
