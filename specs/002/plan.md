# Plan de implementación — SPEC002: extracción de documentos fuente

**Fecha:** 2026-11-17.

**Estado:** F0 en curso con experimentos de host e imagen/perfil específicos; ver [Review.md](Review.md). F1–F11 no iniciadas; falta delegación de recursos por worker y evaluación ampliada antes de integrar.

**Resultado:** desde una obra y su cliente existentes, incorporar o reutilizar un archivo PDF/XLS/XLSX/DOCX, analizarlo de forma segura, identificar datos y conceptos, revisarlos con evidencia, confirmar por separado los destinos autorizados y consultar lo confirmado. La revisión se puede guardar y retomar.

**Fuentes:** [inventario P01–P33](pendients.md), [SPEC002 escrita](spec.md), [SPEC001 vigente](../001/spec.md), [seguimiento de SPEC001](../001/review.md), [README](../../README.md), [MEMORY](../../MEMORY.md), código local y configuración actual.

La instrucción que excluye generación y las elecciones del responsable prevalecen sobre la propuesta original de SPEC002 y sobre las notas históricas de MEMORY. `context.md` está vacío. La solicitud actual autoriza **redactar este plan**, no instalar bibliotecas, cambiar código/esquema, ejecutar documentos reales, desplegar ni modificar datos persistentes.

**Actualización de ejecución:** el responsable solicitó implementar por bloques y llevar trazabilidad en [Review.md](Review.md). La autorización posterior permite evaluar y, tras comprobar versiones, licencias y pruebas aisladas, incorporar PDFBox/POI y Tesseract local. Las propuestas DP-01–DP-38 recibidas después se contrastan en [dp-alineacion.md](dp-alineacion.md); los detalles marcados como pendientes allí no se consideran resueltos ni autorizan procesar documentos reales. Esta actualización no autoriza modificar la base persistente, hacer commits ni desplegar.

## 1. Alcance aprobado y exclusiones

### Incluido

- PDF digital, escaneado y mixto; OCR local solo donde la lectura digital resulte insuficiente.
- XLS/XLSX con selección, agrupación explícita de hojas y mapeo editable de columnas/rangos.
- DOCX **como documento fuente**, con lector de contenido distinto del analizador de marcadores de plantillas.
- Extracción genérica asistida, sin prometer interpretar cualquier diseño y sin exigir paquete o versión de plantilla.
- Candidatos para todos los campos de negocio admitidos, incluidos adicionales existentes; identificadores internos y relaciones no son destinos escribibles.
- Propuestas, precedencia explícita por campo/clase de fuente, revisión humana, evidencia y confirmación selectiva.
- Nombres técnicos separados de denominaciones documentales literales de obra/cliente.
- Presupuestos con modelo híbrido, conceptos ordenados, revisiones inmutables y elección explícita de revisión principal.
- Cálculos de comprobación independientes de los importes fuente; lectura de fórmulas **sin evaluarlas**.
- Confirmación independiente de obra, cliente y cada presupuesto; concurrencia optimista e idempotencia por operación.
- Trabajos persistidos, ejecución local limitada, revisión guardada en servidor, recuperación y auditoría.
- Ventana JavaFX independiente abierta desde una obra o un adjunto. Reutilización de permisos y de los adjuntos existentes.
- Pruebas anotadas y aceptación manual con muestras autorizadas.

**Interpretación de P02-C:** «incluir también DOCX» amplía PDF/XLS/XLSX del alcance escrito; no elimina XLS. La matriz de entrega incluye los cuatro formatos. DOC/DOCM/XLSM y archivos de imagen independientes no se incluyen; una imagen dentro de un PDF se atiende por OCR.

### Fuera de alcance

- Renderizado de plantillas, sustitución de bloques repetibles, vista previa de documentos de salida, conversión DOCX→PDF, generación, exportación y documentos generados.
- Habilitar el botón **Generar documentos** o modificar la política de selección de versiones de SPEC001.
- Alta/reasignación automática de obras/clientes, convenios, estimaciones y aprobaciones interpersonales del Practicante.
- Extracción con modelos remotos, OCR en la nube, nuevos servicios o brokers de mensajes.
- Evaluar fórmulas Excel, ejecutar macros, actualizar vínculos externos o recalcular un libro con una suite ofimática.
- Crear campos globales automáticamente, introducir rutas de origen libres o usar conceptos como campos escalares del catálogo.
- Purgar fuentes, mover archivos históricos o reutilizar autorizaciones antiguas de limpieza.

Rasterizar una **página fuente** para OCR o para revisar evidencia no es generación documental. No se producirá un nuevo documento contractual ni se modificará el original.

## 2. Registro vinculante de las 33 elecciones

Las letras corresponden a las alternativas de `pendients.md`, no a las prioridades B/D/F de aquel documento. Sus recomendaciones históricas quedan sustituidas por esta tabla cuando corresponda.

| ID | Elección | Consecuencia para la implementación | Fases |
|---|---|---|---|
| P01 | B | Extraer, revisar y confirmar selectivamente; no solo mostrar texto. | F3, F7–F10 |
| P02 | C | Añadir DOCX fuente a PDF/XLS/XLSX; no reutilizar el analizador de plantillas como lector. | F1, F4–F5 |
| P03 | B | Lectura genérica y mapeo manual asistido, sin perfiles rígidos por cliente como condición. | F0, F4–F6 |
| P04 | A | Intentar los destinos de negocio admitidos sin depender de un paquete. | F1, F6 |
| P05 | B | Asociar candidatos a destinos permitidos y guardar opcionalmente un perfil de mapeo. | F6, F9 |
| P06 | B | Prioridad explícita por campo/clase de fuente, conservando alternativas y confirmación humana. | F0, F6 |
| P07 | B | Leer posiciones/bloques y tablas identificables en PDF. | F4 |
| P08 | A | OCR únicamente en páginas/regiones con lectura digital insuficiente. | F4 |
| P09 | A | Lectores Java; OCR como proceso local externo, sin nuevo servicio. | F0, F3–F5 |
| P10 | B | Detectar estructura Excel y permitir corregir columnas/rangos/filas. | F5, F9 |
| P11 | A | Usar valores almacenados de fórmulas; conservar la fórmula como evidencia, no evaluarla. | F5 |
| P12 | B | Usuario agrupa hojas como partes, resúmenes o alternativas; no fusionar automáticamente. | F5, F8–F9 |
| P13 | C | Cabecera/identidad/revisiones explícitas y contenido flexible acotado. | F1, F8 |
| P14 | B | Revisiones presupuestales inmutables y elección explícita de revisión principal. | F8 |
| P15 | A | Evidencia mínima: fuente/hash, ubicación, original y destino sugerido. | F1, F4–F6 |
| P16 | B | Separar nombre técnico y denominación documental literal. | F1–F2, F7 |
| P17 | A | Convenciones regionales explícitas por fuente/perfil y cronología vigente. | F0, F6 |
| P18 | B | Comprobar cantidades/importes por separado, sin sustituir los importes fuente. | F6, F8 |
| P19 | B | Datos sin atributo fijo pueden asociarse expresamente a adicionales existentes. | F6–F7 |
| P20 | A | Fuente común en obra/cliente y presupuestos propios; no duplicar valores en `contracts.data`. | F7–F8 |
| P21 | A | Completar ausentes o corregir captura con justificación; no tramitar convenios. | F7–F8 |
| P22 | B | Confirmaciones independientes con resultados claros por destino. | F7–F9 |
| P23 | A | Control optimista por destino, incluido cliente; sin fusión automática por campo. | F2, F7–F8 |
| P24 | A | Clave y fingerprint por petición/contexto; reintentar no vuelve a aplicar cambios. | F3, F7–F8 |
| P25 | B | Trabajos persistidos y ejecución local limitada con consulta de estado. | F3 |
| P26 | B | Borradores de revisión en servidor, con reanudación y control de revisión. | F3, F6–F9 |
| P27 | B | Ventana separada de extracción, no agrandar el asistente actual. | F9 |
| P28 | A | Reutilizar permisos por destino, sin nuevos permisos ni roles. | F1–F10 |
| P29 | A | Procesos limitados dentro del despliegue actual; un subproceso no equivale a sandbox. | F0, F3–F5 |
| P30 | A | Referenciar el adjunto existente; no copiarlo por análisis. | F1, F3 |
| P31 | A | Operación durable en fases, publicación verificable, auditoría y recuperación idempotente. | F1–F3, F7–F11 |
| P32 | A | Entregar datos/colecciones confirmados mediante contratos propios; sin generación. | F8, F10 |
| P33 | A + validación manual de B | Corpus anotado, métricas por campo/fila y prueba manual con archivos reales autorizados. | F0, F10–F11 |

