# ADR-001 — Arquitectura cliente-servidor

## Estado
Aceptada.

## Contexto
BRICA debe ejecutarse como aplicación de escritorio en varias computadoras de la empresa y en una laptop que puede trasladarse a obras. Todas las instalaciones deben trabajar sobre una única fuente de información cuando exista conectividad.

## Decisión
Adoptar una arquitectura cliente-servidor:

- `brica-client`: aplicación de escritorio JavaFX.
- `brica-server`: backend Spring Boot.
- Comunicación mediante API REST sobre HTTP/HTTPS.
- PostgreSQL y los archivos persistentes son accesibles únicamente desde el servidor.
- El cliente no accede directamente a la base de datos ni al filesystem del servidor.
- El modo offline queda fuera del MVP.

## Consecuencias
- Existe una fuente de verdad central.
- Varias PCs pueden trabajar con la misma información.
- La seguridad y las reglas de negocio se concentran en el servidor.
- El uso normal del cliente requiere conectividad con el servidor.
