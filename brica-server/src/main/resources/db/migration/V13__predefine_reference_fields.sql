-- Preserve existing definitions and version associations; only align display names.
UPDATE reference_fields SET visible_name = key, revision = revision + 1 WHERE visible_name <> key;
INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path) VALUES
('NOMBRE_OBRA','NOMBRE_OBRA','TEXT','WORK','name'),
('NOMBRE_CLIENTE','NOMBRE_CLIENTE','TEXT','CLIENT','name'),
('ID_OBRA','ID_OBRA','TEXT','WORK','id'),
('ID_CLIENTE_OBRA','ID_CLIENTE_OBRA','TEXT','WORK','client.id'),
('ID_CLIENTE','ID_CLIENTE','TEXT','CLIENT','id'),
('REPRESENTANTE_LEGAL','REPRESENTANTE_LEGAL','TEXT','CLIENT','legalRepresentative'),
('NUMERO_CONTRATO','NUMERO_CONTRATO','TEXT','WORK','contractNumber'),
('CONTRATISTA','CONTRATISTA','TEXT','WORK','contractor'),
('UBICACION','UBICACION','TEXT','WORK','location'),
('DESCRIPCION_TRABAJO','DESCRIPCION_TRABAJO','TEXT','WORK','description'),
('ESTADO_OBRA','ESTADO_OBRA','TEXT','WORK','status'),
('FECHA_INICIO','FECHA_INICIO','DATE','WORK','startDate'),
('FECHA_TERMINACION','FECHA_TERMINACION','DATE','WORK','endDate'),
('FECHA_TERMINACION_REAL','FECHA_TERMINACION_REAL','DATE','WORK','actualEndDate'),
('MONTO_CONTRATADO','MONTO_CONTRATADO','AMOUNT','WORK','contractedAmount'),
('MONTO_ANTES_IVA','MONTO_ANTES_IVA','AMOUNT','WORK','amountBeforeTax'),
('IVA','IVA','AMOUNT','WORK','taxAmount')
ON CONFLICT (key) DO NOTHING;