## 3. Punto de partida y reutilización

| Existente | Reutilizar | Cambio necesario o precaución |
|---|---|---|
| `WorkAttachmentService`, `WorkAttachment`, almacenamiento y hash | Fuente asociada a obra, metadatos, consulta y conservación. | Admitir XLS seguro; acceso interno controlado a la evidencia de una fuente eliminada lógicamente. |
| `AttachmentFormat` | Reglas iniciales de formato/contendor. | Distinguir almacenar de procesar: cabecera PDF válida no prueba legibilidad. XLS exige validación propia. |
| `ServerDocumentFileStore` | Bytes inmutables, publicación segura y rutas internas. | No inferir formato de `registrations/UUID.docx`; elegir lector por formato verificado. No renombrar archivos antiguos. |
| `ReferenceAttribute`, `ReferenceField`, `ReferenceValue` | Destinos aprobados, tipos y validación común. | Añadir denominaciones documentales con claves nuevas; no cambiar el significado de claves vinculadas. |
| `DocumentInformationService`, `WorkRevision`, `WorkStore` | Reglas de obra, bloqueo breve e `If-Match`. | Crear aplicación selectiva; no enviar un reemplazo completo que borre datos omitidos. Incluir nuevos atributos en ETag. |
| `UpdateClientService`, `ClientStore`, API y cliente de Clientes | Validación y flujo de actualización. | Agregar control optimista coherente para **todos** los escritores de cliente, no solo extracción. |
| `RegisterTemplateVersionService` | Patrón de operación durable/idempotencia. | Reutilizar el concepto, no sus tablas ni estados de registro de versiones. |
| `TemplateAnalysisService`/`brica_docx.py` | Mantener intacto el análisis seguro de plantillas. | El lector DOCX fuente será Java e independiente; no habilitar Jinja ni renderizado. |
| `RegisterContractService`/`contracts.data` | Conservar funcionamiento actual. | No escribir aquí una copia de cada candidato contractual; P20-A usa la fuente común. |
| `ServerSession`, `DocumentUi`, `AsyncDialogSave` | Sesión/CSRF, llamadas asíncronas y recuperación de captura. | No confundir cierre local con tarea terminada; consultas de progreso y reanudación requieren persistencia real. |
| Flyway V1–V14, Testcontainers y pruebas JavaFX | Regresión y actualización segura. | Confirmar numeración al implementar; V14 ya aplicada no se edita. |

**MEMORY es histórico:** sus referencias a solo nombres, V7 o «no iniciar motor» no describen el alcance ahora aprobado. Sus advertencias sobre secretos, permisos, no sobrescribir y no inventar aceptación siguen aplicando. Las cifras de pruebas de SPEC001 son antecedentes, no resultados de SPEC002.

## 4. Diseño objetivo

### 4.1. Organización del código

No crear módulos Maven nuevos ni servicios separados. Las rutas siguientes son **propuestas de implementación**, no archivos ya existentes:

```text
brica-server/src/main/java/com/brica/
  extraction/
    api/             # trabajos, resultados, borradores y confirmaciones
    application/     # coordinación, permisos, mapeo y recuperación
    application/port/# contratos de lectores, trabajos, evidencia y auditoría
    domain/          # propuestas, decisiones, tipos, estados y límites
    infrastructure/
      reader/        # PDF/XLS/XLSX/DOCX; sin reglas de persistencia
      ocr/           # invocación local controlada
      worker/        # proceso Java de lectura y protocolo acotado
      persistence/   # puertos JPA/consultas
  budgets/
    api/ application/ domain/ infrastructure/

brica-client/src/main/java/com/brica/client/
  api/ExtractionApiClient.java
  api/BudgetsApiClient.java
  presentation/extraction/ # ventana, paneles y controladores pequeños

brica-client/src/main/resources/com/brica/client/presentation/extraction/
```

Mantener `api → application → domain`; infraestructura implementa puertos. El adaptador lector no recibe repositorios, credenciales de base, sesión HTTP ni permisos para confirmar datos. Los cambios de obra/cliente se realizan mediante sus casos de uso/reglas, no por SQL del extractor.

### 4.2. Modelo persistente propuesto

Definir el detalle relacional en F1, con claves externas e índices para consultas paginadas; no presentar esta tabla como esquema aplicado.

| Registro | Responsabilidad e invariantes |
|---|---|
| `extraction_jobs` | Obra, solicitud, estado, revisión, actor/fechas, versión de lector/opciones, límites, arrendamiento de ejecución y cancelación. |
| `extraction_sources` | Asociación trabajo–adjunto existente, hash/forma verificados, clase de fuente y orden; sin duplicar binarios. |
| `extraction_attempts` | Intento de lectura, versión del lector/configuración, fingerprint de entrada y resultado/diagnóstico. Resultados publicados inmutables; reanálisis crea intento nuevo. |
| `extraction_candidates` | Texto original, valor tipado propuesto, destino sugerido y localización mínima; nunca un valor vigente por sí solo. |
| `extraction_reviews` y decisiones | Borrador revisionado de mapeos, exclusiones, correcciones, agrupaciones de hojas y revisiones base por destino. Paginable por candidatos/conceptos. |
| `extraction_mapping_profiles` | Configuración declarativa opcional, versionada; sin código, rutas evaluables o expresiones libres. Alcance/edición se cierran en F0. |
| `extraction_confirmations` | Clave idempotente por bloque, hash de la petición, revisión aprobada, destino, estado y respuesta original. Una confirmación de cliente no implica confirmar obra. |
| `extraction_audit_entries` | Actor/fecha, destino, antes/después de lo cambiado, justificación, fuentes y revisiones; datos sensibles con permisos. |
| `budgets` | Identidad por obra, nombre técnico, revisión del agregado y referencia a su revisión principal elegida explícitamente. |
| `budget_revisions` | Número monotónico, revisión anterior, cabecera/totales/moneda tipados, confirmación de origen y evidencia. No se actualiza contenido ya confirmado. |
| `budget_sections`/`budget_items` | Orden, pertenencia/jerarquía acotada y valores fundamentales tipados; código de concepto textual y no necesariamente único. |
| JSONB acotado en propuestas/revisiones/items | Metadatos variables, detalles de formato y evidencia complementaria con esquema/versionado. No sustituir IDs, orden, importes, cantidades o relaciones por JSON libre. |

