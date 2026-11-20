-- Fail on conflicting role names rather than modifying existing roles or users.
INSERT INTO roles (name, hierarchy_level, description, status) VALUES
    ('Gerencial', 4, 'Dirección', 1),
    ('Administrativo', 3, 'Administración', 1),
    ('Practicante', 2, 'Lectura; propuestas fuera del alcance actual', 1),
    ('Invitado', 1, 'Solo lectura', 1);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id_role, p.id_permission
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('Gerencial', 'Administrativo')
  AND p.key IN ('CLIENTS_READ', 'CLIENTS_WRITE', 'WORKS_READ', 'WORKS_WRITE',
                'CONTRACTS_READ', 'CONTRACTS_WRITE', 'DOCUMENT_VALUES_READ', 'DOCUMENT_CONFIG_ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id_role, p.id_permission
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('Practicante', 'Invitado')
  AND p.key IN ('CLIENTS_READ', 'WORKS_READ', 'CONTRACTS_READ', 'DOCUMENT_VALUES_READ');
