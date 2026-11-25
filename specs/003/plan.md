# Plan de implementación — Spec 003

**Funcionalidad:** generación documental desde datos de obra  
**Fecha:** 2026-11-25  
**Estado:** Propuesto  
**Spec:** [spec.md](spec.md) · **Seguimiento:** [review.md](review.md)  
**Autorización / límites:** el usuario autorizó redactar la spec, el plan y el seguimiento en una rama nueva desde la rama anterior. Implementación, decisiones de negocio, dependencias, migraciones, commits, push y despliegue requieren autorización posterior.

## 1. Resultado, alcance y fuentes

- **Resultado:** elaborar DOCX para una obra con valores resueltos y versiones fijadas, conservando historial y acceso autorizado.
- **Incluido / excluido:** véase alcance de `spec.md`; borradores, revisión y descarga requieren concretar decisiones abiertas. Estimaciones, etapas, PDF y extracción adicional quedan excluidos.
- **Fuentes:** ERS, DAS, Diccionario, Constitución, spec 001, README de módulos, código de `documents`, analizador Python y migraciones. Los README documentan árboles objetivo, no garantizan implementación.
- **Rama/estado observado:** `docs/spec-003-document-generation`, creada desde `fix/spec-002-source-extraction`; se preservan `.impeccable/`, `.opencode/commands/impeccable.md` y `.opencode/skills/`, sin seguimiento y ajenos al cambio. La base no es `main` por indicación expresa; no se conoce issue.

## 2. Diagnóstico del repositorio

| Área/ruta | Situación comprobada | Impacto en el plan |
|---|---|---|
| `brica-server/.../documents/application/ResolveReferenceValuesService.java` | Resuelve campos para obra mediante catálogo y fuente; requiere `DOCUMENT_VALUES_READ`. | Reutilizar la resolución y conservar origen/estado, sin deducir autorización de generación. |
| `DocumentSelectionService.java` | Comprueba paquete, revisión, plantilla, versión más reciente elegible e integridad del archivo. | Integrar y comprobar selección antes de fijarla; diseñar reanudación histórica aparte. |
| `documents/infrastructure/ServerDocumentFileStore.java` | Hay almacenamiento documental existente. | Evaluar reutilización segura y recuperación coordinada con metadatos; no asumir transacción atómica de disco y BD. |
| `src/main/python/brica_docx.py` | Analiza y normaliza marcadores DOCX con restricciones; no se ha comprobado que genere salidas. | Evaluar extensión o adaptador; no presentar el análisis como renderizado. |
| Cliente `documents/` y `works/` | Configuración documental y flujo de obras existen; no se ha comprobado módulo de generación. | Integrar desde obra mediante API, sin acceso directo a persistencia. |
| `db/migration/` | Existen V19 y V20 posteriores a V18, citada como última por el README. | Contrastar esquema y entidades al implementar; nueva versión única solo si se aprueba. |
| ERS/DAS/Diccionario y spec 001 | Fuentes iniciales describen etapas y periodos; spec 001 no exige etapas. | No incorporar etapas ni capacidades futuras sin decisión explícita. |

## 3. Decisiones técnicas y de negocio

| ID | Decisión / alternativa | Pros y riesgos | Estado / aprobador / fecha |
|---|---|---|---|
| D-001 | Reutilizar o ampliar permisos documentales frente a permisos específicos. | Menos cambios frente a separación de privilegios. | Pendiente / responsable / — |
| D-002 | Fijar valores al inicio o al confirmar, o combinar instantánea y detección de cambios. | Reproducibilidad frente a frescura y concurrencia. | Pendiente / responsable / — |
| D-003 | Vista previa DOCX renderizada o revisión estructurada; exportación de borradores. | Fidelidad frente a complejidad y archivos temporales. | Pendiente / responsable / — |
| D-004 | Política de opcionales, inválidos y plantillas estáticas. | Evita contenido ambiguo; requiere criterio de negocio. | Pendiente / responsable / — |
| D-005 | Estados, control optimista, idempotencia y recuperación de errores parciales. | Integridad frente a complejidad transaccional. | Pendiente / responsable / — |
| D-006 | Descarga individual o múltiple y política histórica/retención. | Usabilidad frente a límites y privacidad. | Pendiente / responsable / — |
| D-007 | Captura adicional y precedencia frente a datos de origen. | Flexibilidad frente a discrepancias y trazabilidad. | Pendiente / responsable / — |
| T-001 | Evaluar reutilizar `docxtpl` y restricciones actuales frente a otro motor. | Aprovecha infraestructura, pero renderizado seguro y fidelidad deben comprobarse; dependencia nueva requiere autorización. | Propuesto / sin aprobador / — |

## 4. Diseño y cambios previstos