La propuesta de partidas/conceptos con columnas estables y metadatos JSONB materializa **P13-C**, no el modelo enteramente normalizado de P13-A ni un libro completo en un único JSON. Justificar tablas finales e índices con las consultas reales; limitar tamaño/profundidad del contenido flexible.

### 4.3. Denominaciones documentales sin romper SPEC001

1. Conservar `name` técnico y sus reglas en Cliente/Obra. No transformar el literal extraído en ese campo automáticamente.
2. Agregar `documentName` o equivalente explícito, separado y con longitud/reglas documentales que se aprueben en F0. Admitir los espacios/acentos/puntuación que requiere una denominación real sin aceptar caracteres de control peligrosos.
3. Mantener nulos los nuevos valores de registros existentes si no existe fuente confirmada; no inventar denominaciones a partir del nombre técnico.
4. Proponer atributos/claves nuevos, por ejemplo `WORK_DOCUMENT_NAME`/`DENOMINACION_OBRA` y `CLIENT_DOCUMENT_NAME`/`DENOMINACION_CLIENTE`. Ratificar nombres en F0 antes de migrar; conservar `NOMBRE_OBRA`/`NOMBRE_CLIENTE` y las versiones que ya los usan.
5. Extender DTOs, edición/consulta, resolución, validaciones y ETags. Omitir un nuevo atributo en una petición antigua no debe borrarlo.
6. El motor futuro decidirá qué nombre consume una plantilla nueva; esta entrega no modifica plantillas históricas ni su contenido.

### 4.4. Dos máquinas de estado, no una «generación parcial»

**Lectura técnica propuesta:** `QUEUED → RUNNING → COMPLETED | PARTIAL | FAILED | CANCELLED`. Reclamación, lease/heartbeat e intento identifican al ejecutor. Una respuesta de un proceso vencido/cancelado no puede publicar sobre el intento vigente.

**Revisión propuesta:** `DRAFT → READY_FOR_REVIEW → PARTIALLY_CONFIRMED → CONFIRMED`, con cancelación de lo no confirmado. Los estados definitivos por bloque se consultan por separado; un fallo de cliente no revierte un bloque de obra ya confirmado.

**Alineación DP-31, pendiente de confirmación del detalle:** la propuesta más reciente presenta al usuario `borrador`, `confirmado` y `cancelado`. Esos tres estados visibles no sustituyen los estados técnicos de lectura de la línea anterior ni los resultados independientes de confirmación por destino. Un borrador con un bloque confirmado y otro pendiente debe mostrar esa situación; cancelar lo pendiente no revierte lo confirmado. Los nombres y transiciones persistidos se acuerdan en F0/F1 sin ocultar estados parciales ni convertir un análisis técnico terminado en confirmación de negocio.

- Un análisis completado no significa información de negocio confirmada.
- Un fallo/timeout puede dejar evidencias parciales identificadas, no datos aceptados implícitamente.
- Cancelar una revisión parcialmente aplicada no deshace bloques confirmados: la interfaz debe advertirlo y conservar auditoría.
- Reanudar vuelve a comprobar fuentes, permisos, catálogo y revisiones. No ejecutar confirmaciones automáticamente durante recuperación de trabajos.

### 4.5. Permisos con el contrato existente

Matriz propuesta bajo P28-A, a comprobar con sesiones reales:

| Acción | Permisos |
|---|---|
| Ver obra/fuentes y resultados autorizados en su contexto | `WORKS_READ`; datos actuales de cliente requieren además `CLIENTS_READ`. |
| Adjuntar, solicitar análisis, cancelar una tarea o editar borrador/mapeos de obra | `WORKS_READ` + `WORKS_WRITE`. |
| Confirmar información de obra, adicionales o presupuesto; elegir revisión principal | `WORKS_READ` + `WORKS_WRITE`. |
| Confirmar datos del cliente compartido | `CLIENTS_READ` + `CLIENTS_WRITE`, además de lectura de la obra de origen. |
| Resolver valores del catálogo mediante el caso de uso existente | `DOCUMENT_VALUES_READ`; no otorgarlo por tener escritura. |
| Crear/modificar definiciones globales del catálogo | `DOCUMENT_CONFIG_ADMIN`, fuera del mapeo de una extracción. |

Sin nuevos permisos ni cambios de roles por defecto. Gerencial/Administrativo pueden escribir según permisos vigentes; Practicante/Invitado no pueden crear propuestas de extracción ni confirmar por tener solo lectura. No implementar aprobaciones interpersonales. Mantener alcance global actual: no inventar asignaciones usuario–obra, pero siempre comprobar que cada fuente/revisión pertenece a la obra solicitada.

## 5. Valores técnicos que se cerrarán en F0

Las alternativas P01–P33 están elegidas. Los parámetros siguientes no se deducen de una letra y no deben fijarse silenciosamente; F0 producirá una ficha concreta para aprobar antes de incorporar dependencias/migraciones que los materialicen.

**Actualización bajo delegación de diseño y OK del bloque F0:** [ficha-tecnica-f0.md](ficha-tecnica-f0.md) concreta política v1 de límites, precedencia, decimales/discrepancias, perfiles, estados y retención, distinguiendo decisión de evidencia medida. Los candidatos de lectores tienen pruebas sintéticas, pero falta verificar aislamiento/recursos agregados en el despliegue y completar evaluación de seguridad. No se considera cumplida la salida de F0 ni se integran dependencias por ese documento.

