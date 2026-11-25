# Seguimiento de implementación — Spec 002

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Inicio:** 2026-11-17 · **Última actualización documental:** 2026-11-20
**Rama observada:** `actualization`
**Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

> Registro factual: código presente no equivale a verificación técnica actual ni a aceptación funcional. Los resultados de F0 son históricos y no se repitieron en esta edición.

## 1. Estado general

**Estado:** implementado parcialmente; verificación integral y aceptación pendientes.

| Área / fase | Evidencia observada | Pendiente |
|---|---|---|
| F0 · Seguridad y corpus | Banco sintético y contenedor experimental; resultados históricos abajo. | Frontera independiente por worker en despliegue, evaluación de dependencias y corpus ampliado. |
| F1–F3 · Modelo y ejecución | Código de extracción y migraciones V15–V18 presentes; worker falla cerrado sin cgroup configurado. | Ejecutar pruebas de aplicación, migración y recuperación; comprobar aislamiento efectivo del destino. |
| F4–F6 · Lectores y revisión | `SourceReader`, servicios de candidatos/perfiles y borradores presentes. | Cobertura integral por formato, OCR mixto/degradado y cachés XLS. |
| F7–F8 · Confirmación y presupuestos | Servicios, entidades y pruebas focalizadas presentes. | Verificar permisos reales, concurrencia y recorrido integrado. |
| F9 · Escritorio | Ventana JavaFX de extracción y mapeo presente. | Comprobación visual y funcional en Windows. |
| F10–F11 · Aceptación y operación | Existen pruebas automatizadas; no se ejecutaron en esta actualización. | Corpus anotado completo, aceptación manual, respaldo y autorización de despliegue. |

La bitácora anterior indicaba F1–F11 no iniciadas: era cierta para el bloque experimental de 2026-11-17, pero ya no describe la presencia de código de la rama actual. No se encontraron aquí fechas o evidencia de ejecución para atribuir los cambios posteriores a un hito concreto.

## 2. Autorizaciones y decisiones

| Fecha | Decisión / alcance | Autorización y límite |
|---|---|---|
| 2026-11-17 | P01–P33: extracción con confirmación selectiva, cuatro formatos, mapeo asistido y sin generación. | Elecciones registradas del responsable; no autorizan por sí solas despliegue ni uso de datos reales. |
| 2026-11-17 | F0 y diseño v1: precedencia, `CONTROL_V1`, discrepancias, borradores, perfiles y retención. | Responsable delegó decisiones de diseño y autorizó banco sintético; detalles en [plan.md](plan.md). |
| 2026-11-17 | PDFBox/POI/Tesseract y pruebas aisladas; imagen/perfil experimentales. | Autorización condicionada a comprobar versiones, licencias y pruebas; no autoriza adoptar el perfil experimental en producción. |
| 2026-11-20 | Reescritura y consolidación de esta spec. | Usuario autorizó integrar y eliminar cuatro documentos auxiliares, renombrar el seguimiento y actualizar referencias. El banco F0 se retiró posteriormente por autorización. |

No se registró autorización para leer muestras reales por su mera presencia en el repositorio. No se infieren aprobadores o fechas individuales para decisiones que no los identifican.

La bitácora histórica F0 se consolidó en este seguimiento; `review-history-f0.md` no está presente en esta rama.

## 3. Bitácora cronológica

### 2026-11-25 — Seguimiento de #28 en rama de trabajo

- Se ejecutaron las pruebas sintéticas de lectores y dos escenarios de
  persistencia DOCX/XLSX con PostgreSQL desechable; estos últimos llaman al
  lector directamente y no demuestran aislamiento.
- Se empaquetó el servidor y se ejecutaron por separado las pruebas del lector
  Windows aislado con DOCX y XLSX sintéticos; en XLSX se comprobó la fórmula y
  una caché cero sin evaluación. No se examinó una fuente real.
- Se explicó el fallo cerrado por cgroup no configurado y el requisito del JAR
  en Windows. El Compose predeterminado no configura aislamiento Linux. No se
  ha demostrado un entorno Linux delegado ni se ha realizado el recorrido
  visual de JavaFX; el issue sigue sin aceptación integral.
- El intento de ejecutar Maven en WSL desde este worktree no localizó el
  wrapper; las pruebas registradas se ejecutaron en Windows con Java 27,
  compilando para la versión 25 configurada por el proyecto.

### 2026-11-17 — Alcance y evaluación inicial F0

- Se acordaron PDF/XLS/XLSX/DOCX fuente, revisión y confirmación por destino; generación/exportación se difirieron. Las propuestas DP-33–DP-36 corresponden a esa generación futura.
- Un prototipo sintético leyó PDF digital, XLS/XLSX y DOCX sin marcadores; no probó OCR, aislamiento completo ni seguridad integral. Un aviso Log4j quedó pendiente de revisión del grafo de dependencias.
- Se autorizaron datos construidos, no documentos reales. La lectura de muestras de negocio requiere identificar y autorizar cada archivo por separado.

### 2026-11-17 — Banco sintético y políticas F0