- **Flujo entre módulos/capas:** JavaFX solicita selección y resolución al servidor; una aplicación de generación valida permisos y selección, fija versiones, gestiona elaboración y delega renderizado/almacenamiento a puertos. `documents` continúa siendo dueño de configuración y resolución.
- **Contratos API/UI:** operaciones para iniciar, consultar, guardar, revisar, confirmar, listar y descargar, sujetas a decisiones; DTO propios y errores compatibles con `ProblemDetail`. No fijar rutas, verbos ni estados HTTP sin diseño aprobado.
- **Entidades y migraciones Flyway:** evaluar registros de ejecución, selección, valores necesarios y archivos, incluyendo revisión y restricciones. Comparar V20, entidades y datos existentes; si procede, migración nueva sin editar históricas, validando instalación nueva y actualización.
- **Archivos/rutas previstos:** nombres ilustrativos, no archivos existentes ni orden de creación:
  - Servidor `brica-server/src/main/java/com/brica/generation/{api,application,application/port,domain,infrastructure}/`: controladores/DTO, servicios de borrador y confirmación, puertos de contexto y renderizado, modelo y adaptadores.
  - Servidor `brica-server/src/main/java/com/brica/documents/`: integrar las interfaces de selección, valores y archivos existentes; revisar consumidores antes de modificarlas.
  - Procesador `brica-server/src/main/python/brica_docx.py` y `requirements.txt`: evaluar extensión segura del analizador y pruebas en `src/test/python/`; sin adoptar nuevas dependencias por anticipado.
  - Cliente `brica-client/src/main/java/com/brica/client/generation/{api,application,presentation}/`: cliente HTTP, estado de elaboración y pantallas; integrar acceso con `works/` según patrón real.
  - Recursos `brica-client/src/main/resources/com/brica/client/generation/` si se necesitan FXML; pruebas Java correspondientes en ambos módulos y fixtures ficticios en `brica-server/src/test/resources/documents/`.
- **Seguridad, autorización, privacidad y límites:** decisiones explícitas de permisos; sesión/CSRF, límites y validación DOCX del servidor; sin datos reales ni rutas internas expuestas.
- **Compatibilidad, idempotencia, concurrencia, rollback/recuperación:** conservar API y migraciones existentes; diseñar recuperación ante fallos entre disco y BD, reintentos y revisión concurrente según D-005. No sobrescribir historial ni prometer rollback transaccional del sistema de archivos.

## 5. Fases y criterios de salida

| Fase | Entrega | Archivos/áreas | Pruebas/evidencia | Dependencias / aprobación |
|---|---|---|---|---|
| F0 | Aclarar decisiones, matriz de campos y contrato; revisar esquema y plantillas representativas. | Spec, configuración, obra y archivos | Decisiones registradas y escenarios aprobados. | D-001 a D-007; autorización. |
| F1 | Diseñar persistencia, selección fijada y permisos. | `generation`, `documents`, Flyway si procede | Pruebas unitarias, seguridad y migración nueva/actualizada. | F0 y aprobación de esquema. |
| F2 | Resolver contexto y generar DOCX sin alterar originales. | Servidor, adaptador y Python | Valores, marcadores, formato, límites y errores con fixtures. | F1; decisión T-001. |
| F3 | Elaboración recuperable, confirmación e historial/descarga. | API, almacenamiento y persistencia | Integración, reintentos, concurrencia y fallo parcial. | F1–F2 y D-003/D-005/D-006. |
| F4 | Flujo JavaFX accesible desde obra. | Cliente, FXML si aplica | Pruebas de contrato y recorrido visual en Windows. | Contratos aprobados y F3. |
| F5 | Validación integral y aceptación. | Ambos módulos y seguimiento | Maven, Python, DOCX abierto y evidencia manual. | F0–F4; responsable de aceptación. |

## 6. Verificación

| Comprobación | Comando/recorrido | Entorno | Criterio de éxito |
|---|---|---|---|
| Pruebas | `./mvnw.cmd test` | Windows, Java 25; Docker si corresponde | Pruebas pertinentes exitosas. |
| Integración/empaquetado | `./mvnw.cmd verify` | Docker/Testcontainers y entorno Python según configuración | Suites y empaquetado correctos; migraciones nueva y actualizada verificadas. |
| Procesador | `python -m unittest discover -s brica-server/src/test/python -v` | Python y dependencias del proyecto | Pruebas documentales exitosas. |
| Diff | `git diff --check` y revisión de archivos nuevos | Raíz | Sin errores de espacios ni cambios incidentales. |
| Manual | Flujo de obra, faltantes, borrador, generación y apertura DOCX | Windows, editor compatible y datos ficticios | Criterios de spec y presentación verificados; evidencia registrada. |

## 7. Riesgos, bloqueos y próximos pasos

- **Riesgos:** discrepancia entre configuración actual y requisitos históricos; pérdida de formato; cambios de valores o versiones durante la elaboración; persistencia parcial entre archivo y BD; filtración de datos.
- **Datos/instancias:** no consultar datos reales, eliminar volúmenes ni alterar migraciones históricas; usar fixtures y bases aisladas.
- **Bloqueos:** decisiones D-001 a D-007, diseño de contratos y permisos, y aprobación de cualquier dependencia o migración.
- **Próximo paso sujeto a autorización:** revisión de la spec y resolución de decisiones con el responsable; ninguna fase de implementación se inicia por la existencia de este plan.