| Detalle | Entregable requerido |
|---|---|
| Lectores/binarios | Bibliotecas, versiones exactas, licencias, vulnerabilidades conocidas y compatibilidad con Java 25/imagen Linux. Candidatos a evaluar: Apache PDFBox, Apache POI y Tesseract local. No instalados ni aprobados por aparecer aquí. |
| Límites por formato | Bytes, páginas/regiones, resolución/píxeles, hojas/filas/celdas, expansión ZIP, tamaño de resultados/JSON y recursos por proceso; coherencia con subida, almacenamiento y descarga. Mantener 10 MiB como techo existente hasta aprobar otro, sin confundirlo con capacidad suficiente de procesamiento. |
| OCR | Idiomas/modelos, detección de lectura insuficiente, rotación y calidad mínima; tiempos por página/total, máximo de procesos y comportamiento ante fallo parcial. No prometer manuscritos o precisión universal. |
| Precedencia | Tabla campo × clase de fuente y desempates; no adoptar «último archivo gana». Una prioridad solo recomienda y no sustituye la aprobación. |
| Mapeo/perfiles | Destinos iniciales, alcance y permiso de edición de perfiles reutilizables, versión de esquema, campos mínimos y límites de jerarquía. Propuesta inicial: perfiles de obra; compartir a nivel cliente requiere regla explícita, no acceso público. |
| Fechas y números | Convenciones regionales por fuente, formatos textuales, sistemas de fechas Excel, moneda y tolerancias/precisión del cálculo de comprobación. Nunca usar redondeo de presentación como cálculo. |
| Corrección contractual | Campos protegidos, definición operativa de corrección de captura, justificación obligatoria y qué cambios se consideran un convenio que esta SPEC no tramita. |
| Operación/retención | Caducidad de borradores, temporales y trabajos abandonados, conservación de auditoría/evidencia, cancelación y limpieza segura. No activar purgas de originales por defecto. |
| Calidad/aceptación | Archivos autorizados, anotaciones por campo/fila, métricas y umbrales por dato crítico más recorrido manual. No fijar un porcentaje ficticio para todos los lectores. |

Consultar documentación de las versiones seleccionadas al implementar; las APIs de lectores/cachés varían. Leer valores de fórmula almacenados exige configurar y comprobar ese modo explícitamente, no confiar en un valor predeterminado de la biblioteca.

## 6. Implementación por fases y pasos verificables

Cada fase incorpora pruebas del comportamiento que añade. El resultado de una fase técnica no equivale a aceptación del usuario. Antes de iniciar código, revisar nuevamente rama/estado y presentar el alcance de cambios de esa fase conforme a AGENTS.md.

### F0 — Alinear el contrato y aprobar la ficha técnica

**Dependencia:** ninguna. **Decisiones:** P02–P09, P15–P18, P21, P29, P33.

1. Confirmar el inventario de cambios locales de SPEC001 y su estado de verificación; no resetear ni incorporar archivos ajenos a esta entrega.
2. Registrar las 33 elecciones de este plan como fuente vigente. Preparar actualización acotada de `spec.md`: HU-001–HU-003, DOCX fuente y nueva condición de entrega; marcar HU-004–HU-006 como generación futura sin renumerar IDs históricos.
3. Identificar las muestras realmente disponibles: `PPTO 01 041126.xlsx`/PDF, contrato dañado y documentos DOCX locales no son automáticamente un corpus aceptado. Solicitar ejemplos XLS, PDF escaneado/mixto y tablas DOCX si faltan. No sustituir las muestras citadas por archivos de otro nombre sin comprobarlo.
4. Definir un corpus construido/anotado y una lista privada de muestras reales autorizadas. Los fixtures verifican un lector genérico, no obligan al usuario a usar un diseño de presupuesto único.
5. Evaluar lectores Java y OCR local en una prueba de concepto acotada, sin DB ni documentos reales no autorizados. Medir memoria, tiempo, lectura de tablas, OCR mixto y cachés de fórmulas.
6. Cerrar la ficha de §5 con valores concretos y presentar dependencias/versiones para aprobación. Verificar que el aislamiento de P29-A se puede hacer efectivo en el despliegue actual; si no, detener esa parte y consultar, no sustituirlo por parsear archivos sin límites dentro del backend.

**Archivos previstos:** `specs/002/spec.md`, un registro técnico de decisiones, manifiesto del corpus y posteriormente `README.md`/`MEMORY.md`; no modificar automáticamente ERS/DAS/Diccionario.

**Artefactos F0 ya creados:** `ficha-tecnica-f0.md` y `experiments/` contienen el banco de evaluación aislado y sus instrucciones; no son un módulo Maven ni adaptadores registrados en el backend. Tras el EPERM inicial, el responsable autorizó una imagen/perfil **experimentales**: `experiments/container/` demuestra namespaces con perfil exterior acotado y filtro de payload obligatorio, más límites cgroup/tmpfs del contenedor de prueba. La delegación de límites por worker dentro del backend sigue pendiente; no se autoriza aplicar ese perfil al despliegue ni recurrir a `privileged`, Docker socket o lectores desprotegidos.

**Salida:** contrato de extracción sin generación, matriz de formatos y ficha aprobada. No pasar a integrar dependencias sin esta salida.

### F1 — Definir contratos, modelo híbrido y migraciones nuevas

**Dependencia:** F0. **Decisiones:** P04, P13–P16, P19, P24, P26, P28, P30–P31.

1. Diseñar contratos tipados de fragmento leído, ubicación, candidato, valor normalizado, decisión de revisión y confirmación por bloque. Distinguir texto extraído de dato confirmado.
2. Enumerar todos los destinos admitidos: obra/cliente, denominaciones nuevas, adicionales existentes y colección presupuestal. Excluir IDs, reasignaciones y destinos que el usuario no tenga permiso de modificar. No exigir plantilla como precondición.
3. Precisar las entidades/relaciones de §4.2, estados y control de revisión. Cantidades/precios/importes precisos con `BigDecimal`/`NUMERIC`; códigos/unidades como texto; orden explícito y claves de concepto potencialmente repetidas.
4. Definir restricción de presupuesto–obra, revisión–presupuesto y fuente–obra; ninguna revisión de otro presupuesto puede hacerse principal. Definir unicidad de número de revisión, clave de confirmación y reclamación de trabajo sin ciclos inconsistentes.
5. Definir el acceso interno a adjuntos eliminados lógicamente cuando ya sean evidencia histórica. No reabrir su descarga ordinaria ni permitir usarlos para nuevas extracciones si ya estaban eliminados antes de solicitarlas.
6. Revisar la última versión Flyway disponible. Si sigue siendo V14, agregar sucesoras para denominaciones/concurrencia, trabajos/revisión y presupuestos/auditoría, agrupadas coherentemente. Los números/nombres definitivos se fijan al crear los archivos, no se reserva V15 si otra tarea ya la utilizó.
7. Probar esquema desde cero y migración desde V14 con cuentas, datos, archivos, versiones y vínculos previos. Añadir columnas nuevas sin inventar denominaciones o presupuestos y no cambiar migraciones aplicadas.

**Archivos previstos:** nuevos paquetes `extraction/domain`, `budgets/domain`, puertos y adaptadores JPA; `clients/domain/Client.java`, `works/domain/Work.java`, `db/migration/V<next>__*.sql` y pruebas de migración/persistencia equivalentes.

**Salida:** esquema y contratos verificables en PostgreSQL desechable, sin aplicar migraciones a `brica` todavía.

### F2 — Preparar denominaciones y control optimista de todos los destinos

**Dependencia:** F1. **Decisiones:** P16, P20, P23.

1. Incorporar denominaciones según §4.3, manteniendo nombres técnicos y claves históricas. Propagar a consultas, formularios y DTOs con compatibilidad de omisión explícita.
2. Agregar revisión/ETag de cliente y comprobación en escritura, incluida edición manual y eliminación relevante. Actualizar `ClientsApiClient`/`ClientsController` y pruebas; no dejar el escritor antiguo como vía de sobrescritura sin revisión.
3. Incluir denominación de obra en `WorkRevision`; establecer también un ETag para presupuesto/revisión principal y para borrador de revisión.
4. Implementar comandos de aplicación selectiva que lean el estado actual bajo transacción breve, comprueben revisión y validen el agregado resultante. No reutilizar un PUT completo con nulos para simular un parche parcial.
5. Mantener resolución del catálogo y `WorkReferenceData` como fuente común; no escribir copias en `contracts.data` ni cambiar contratos históricos.

