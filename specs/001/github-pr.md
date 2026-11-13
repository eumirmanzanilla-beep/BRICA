# PR: feat(documents): add persistent work-centered document configuration

## Alcance general

Refs #23

Configuración documental centrada en obra/cliente, con catálogo tipado, versiones recuperables, paquetes contextuales y adjuntos persistentes. No incluye extracción ni generación/exportación definitiva; por eso no cierra el issue automáticamente.

## Descripción

- Conserva los commits previos de catálogo, seguridad, análisis DOCX, versiones, paquetes y escritorio, sin reescribir el historial.
- Añade dominio y migraciones V8–V13: adicionales tipados, importes precisos, estados, roles, datos opcionales de obra, ámbitos y catálogo predefinido.
- Añade API de información completa con If-Match, aislamiento obra/cliente y adjuntos DOCX/PDF/XLSX de hasta 10 MiB. El análisis de plantillas DOCX mantiene su límite de 1 MiB.
- Reorganiza Obras en cuatro acciones: edición, asistente, consulta y paquetes/plantillas/versiones. La navegación global pasa a Catálogo de campos.
- Cierra formularios localmente sin esperar la red, indica Guardando y recupera la captura ante fallo, sin anunciar éxito prematuro.
- Conserva datos, archivos, versiones históricas y ámbitos heredados. Las herramientas de cuentas son operaciones explícitas, nunca semillas de arranque.

## Separación de los nuevos commits y archivos

1. `feat(documents): extend typed domain and persistent schema`
   - Dominio de `documents/` y `works/`, migraciones V8–V13 y pruebas de dominio/migración.
2. `feat(server): add scoped document and work attachment APIs`
   - API, aplicación, puertos, infraestructura, configuración del servidor y pruebas correspondientes.
3. `feat(desktop): add contextual work document workflows`
   - `brica-client/src/main/` y `brica-client/src/test/`: DTO, sesión, controladores, diálogos, FXML, estilos y pruebas.
4. `docs(operations): document persistent installation and integration`
   - `compose.yml`, `tools/`, `README.md` y `specs/001/`.

Los commits se revisan en orden; el esquema/dominio precede a sus consumidores. No se incluyen documentos de negocio locales, secretos, respaldos ni evidencia privada externa al repositorio.

## Validación e impacto

- `verify` repetido antes de los commits: 178 pruebas del servidor y 21 del cliente, sin fallos, errores u omitidas (`/tmp/opencode/brica-github-verify.log`). Revisión de espacios correcta.
- Prueba gráfica JavaFX: cuatro acciones, asistente sin fechas obligatorias, cierre local y recuperación de captura. Última medición registrada: 2,2 ms; no es una garantía de latencia de red ni de todos los equipos.
- Migraciones V1–V13 aplicadas en la instalación local autorizada. Reinicio real de PostgreSQL/backend comprobado; datos, cuentas y hashes de archivos conservados.
- V8–V13 son migraciones nuevas respecto de esta rama; no modificar sus contenidos después de su aplicación.
- Compose conserva los volúmenes existentes y configura `unless-stopped`. No ejecutar `down -v`.
- El backend habitual es `http://localhost:8080`, con HTTP explícito solo para uso local.
- Pendiente: aceptación humana Windows/Word, extracción, generación definitiva, aprobación del Practicante y despliegue público/TLS.

## Integración

- Base: `main`; head: `feat/23-documents-secure-reference-catalog`.
- Mantener los commits significativos; no hacer squash automático ni force-push.
- No fusionar hasta revisar las comprobaciones y los pendientes de aceptación.