- El banco F0, retirado posteriormente por autorización, incorporó lector de prueba, anotaciones independientes, evaluación de sandbox, auditoría de dependencias y ejemplos decimales. No era código integrado en la aplicación.
- Su banco host registró **10/10 comprobaciones**, incluidas **28 aserciones lector/OCR**. Once fuentes sintéticas conservaron sus hashes; resultados no demuestran precisión universal ni cobertura integral.
- Sus pruebas decimales registraron inicialmente **3/3** resultados. La auditoría registró 17 coordenadas y ninguna coincidencia informada por OSV el 2026-11-17; no prueba ausencia de vulnerabilidades ni completa las licencias nativas o el grafo Spring.
- Incidencias conservadas: generador `unsetV()` sobre nodo ausente; enlaces/configuración Java no montados; POI intentó escribir XLSX al cerrar; getter XLSX devolvió cero para caché ausente; primera auditoría interpretó mal el POM XMLBeans. Se corrigieron el generador, montajes mínimos, apertura en solo lectura, comprobación estructural y auditoría, y se repitieron las pruebas. El tratamiento de cachés XLS permaneció abierto.
- Corpus propuesto S01–S10: PDF digital/escaneado/mixto, cifrado/dañado, XLSX con hojas/fórmulas, XLS, DOCX, fuentes contradictorias y fechas/decimales ambiguos. Cobertura ejecutada parcial de S01–S08; S09/S10 no estaban conectados al mapeo. Faltaban escaneos degradados/rotados, tablas complejas y límites acumulados.

### 2026-11-17 — Imagen y perfil experimentales

- La imagen inicial devolvió `EPERM` al crear namespaces. Una imagen/perfil específicos y filtro interior obligatorio permitieron probar el aislamiento sin Docker socket ni capacidades amplias; el perfil default siguió fallando cerrado.
- Ejecución final de contenedor: **13/13 comprobaciones y 28/28 aserciones**; límites agregados de scratch, tareas y memoria probados para el **contenedor completo**, no para un worker dentro del backend. La prueba de estrés registró OOM esperado de un hijo; no demuestra protección del backend.
- Regresión del banco host: **10/10**; unitarias de experimentos: **6/6**. Sin suite Maven en ese bloque. Los informes en `/tmp/opencode` eran temporales y pueden no existir. El banco fue retirado posteriormente por autorización.
- F0 quedó abierta entonces: faltaban delegación de cgroup por worker, revisión de licencias/seguridad nativas y del grafo integrado, corpus ampliado y aceptación manual. No se autorizaron cambios de despliegue ni purgas.

### 2026-11-20 — Consolidación y contraste con código

- Se revisaron `ExtractionController`, servicios, lector, worker, migraciones V15–V18, POM, configuración, ventana y nombres de pruebas. No se consultó una instancia PostgreSQL ni documentos reales.
- Se consolidaron las decisiones y el corpus en `spec.md`, `plan.md` y este seguimiento; se eliminaron `corpus-f0.md`, `dp-alineacion.md`, `ficha-tecnica-f0.md` y `pendients.md` por autorización. `Review.md` se renombró a `review.md`; referencias afectadas actualizadas. El banco F0 fue retirado posteriormente por autorización.
- No se modificó código ni configuración de aplicación; no se ejecutaron Maven, pruebas de sandbox ni aceptación manual para esta edición. Los archivos locales ajenos se preservaron.

## 4. Verificación acumulada

| Fecha | Comando / escenario | Resultado registrado | Alcance / límite |
|---|---|---|---|
| 2026-11-17 | Banco host F0 retirado | 10/10; 28 aserciones lector/OCR. | Histórico, sintético; no regresión del backend. |
| 2026-11-17 | Pruebas F0 retiradas | 6/6 tras ampliar perfiles. | Histórico; política/experimento, no aceptación de negocio. |
| 2026-11-17 | Banco `experiments/container/` | 13/13; 28 aserciones. | Histórico: banco retirado en `c979921`; el límite era del contenedor completo, no del worker. |
| 2026-11-20 | Inspección estática de código, migraciones y pruebas. | Componentes presentes. | No demuestra que las pruebas pasen ni que el destino esté configurado. |
| 2026-11-23 | [R0 del refactor](refactor-plan.md#evidencia-de-r0--2026-11-23): pruebas focalizadas de Extracción, worker Windows empaquetado y ejemplos decimales F0 antes de retirar el banco. | 18 pruebas Java aprobadas, incluida la lectura de un DOCX sintético con el worker Windows; 3 ejemplos F0 aprobados en Ubuntu/WSL. | El banco host completo no se repitió; estas pruebas no sustituyen la aceptación. |

La ejecución final experimental registró `summary.json` SHA-256 `34185e2768b837ea8949bf412f4362db1456efac07f425d1a8864ed888aa5a0f`; informe temporal sin garantía de retención. El banco F0 y el de contenedor fueron retirados; esta bitácora no ofrece un procedimiento de reproducción.

## 5. Cierre de aceptación

- **Aceptados con evidencia funcional:** ninguno registrado.
- **Código presente, sin aceptación:** extracción, revisión, presupuestos, API y ventana; ver tabla de estado. No se declara cada criterio EX cumplido solo por existir clases o pruebas.
- **Pendiente:** EX-01–EX-14, corpus ampliado, seguridad/recuperación, permisos reales y recorrido Windows con actor y evidencia. Muestras reales requieren autorización por archivo.
- **Riesgos residuales:** aislamiento del worker en destino, licencias nativas/grafo integrado, operación, respaldo y retención. Generación y exportación están fuera del alcance.
- **Responsable y fecha de aceptación:** pendientes.