**Archivos previstos:** servicios/puertos/entidades de `clients` y `works`, `ClientController`/DTOs, `WorkRevision`, `ReferenceAttribute`, `WorkReferenceData` y API/edición de Clientes/Obras del escritorio.

**Pruebas de salida:** dos escritores con la misma revisión; el segundo recibe conflicto sin pérdida de datos. Resolver claves antiguas devuelve su significado anterior; las nuevas denominaciones solo aparecen si fueron confirmadas/capturadas.

### F3 — Fuentes verificadas, cola durable y ejecución local aislada

**Dependencias:** F1; F2 para las revisiones base. **Decisiones:** P24–P26, P28–P31.

1. Crear una extracción desde una obra activa: adjuntos existentes o carga nueva con el flujo de adjuntos. Verificar clase/formato y pertenencia, no aceptar rutas libres ni adjuntos de otra obra.
2. Ampliar validación para XLS sin alterar DOCX/PDF/XLSX existentes. Validar formato de bytes y contenido activo; extensión original, MIME y ruta interna no son prueba suficiente.
3. Registrar intención durable e identidad idempotente antes de iniciar lectura. Para fuentes nuevas, publicar original de forma verificable y registrar metadatos; una caída deja una operación recuperable, no un éxito aparente.
4. Implementar reclamación de trabajos con bloqueo breve, lease, heartbeat y token de intento. Limitar ejecutores locales y disponer de consulta de estado/progreso; no mantener transacciones mientras corre el lector.
5. Ejecutar lectores Java en un subproceso local acotado. Usar lista fija de argumentos, entorno saneado sin credenciales DB, entrada de solo lectura y temporales privados. No construir comandos de shell con nombres/contenido de archivos.
6. Ejecutar OCR nativo bajo el mismo control de procesos. Imponer límites efectivos de memoria/CPU/píxeles/archivos/tiempo y salida. `-Xmx`, timeout o usuario no root, por separado, no garantizan aislamiento ni bloqueo de red; verificar medidas OS aplicables.
7. Publicar resultados por intento con hash/esquema/tamaño comprobados. Rechazar respuestas tardías de intentos cancelados/vencidos. Reanálisis produce otro intento, no modifica uno ya usado para confirmar.
8. Recuperar trabajos abandonados tras reinicio: conservar evidencias/resultados durables, reintentar solo lectura según política y nunca repetir confirmaciones de negocio. Añadir cancelación que termine también procesos hijos.

**Archivos previstos:** `extraction/application/*`, `application/port/*`, `infrastructure/worker/*`, persistencia de trabajos y cambios acotados en adjuntos/almacenamiento. `application.yml`, POM y Dockerfile solo con dependencias/parámetros aprobados.

**Pruebas de salida:** pérdida de conexión, caída antes/después de publicación, timeout, doble solicitud, cancelación y reinicio. Mismos bytes y opciones con petición nueva no se confunden con un reintento de la misma clave.

### F4 — Lector PDF y OCR selectivo

**Dependencia:** F3. **Decisiones:** P07–P09, P15, P29.

1. Abrir el documento con el lector aprobado y detectar errores de estructura, cifrado o recursos excedidos antes de procesarlo completamente. Un `%PDF-` válido no basta.
2. Extraer texto con posiciones, bloques, orden de lectura propuesto y tablas cuando se identifiquen. Conservar página/fragmento como evidencia mínima y posiciones técnicas para mapeo, sin prometer una tabla perfecta.
3. Evaluar insuficiencia de lectura por página/región con los criterios F0. Una página con encabezado textual e imagen de tabla requiere analizar también esa región; no asumir que «tiene algo de texto» evita OCR.
4. Rasterizar solo las regiones/páginas necesarias dentro de límites de resolución y ejecutar OCR local. No descartar texto digital válido al combinar resultados ni duplicar conceptos reconocidos por ambos métodos.
5. Devolver candidatos y diagnóstico parcial: ilegibilidad, ambigüedad o error en una página no confirma valores ni invalida silenciosamente las evidencias válidas restantes.
6. Comprobar seguridad ante PDF cifrado/dañado, imágenes enormes, rotación, lectura vacía y texto que aparenta instrucciones. Ningún contenido del PDF dirige herramientas, red o comandos.

**Archivos previstos:** `extraction/infrastructure/reader/PdfSourceReader.java`, adaptador de geometría/tablas y `infrastructure/ocr/*`; pruebas unitarias y de proceso.

**Salida:** PDF digital, escaneado y mixto del corpus producen datos revisables y ubicaciones verificables; límites y fallos no dejan procesos fuera de control.

### F5 — Lectores Excel y DOCX de contenido

**Dependencia:** F3; puede desarrollarse en paralelo técnico a F4 sin delegar automáticamente. **Decisiones:** P02–P03, P09–P12, P15, P18.

1. Para XLS/XLSX, enumerar hojas y sus estados, rangos, filas/columnas y celdas fusionadas sin ejecutar macros/vínculos. Presentar hojas/filas ocultas como tales y no importarlas silenciosamente.
2. Distinguir tipo original, valor numérico almacenado, formato visible, fórmula y resultado almacenado. Si no existe resultado utilizable, dejar candidato pendiente; no convertirlo en cero ni ejecutar un evaluador de fórmulas.
3. Preservar números/códigos con ceros iniciales y textos, y reconocer el sistema de fechas del libro. No prometer recuperar dígitos que ya no están almacenados en el archivo.
4. Proponer encabezados y columnas de concepto, permitir elegir/corregir rango y asociar clave, descripción, unidad, cantidad, precio e importe. Permitir excluir filas, marcar partidas, resúmenes y totales; conservar el orden explícito.
5. Devolver un esquema de hojas/rangos que permita al usuario agrupar continuaciones y distinguir alternativas. Ninguna agrupación se confirma solo por nombre de hoja.
6. Para DOCX, leer párrafos/tablas y regiones admitidas, incluidos encabezados/pies cuando proceda. Informar localización por parte/párrafo/tabla/fila/celda: Word no ofrece un número de página estable a partir del XML, por lo que no se inventará.
7. El DOCX fuente puede no contener `{{CLAVE}}`. No pasarlo por `TemplateAnalysisService` ni por `brica_docx.py` para obtener sus valores; rechazar contenido activo y no renderizar ni modificar el original.
8. Considerar perfiles versionados **como sugerencias opcionales**, sin sustituir extracción genérica/mapeo manual. Normalizar encabezados para comparar y conservar el original. Proponer filas de concepto, partida, subtotal, impuesto, total, nota o dudosa sin descartar las desconocidas. Las equivalencias y reglas de inicio/cierre son propuestas revisables, no una interpretación definitiva.

**Archivos previstos:** `ExcelSourceReader.java`, `DocxSourceReader.java`, DTOs de estructura de fuente y pruebas construidas por formato en `src/test`.

