# Plan de implementación — Spec 001

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Funcionalidad:** configuración documental
**Fecha:** actualización documental 2026-11-20
**Estado:** implementación registrada; aceptación manual pendiente
**Spec:** [spec.md](spec.md) · **Seguimiento:** [review.md](review.md)
**Límites:** este plan no autoriza cambios funcionales, migraciones, despliegues ni publicación.

## 1. Resultado, alcance y fuentes

Administrar campos, plantillas DOCX, versiones y paquetes; consultar valores de obras. Incluye CA-01–CA-48. Excluye extracción, generación/exportación, historial real de generaciones y despliegue/retención definitivos.

Fuentes: [spec](spec.md), [seguimiento](review.md), código y migraciones. Rama observada: `actualization`; hay cambios locales en estos documentos y otras áreas que deben preservarse.

## 2. Diagnóstico del repositorio

| Área | Situación | Impacto |
|---|---|---|
| `brica-server/.../documents/` | API, aplicación, dominio e infraestructura documentales presentes. | Mantener reglas y permisos en servidor. |
| `brica-client/.../documents/` | Cliente API y presentación documental presentes. | Revisar FXML y ejecución asíncrona junto con la interfaz. |
| `db/migration/` | V5–V15 incluyen cambios relacionados; existen migraciones posteriores hasta V18. | No modificar migraciones históricas; cualquier cambio nuevo requiere evaluación y autorización. |
| ERS/DAS/Diccionario | Fuentes iniciales, no evidencia de implementación. | No imponer etapas ni atribuir a esta entrega la generación. |

## 3. Decisiones técnicas y de negocio

| Decisión | Estado / límite |
|---|---|
| Paquetes de cliente u obra, sin etapa ni orden obligatorio. | Registrada como vigente; ver D-02 en [seguimiento](review.md). |
| Análisis DOCX con `docxtpl` en proceso local del servidor, sin acceso Python a PostgreSQL. | Registrada como vigente; D-04. |
| Almacenamiento administrado por servidor; metadatos en PostgreSQL. | Probado con archivos de prueba; despliegue y retención finales pendientes, D-06. |
| Tipo documental identifica alternativas. | Registrada como vigente; D-12. |

El seguimiento disponible no identifica aprobador ni fecha para todas las decisiones; no se infieren.

## 4. Diseño y cambios previstos

- **Flujo:** JavaFX → API → aplicación/dominio → persistencia y almacenamiento; análisis DOCX mediante proceso Python local.
- **Contratos:** API autoriza y valida; la interfaz conserva la captura ante fallos y confirma el guardado solo al recibir respuesta.
- **Datos:** Flyway administra el esquema; versiones e historial documental se conservan mediante bajas lógicas. Los paquetes confirmados usan `If-Match`.
- **Seguridad y recuperación:** permisos en servidor; registro de versiones idempotente y recuperable, sin habilitar versiones incompletas.
- **Rutas:** módulos `documents` de servidor y cliente, recursos FXML asociados, migraciones y pruebas correspondientes. Cualquier ampliación requiere un plan y autorización propios.

## 5. Fases y criterios de salida

Fases reconstruidas como guía de implementación a partir de la spec y la evidencia disponible; no representan un registro cronológico detallado.

| Fase | Entrega y áreas | Criterio de salida / dependencia | Estado registrado |
|---|---|---|---|
| F0 · Alcance | Decisiones, reglas y límites; `specs/001/`. | Alcance y decisiones de negocio acordados antes de implementar. | Decisiones clave registradas; datos de aprobación incompletos. |
| F1 · Seguridad | Permisos documentales y controles de API; servidor. | Lectura y escritura autorizadas por separado; rechazo sin permiso. | Verificación técnica histórica; recorrido con permisos reales pendiente. |
| F2 · Campos | Catálogo, validaciones y resolución desde Cliente/Obra; API, aplicación y persistencia. | Claves y tipos validados; valores presentes, faltantes e inválidos distinguibles. | Verificado técnicamente según seguimiento. |
| F3 · Análisis | Carga y análisis DOCX, marcadores y errores; infraestructura y Python. | Marcadores admitidos reconocidos; inválidos bloqueados; estático requiere confirmación. | Verificado con casos de prueba según seguimiento. |
| F4 · Versiones | Archivo, metadatos, obligatoriedad e historial; almacenamiento y persistencia. | Reanálisis, idempotencia y recuperación; versión incompleta no seleccionable. Depende de F2–F3. | Verificación técnica histórica; almacenamiento definitivo pendiente. |
| F5 · Paquetes | Borradores, ámbitos, composición y selección; servidor y API. | Elegibilidad y concurrencia verificadas; sin etapas. Depende de F4. | Implementado y verificado; historial de generaciones excluido. |
| F6 · Escritorio | Captura, consulta, versiones y paquetes; JavaFX/FXML. | Flujos conectados sin bloquear UI; recorrido visual y permisos aceptados en Windows/Word. Depende de F1–F5. | Implementado; aceptación manual pendiente. |

## 6. Verificación

| Comprobación | Comando / recorrido | Entorno y criterio |
|---|---|---|
| Suite | `./mvnw verify` | Docker y dependencias aplicables; sin fallos. Último resultado histórico: 2026-11-15, ver [seguimiento](review.md). |
| Diff | `git diff --check` | Raíz; sin errores de espacios. |
| Aceptación | Flujo completo de [spec.md](spec.md), incluidos CA-35 y CA-43–CA-48 y permisos con/sin autorización. | Windows/Word y datos de prueba; registrar actor, entorno y evidencia. |

## 7. Riesgos, bloqueos y próximos pasos

- La verificación histórica no sustituye la aceptación funcional ni prueba el estado actual del árbol modificado.
- No utilizar datos reales ni alterar almacenamiento persistente para pruebas sin autorización.
- Pendientes: aceptación Windows/Word, permisos y decisiones de operación/retención. La generación corresponde a otra entrega.
- Siguiente paso sujeto a autorización: preparar y ejecutar el recorrido manual con datos de prueba.
