ALTER TABLE works ADD COLUMN document_name VARCHAR(1000);
ALTER TABLE cliente ADD COLUMN document_name VARCHAR(1000);
ALTER TABLE cliente ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

ALTER TABLE reference_fields DROP CONSTRAINT ck_reference_fields_source;
ALTER TABLE reference_fields ADD CONSTRAINT ck_reference_fields_source CHECK (
    (origin = 'CLIENT' AND data_type = 'TEXT' AND source_path IN ('id','name','legalRepresentative','documentName')) OR
    (origin = 'WORK' AND (
        (data_type = 'TEXT' AND source_path IN ('id','client.id','name','contractNumber','contractor','location','description','status','documentName')) OR
        (data_type = 'DATE' AND source_path IN ('startDate','endDate','actualEndDate')) OR
        (data_type = 'AMOUNT' AND source_path IN ('contractedAmount','amountBeforeTax','taxAmount'))
    )) OR
    (origin = 'ADDITIONAL' AND data_type IN ('TEXT','DATE','AMOUNT') AND source_path ~ '^[A-Z0-9_]{1,50}$')
);
INSERT INTO reference_fields (key,visible_name,data_type,origin,source_path) VALUES
('DENOMINACION_OBRA','DENOMINACION_OBRA','TEXT','WORK','documentName'),
('DENOMINACION_CLIENTE','DENOMINACION_CLIENTE','TEXT','CLIENT','documentName');