**Salida:** identificación genérica y mapeable en XLS/XLSX/DOCX, con pruebas de cachés ausentes/desactualizadas, varias hojas, tablas y textos sin marcadores.

### F6 — Mapeo, precedencia, normalización y revisión persistente

**Dependencias:** F2–F5. **Decisiones:** P04–P06, P15, P17–P19, P23–P26.

1. Construir el inventario de campos objetivo desde el catálogo admitido y los destinos nuevos. Exponer metadatos de mapeo autorizados sin conceder administración global de plantillas/catálogo.
2. Proponer correspondencias usando estructura/etiquetas reconocidas y permitir al usuario corregirlas. Todo candidato conserva original, fuente/hash y ubicación mínima. No crear campos por etiquetas desconocidas ni evaluar expresiones.
3. Conservar candidatos alternativos por destino y aplicar la precedencia aprobada como recomendación visible. Si no hay regla aplicable o hay empate semántico, marcar conflicto; no escoger el primer/último valor.
4. Normalizar fechas/números según configuración explícita por fuente/perfil. Distinguir tipo inválido, ambigüedad, ausencia y desacuerdo con el valor actual; no reutilizar `PRESENT/MISSING/INVALID` para ocultar estados de revisión.
5. Para nombres, proponer el literal como denominación documental. El nombre técnico solo cambia por una decisión separada del usuario y debe cumplir SPEC001.
6. Asociar datos no fijos a adicionales existentes compatibles; si no hay destino, conservarlos sin mapear/excluirlos de la confirmación. Revalidar catálogo y tipos al aplicar, incluso si el borrador se abrió antes de su eliminación.
7. Calcular comprobaciones independientes con decimales precisos: cantidad × precio, sumas de conceptos y contraste con totales declarados. No evaluar la fórmula Excel ni sustituir valor fuente/confirmado; no inventar IVA, moneda o reglas de descuento.
8. Guardar borrador, decisiones por candidato/fila, agrupaciones y revisiones base con ETag. Un reanálisis conserva decisiones anteriores asociadas al intento original; el usuario decide qué trasladar al nuevo, no se aplican silenciosamente.
9. Permitir guardar opcionalmente un perfil declarativo versionado con el alcance aprobado. Reutilizarlo sugiere mapeos, nunca autoriza confirmaciones ni elimina la revisión del nuevo archivo.

**Archivos previstos:** `MapExtractionCandidatesService`, `NormalizeExtractionValues`, `SourcePrecedencePolicy`, `SaveExtractionReviewService`, `MappingProfileService` y pruebas equivalentes.

**Salida:** revisión persistente reproducible y paginada, con evidencia y diferencias frente a datos vigentes. Ningún paso de esta fase modifica aún obra/cliente/presupuesto.

### F7 — Confirmar bloques independientes de obra y cliente

**Dependencias:** F2–F3 y F6. **Decisiones:** P01, P16, P19–P24, P28, P31.

1. Diseñar cada petición como conjunto explícito de cambios aceptados: destino, revisión base, decisiones/fuente, justificación cuando aplique y clave idempotente. Omitido o descartado significa **no modificar**, no vaciar.
2. Reautorizar en cada confirmación y comprobar obra/cliente activos, pertenencia de fuentes y versión de revisión. Cliente sigue ligado a la obra original; no reasignar relaciones a partir de nombres/números impresos.
3. Mostrar que el cliente es compartido y que cambiar su denominación/datos afecta a otras obras. Exigir `CLIENTS_WRITE`; `WORKS_WRITE` no lo sustituye.
4. Distinguir completar ausente, corregir captura y modificar condiciones contractuales. Exigir justificación para correcciones conforme a F0. Si el cambio es un convenio, bloquear su aplicación en este flujo y conservar la propuesta; no implementarlo mediante reimportación.
5. Dentro de una transacción breve por bloque: comprobar revisión, aplicar cambios selectivos con las validaciones del agregado, registrar auditoría y completar la confirmación. Actualizar contenido y resultado idempotente de manera atómica en DB.
6. Autorizar/contextualizar y consultar primero si la misma confirmación ya terminó: petición idéntica devuelve su resultado original, aunque su propia escritura haya cambiado el ETag. Misma clave con otra intención produce conflicto. Una nueva confirmación sí debe comprobar el ETag vigente.
7. Un 400/412 conserva decisiones/captura. Un bloque confirmado de obra no se repite ni revierte porque falle el de cliente; mostrar el estado parcial real y permitir corregir el bloque pendiente.

**Archivos previstos:** `ConfirmWorkExtractionService`, `ConfirmClientExtractionService`, comandos selectivos en módulos propietarios, registro de confirmaciones y auditoría.

**Salida:** pruebas reales de permisos separados, cancelación, corrección justificada, ausencia de escrituras parciales dentro del bloque e idempotencia después de pérdida de respuesta.

### F8 — Confirmar presupuestos y publicar colecciones consultables

**Dependencias:** F1–F3 y F5–F6. **Decisiones:** P12–P14, P18, P21–P24, P28, P31–P32.

1. Hacer explícita la intención: presupuesto nuevo o revisión de uno existente. Elegir la agrupación de hojas y orden de sus partes; alternativas son presupuestos distintos, no revisiones fusionadas automáticamente.
2. Validar clave/descripcion/unidad/cantidad/precio/importe según el contrato F0; conservar conceptos sin clave si se permiten, códigos repetidos con identidad propia y evidencia de origen. No crear una clave de catálogo por concepto.
   Los conceptos sin precio ni importe son admisibles cuando no se hayan registrado; un valor ausente no es cero. Admitir conceptos sueltos y un primer nivel de partidas, sin asumir subpartidas. Conservar descripción completa, orden, texto original de medición/unidad y notas comerciales separadas; las continuaciones o mediciones ambiguas se sugieren para revisión.
3. Registrar cada presupuesto como bloque de confirmación independiente y atómico. Una revisión incluye cabecera, partidas, conceptos y resultados de comprobación; un fallo revierte ese bloque completo.
4. Conservar datos precisos y valores/cálculos de evidencia separados. La propuesta DP-27 permite aceptar diferencias interpretables con justificación y bloquear valores inválidos, pero queda por confirmar su aplicación a cada campo y los casos que impiden una revisión coherente. No corregir las sumas de la fuente ni convertir un total de presupuesto en monto contratado de la obra automáticamente.
5. Asignar número monotónico dentro del presupuesto y no modificar revisiones confirmadas. Reimportar no actualiza una revisión en sitio; volver a confirmar la misma clave no crea otra.
6. Elegir revisión principal solo por acción explícita bajo ETag del presupuesto. No seleccionar por fecha de archivo, orden de hoja o «última importada»; tampoco confundir una revisión principal con un presupuesto universal para todas las alternativas de una obra.
7. Exponer cabecera y conceptos/partidas ordenados y paginados, con IDs de presupuesto/revisión y contratos tipados. Este es el producto consumible por la futura generación, sin definir Jinja, renderizado o exportación.

**Archivos previstos:** `budgets/*`, `ConfirmBudgetExtractionService`, stores/consultas de revisión y pruebas de persistencia/concurrencia.

**Salida:** mismo libro puede aportar partes de un presupuesto y alternativas independientes; versiones previas y fuentes permanecen intactas y consultables con los permisos correspondientes.

### F9 — API y ventana de extracción JavaFX

**Dependencias:** contratos estables de F3–F8; se permiten prototipos con API simulada sin declarar aceptación. **Decisiones:** P01, P05, P10, P12, P15, P22, P25–P28.

1. Implementar endpoints de trabajos, estructura fuente, candidatos, borradores, perfiles y confirmaciones, con límites de página y errores ProblemDetail/CSRF vigentes. No exponer entidades JPA, rutas internas o texto sensible en logs.
2. Añadir entrada **Extraer datos** desde obra/adjunto sin habilitar Generar documentos ni sustituir las cuatro acciones existentes. La ventana identifica obra, cliente, fuente y trabajo en curso.
3. Dividir UI en carga/selección de fuentes, estado de análisis, mapeo/revisión, presupuestos y resumen de confirmaciones; controladores pequeños, red fuera del hilo JavaFX y tablas virtualizadas/paginadas.
4. Mostrar evidencia mínima junto al candidato, valor actual, interpretación tipada, alternativas y diagnóstico. Para PDF mostrar página/extracto y, si se aprueba el visor de F0, representación inerte de fuente; para DOCX usar ubicación estructural y texto, no fingir paginación Word.
5. Permitir elegir hojas, rangos, columnas, partes/resúmenes/alternativas, excluir filas y guardar/reutilizar perfiles. Mantener fórmulas como evidencia inerte, sin ejecutarlas ni convertirlas en instrucciones del visor.
6. Guardar borrador en servidor y recuperar listas de trabajos al abrir. Consultar progreso con intervalos acotados; impedir respuestas tardías de otra sesión/trabajo, múltiples confirmaciones accidentales y actualizaciones sobre una ventana que ya cambió de contexto.
7. Presentar botones separados de confirmar Obra, Cliente y cada Presupuesto, con permisos/estados correctos y advertencia de cambio compartido. Guardar las revisiones base y manejar 412 sin perder captura ni reintentar escritura automáticamente.
8. Mostrar Analizando, Guardando revisión, Parcialmente confirmado y Confirmado solo cuando correspondan. El cierre local inmediato sigue siendo un acuse local, no una promesa de OCR instantáneo.

**Propuesta de contratos HTTP a concretar en F1/F9:**

| Operación | Ruta orientativa | Respuesta/condición |
|---|---|---|
| Crear extracción con fuentes y opciones | `POST /api/v1/works/{workId}/extractions` | 202, ID durable y enlace de estado; clave idempotente. |
| Listar trabajos de obra | `GET /api/v1/works/{workId}/extractions` | Paginación y contexto verificado. |
| Consultar estado/intento | `GET /api/v1/extractions/{id}` | Estado/progreso/revisión sin rutas privadas. |
| Consultar estructura y candidatos | `GET /api/v1/extractions/{id}/sources` y `/candidates` | Fuentes/candidatos paginados con evidencia mínima. |
| Guardar revisión | `PUT /api/v1/extractions/{id}/review` | `If-Match`; retorno de revisión nueva. No guardar contenido ilimitado de una sola vez. |
| Reanalizar o cancelar | `POST /api/v1/extractions/{id}/attempts` o `/cancellation` | Intención explícita; no ejecutar cambios de negocio. |
| Confirmar un bloque | `POST /api/v1/extractions/{id}/confirmations` | Tipo/destino explícitos, revisión base y clave idempotente; resultado por bloque. |
| Consultar presupuestos/revisiones | `GET /api/v1/works/{workId}/budgets`, `/budgets/{id}/revisions` y `/budget-revisions/{id}/items` | Pertenencia y paginación estable; ninguna selección de plantilla. |
| Elegir revisión principal | `PUT /api/v1/budgets/{id}/principal-revision` | `If-Match` y elección explícita. |

Los nombres finales y la modalidad de clave idempotente se documentan antes de implementar, manteniendo `/api/v1` y contratos del proyecto. Referencias por ID no sustituyen comprobación de permisos/pertenencia. Si un bloque es procesado en la petición, no indicar 202 como si siguiera pendiente; documentar 200/201 según resultado. Los timeouts de análisis se resuelven con trabajos, no alargando indefinidamente el HTTP del cliente.

**Salida:** recorrido completo de carga, análisis, revisión, guardado, reanudación y confirmaciones independientes sin documentos generados ni acceso directo del cliente a DB/almacenamiento.

### F10 — Regresión, seguridad y aceptación anotada/manual

**Dependencias:** F2–F9. **Decisiones:** P33-A + validación manual de B.

1. Ejecutar pruebas unitarias de normalización, mapeo, precedencia, tipos, calendarios/cronología y comprobación económica separada. Las pruebas de nombres técnicos de SPEC001 no se relajan al agregar denominaciones.
2. Integrar lectores/procesos con fixtures construidos anotados: PDF digital/escaneado/mixto, XLS/XLSX y DOCX sin marcadores; medir candidatos correctos, omisiones y asociaciones incorrectas por campo/fila.
3. Probar ingreso hostil y límites: contenedores cifrados/dañados, expansiones desmedidas, imágenes enormes, macros/vínculos, salida excesiva, timeout y cancelación de hijos. Verificar que no hay acceso de red ni DB desde lectores.
4. Probar PostgreSQL limpio y actualizado desde V14, claves externas, auditoría, atomicidad por bloque, números de revisión y selección principal. Conservar archivos y datos anteriores.
5. Probar autorización real y CSRF, fuentes de otra obra, lectura sin escritura, intento de confirmar cliente con solo `WORKS_WRITE`, y baja de permiso entre analizar y confirmar.
6. Probar dos revisores, ETag obsoleto de cliente/obra/presupuesto/catálogo, idempotencia antes/después de un timeout y bloque de obra confirmado con cliente todavía fallido. Reiniciar backend durante lectura/publicación; jamás repetir confirmación de negocio.
7. Probar fuente eliminada lógicamente después de analizar/confirmar: conservar evidencia histórica y bytes, impedir reutilización ordinaria no autorizada y no revocar valores confirmados por cascada.
8. Verificar JavaFX real: navegación, mapeo, tamaño/paginación de tablas, reanudación, permisos, fallos conservando captura y ausencia de bloqueo del hilo de interfaz.
9. Ejecutar validación manual con archivos reales autorizados y resultados esperados anotados. Distinguir aceptación del extractor, calidad OCR y corrección del mapeo humano. Registrar limitaciones y no convertir el resultado de un archivo en compatibilidad universal.
10. Ejecutar Maven Wrapper con Docker y entorno Python de SPEC001 disponible para su regresión: `./mvnw test`, `./mvnw verify` y `git diff --check`. No omitir pruebas por dependencia OCR/lector ausente; documentar preparación del entorno. La aceptación de esta fase no requiere ejecutar `clean` ni tocar la base persistente.

**Archivos previstos:** pruebas en paquetes espejo de servidor/cliente, fixtures anonimizados/construidos, `specs/002/manual-test.md` y `specs/002/review.md` con evidencia real y pendientes.

**Salida:** suite completa sin fallos/omisiones no justificadas, métricas respecto de los umbrales F0 y aceptación manual registrada; no inventar cifras ni equivaler compilación a aceptación visual.

### F11 — Documentación operativa y despliegue autorizado

**Dependencias:** F10 y autorización específica del destino. **Decisiones:** P29–P31, P33.

1. Documentar instalación de lectores Java/OCR, idiomas, límites efectivos, variables del servidor, ubicación de temporales y diagnóstico de trabajos fallidos sin revelar documentos/credenciales.
2. Actualizar SPEC002, README/MEMORY y guía operativa con estado comprobado y enlaces; señalar qué quedó implementado y qué sigue diferido. No afirmar que el motor genera documentos.
3. Antes de usar `brica`, confirmar identidad/esquema actuales, estado del servicio y autorización para migrar/reiniciar; respaldar DB y volumen documental conjuntamente en destino privado durable y comprobar recuperación en un entorno aislado.
4. Aplicar solo migraciones nuevas autorizadas y desplegar la imagen con los binarios/modelos aprobados, conservando volúmenes/rutas previas. Validar capacidades al arrancar; si OCR falta, informar indisponibilidad real y no declarar trabajos OCR completados.
5. Comprobar cuentas/permisos existentes, salud, consulta de datos anteriores y un recorrido autorizado sin sembrar documentos de demostración indiscriminadamente. Probar reinicio/recuperación y verificar hashes/relaciones conservados.
6. Habilitar tareas de reconciliación/retención únicamente con la política aprobada. La recuperación de un archivo ausente informa inconsistencia: no publica un archivo vacío ni borra su referencia para esconder el problema.
7. Entregar evidencia de instalación, respaldos y límites, sin secretos. Mantener generación deshabilitada. Preparar commits/PR solo ante solicitud expresa posterior.

**Salida:** extractor instalado en el destino autorizado, operación recuperable y aceptación separada de cualquier futura generación.

## 7. Trazabilidad de aceptación

Los CA/MD originales se conservan como referencias históricas; el texto escrito de `spec.md` requiere la alineación prevista en F0. Los criterios EX siguientes son propuestos para esta implementación, sin apropiarse de IDs de SPEC001.

| Criterio | Resultado verificable | Referencias | Fases |
|---|---|---|---|
| EX-01 | PDF digital/escaneado/mixto produce candidatos y evidencia, sin escritura de negocio. | CA-01–CA-04, MD-001–MD-002; P07–P09 | F3–F4, F10 |
| EX-02 | XLS/XLSX muestra hojas, permite rangos/columnas, distingue conceptos/resúmenes y conserva resultados de fórmula sin evaluarla. | CA-06–CA-10, MD-003; P10–P12 | F5, F9–F10 |
| EX-03 | DOCX fuente se lee sin requerir marcadores; su original y analizador de plantillas quedan intactos. | P02-C, P09-A | F5, F10 |
| EX-04 | Mapeo asistido y precedencia conservan alternativas; unknowns solo se asocian a adicionales existentes compatibles. | CA-03/CA-11–CA-13; P04–P06/P19 | F6, F9–F10 |
| EX-05 | Denominación literal y nombre técnico coexisten; claves/plantillas antiguas no cambian significado. | P16-B y SPEC001 | F2, F7, F10 |
| EX-06 | Fechas y decimales se interpretan con configuración explícita; diferencias de cálculo no sobrescriben importes fuente. | CA-08/CA-13; P17–P18 | F6, F8, F10 |
| EX-07 | Cada destino confirma solo lo aceptado, con permiso, ETag, auditoría y justificación cuando corresponde. | CA-11–CA-14, MD-004; P20–P23/P28 | F7–F10 |
| EX-08 | Confirmar obra no confirma cliente/presupuestos; cancelar no revierte bloques ya aplicados. | P22-B/P26-B | F7–F10 |
| EX-09 | Presupuestos conservan partes/alternativas, revisiones inmutables, orden y principal explícita; conceptos consultables paginados. | CA-09/CA-15, MD-005; P12–P14/P32 | F8–F10 |
| EX-10 | Reintento idéntico devuelve el resultado original; reanálisis y nueva intención no duplican confirmaciones ni destruyen decisiones previas. | P24-A/P31-A | F3, F6–F8, F10 |
| EX-11 | Cerrar cliente/reiniciar servidor conserva revisión y recupera trabajos sin aplicar datos automáticamente. | P25-B/P26-B | F3, F9–F11 |
| EX-12 | Fuente eliminada lógicamente conserva evidencia protegida; pertenencia y límites impiden accesos/lecturas indebidos. | CA-05, MD-014; P29–P31 | F1, F3, F10–F11 |
| EX-13 | Suite anotada y recorrido manual demuestran límites conocidos y calidad acordada, sin promesa universal. | P03-B/P33-A+B | F0, F10–F11 |
| EX-14 | No se producen DOCX/PDF de salida, no se habilita Generar ni se alteran versiones/paquetes como efecto de extraer. | P32-A e instrucción de alcance | Todas |

**Diferido, no cerrado:** HU-004–HU-006, CA-16–CA-30 y MD-006–MD-013 en sus aspectos de selección/renderizado/generación/exportación. La parte de validación de datos y consulta de colecciones se verifica por EX-04/EX-06/EX-09, no se declara cumplida toda la generación por disponer de esos contratos.

## 8. Secuencia de entrega y control de avance

```text
F0 → F1 → F2
      └→ F3 → F4 ┐
             F5 ┴→ F6 → F7 ┐
                     └→ F8 ┴→ F9 → F10 → F11
```

F6 consume F2 y los lectores de F4/F5. F8 puede empezar su modelo con F1, pero su confirmación necesita revisión/normalización. La UI se puede prototipar antes sin declarar que los servicios ya existen. No se despliega una fase con migraciones o dependencias pendientes de aprobar.

| Entrega técnica | Unidad coherente | Condición de cierre |
|---|---|---|
| E0 | Alcance alineado y ficha técnica/corpus | Decisiones concretas de §5 aprobadas. |
| E1 | Modelo, migraciones, denominaciones y concurrencia | Base limpia/actualizada y regresión de SPEC001. |
| E2 | Fuentes, trabajos y procesos controlados | Recuperación, cancelación y límites comprobados. |
| E3 | Lectores PDF/OCR/Excel/DOCX | Corpus anotado por formato; ninguna confirmación implícita. |
| E4 | Mapeo y borradores | Revisión persistente, evidencia, precedencia y tipos. |
| E5 | Confirmaciones y presupuestos | Atomicidad por bloque, idempotencia, historial y consultas. |
| E6 | JavaFX y aceptación integral | EX-01–EX-14, pruebas automatizadas y manuales registradas. |
| E7 | Puesta en servicio | Autorización de destino, respaldo y evidencia operativa. |

Al implementar, separar cambios y pruebas por estas unidades manteniendo estados verificables; no mezclar la limpieza de archivos locales o los cambios pendientes de SPEC001 con una entrega de SPEC002. Este documento no autoriza commits, push, PR, cambios de rama ni modificaciones del sistema persistente.
