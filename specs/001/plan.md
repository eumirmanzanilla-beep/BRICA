# Plan de implementación — Spec 001

**Funcionalidad:** plantillas, versiones, paquetes y campos de referencia.

**Fecha:** 2026-11-09.

**Estado:** implementación hasta F6 y correcciones ampliadas con casos construidos. Catálogo/seguridad, análisis, versiones, paquetes, atributos adicionales y diálogos de escritorio implementados. La evidencia actual y los recuentos de pruebas están en `review.md`; los resultados históricos no representan la suite ampliada. Aceptación Windows/Word pendiente. No se inicia F7 ni el motor de Spec 002.
**Especificación:** [spec.md](spec.md), requisitos CD-001–CD-007 y criterios CA-01–CA-42.

**Seguimiento:** [review.md](review.md), con tareas individuales, decisiones confirmadas y evidencia de cada avance.

**Punto de control:** se permite avanzar técnicamente con fixtures construidos y permisos reales en PostgreSQL desechable. F0/F1 no se declaran aceptadas de extremo a extremo mientras falten la plantilla conjunta y el recorrido manual. Los estados Borrador/No disponible siguen visibles y administrables, pero no son elegibles para confirmar paquetes ni generar.

**Actualización vigente:** el responsable amplió D-01/D-03 a todos los atributos fijos de Obra/Cliente y adicionales TEXT/DATE/AMOUNT por obra, fecha española e importes precisos separados de su presentación. Aprobó estados de obra y selectores con permisos independientes, diálogos, unicidad exacta global de plantillas y borrado de campos sin referencias. La sección histórica de decisiones de abajo no vuelve a restringir el alcance a nombres. D-04/D-06/D-12 conservan el proceso Python local, almacenamiento de prueba y tipo documental, sin implementar el motor.

**Protección de desarrollo:** no aplicar V8–V10 ni sustituir cuentas de desarrollo. Verificar únicamente en PostgreSQL desechable y ofrecer un entorno aislado de revisión mediante `tools/review-environment.py`. Roles aprobados: Gerencial 4 y Administrativo 3, mismos permisos actuales; Practicante 2 e Invitado 1, solo lectura. Propuestas/aprobación fuera de este sprint. No hacer commit, push, merge ni cambiar de rama.

**Correcciones de escritorio:** completar diálogos de paquetes y valores, conservar captura al registrar plantilla y evitar peticiones/cierres simultáneos. Separar captura de datos documentales de obra en `WorkDocumentInformationDialog`, con fechas estrictas y entradas por definición adicional; mantener errores del servidor sin perder captura. Preservar `If-Match` y resolución exclusivamente desde la API. Ejecutar pruebas de servidor/cliente, carga real JavaFX separada de aceptación Windows y actualizar la guía vigente.

**Sustitución aprobada el 2026-11-13:** se autoriza la base persistente `brica`, nuevas migraciones y sustitución explícita de las tres cuentas antiguas por las dos propuestas, después de respaldo. La restricción anterior de no aplicar a desarrollo queda superada por esta autorización; se mantienen sin commit/push/cambio de rama. La aplicación habitual deja de usar el entorno temporal. Nuevas tareas: V11 ubicación/fechas opcionales, V12 ámbito obra/cliente y adjuntos, V13 nombres canónicos y campos fijos internos; navegación de Obras con cuatro iconos, asistente de tres pasos, configuración contextual, catálogo único y cierre local asíncrono con recuperación de captura. DOCX/PDF/XLSX, sin extracción; generación definitiva sigue diferida.

## 1. Objetivo, fuentes y límites

Implementar la configuración documental, desde un catálogo seguro de atributos hasta un paquete confirmado consumible por el futuro motor. Demostrar el recorrido con una obra y su cliente existentes, dos versiones de una plantilla y valores resueltos desde su fuente vigente.

Fuentes efectivas: `AGENTS.md`, `docs/constitution.md`, únicamente la spec 001, POM, Compose, configuración, migraciones y código actual. Se sigue el flujo de `.opencode/commands/feature.md`: inspección, plan aprobado, implementación acotada y evidencia; revisión posterior de solo lectura según `/review`.

**Actualización de fuentes al iniciar la implementación:** el responsable incorporó contenido a `docs/ERS_BRICA.docx` y `docs/DAS_BRICA.docx`, añadió `docs/DiccionarioDatos_BRICA.docx` y solicitó utilizarlos. Los tres fueron leídos y contrastados con la Spec 001 y el código. Esta instrucción sustituye la exclusión temporal utilizada al redactar el plan. ERS CU-03/CU-08 y DAS Configuración documental/Seguridad/Archivos sustentan el bloque; las discrepancias y decisiones pendientes se registran en `review.md`. No modificar contratos de negocio previos para ajustarlos silenciosamente al Diccionario.

### Incluido

- Catálogo con claves canónicas, atributos aprobados y edición descriptiva.
- Resolución administrativa de valores y resolución de una selección aplicable al motor.
- Análisis DOCX, clasificación por campo, documentos estáticos y confirmación revisable.
- Versiones inmutables, archivos, recuperación e idempotencia mediante adaptador de archivos del servidor; validación preliminar con casos construidos, sin aceptación del despliegue definitivo.
- Plantillas activas/inactivas, eliminación lógica e historial de versiones.
- Paquetes asociados a un cliente, sin operación/etapa ni secuencia de generación; borradores persistentes y edición concurrente como objetivo del flujo completo.
- API y cliente JavaFX, pruebas y permisos reales como condición de habilitación.

### Excluido

- Extracción PDF/OCR, renderizado, previsualización documental y exportación: Spec 002.
- Ejecuciones de generación y su historial completo: definir el contrato ahora, persistirlo con el motor. No crear generaciones ficticias para declarar aprobados sus criterios.
- Paquetes generales sin cliente, el concepto de operación/etapa documental, reglas contractuales por convenio y cambios amplios de Clientes, Obras o Contratos.
- Nuevas fuentes libres, expresiones evaluadas, acceso a PostgreSQL o archivos desde JavaFX.
- Almacenamiento y despliegue definitivos. Fechas e importes sí están incluidos en las correcciones aprobadas; no se incorporan cálculos fiscales ni conversión de moneda.

## 2. Diagnóstico contrastado con el repositorio

| Área | Situación comprobada | Consecuencia |
|---|---|---|
| Servidor | `com.brica.clients`, `works`, `contracts`; capas `api/application/domain/infrastructure`. | Crear funcionalidad documental respetando estos límites, sin reorganizar módulos existentes. |
| Obra | `Work`: cliente, nombre, ubicación, descripción, fechas y borrado lógico. | Resolver usando relaciones existentes; no copiar valores de negocio a la configuración. |
| Cliente | `Client`: nombre, representante legal y borrado lógico. | El cliente para resolver proviene de la obra, nunca de un identificador enviado para sustituir esa relación. |
| Correcciones | `CorrectWorkService` bloquea la fila y comprueba `WorkRevision` / `If-Match`; solo corrige información general. | Preservar ese control. CA-04 necesita decidir cómo corregir una fecha, hoy sin caso de uso público equivalente. |
| Contrato | `Contract.data` es JSONB; existe registro por API. | No convertirlo en un origen libre ni asumir que contiene importes contractuales aprobados. |
| Esquema | V1–V4: `cliente`, `works`, `contracts`, eliminación lógica. | No hay tablas documentales ni columna de marcador que alterar actualmente. Crear migraciones posteriores a V4, verificando nuevamente la numeración. |
| Errores | `ApiExceptionHandler` devuelve `ProblemDetail`, `code`, `suggestion` y errores por campo; maneja 400/404/409/412/428. | Extender el contrato, no crear una envoltura paralela. Seguridad requiere además respuestas desde la cadena de filtros. |
| Seguridad | Sin dependencia ni configuración de autenticación/autorización en el servidor. | Dependencia bloqueante para habilitar y aceptar el módulo, no cubierta con permisos simulados. |
| Archivos y DOCX | Sin almacenamiento ni analizador documental existentes. | Definir puertos y evaluar implementación/dependencias antes de incorporarlas. |
| Escritorio | `MainController`, FXML de clientes/obras y clientes HTTP asíncronos; Contratos provisional. | Añadir pantallas pequeñas; mantener tareas de red y archivos fuera del hilo JavaFX. |
| Pruebas | JUnit, pruebas MVC y PostgreSQL 18 con Testcontainers; ejemplo HTTP real en `ContractDockerHttpIntegrationTest`. | Reutilizar patrones y agregar pruebas de fallos y concurrencia reales. |

El diagnóstico de la tabla corresponde a la planificación original. Al iniciar la implementación se verificó con DBHub, en solo lectura, PostgreSQL 18.6, base `brica`, usuario `brica_mcp`, tablas de negocio existentes y V1–V4 aplicadas. Se contrastó la identidad con el contenedor de Docker Desktop por versión, base y fecha exacta de inicio de PostgreSQL. La configuración efectiva actual publica el contenedor en el puerto 5432 del host; 5433 sigue siendo el valor por defecto de Compose, no el puerto observado. V5 se verifica en bases desechables de pruebas; no se aplica manualmente a desarrollo desde DBHub.

Estado posterior comprobado: V5 y V6 existen en código; catálogo de nombres y seguridad están implementados, y el backend de desarrollo llegó a V6. El 2026-11-10 se ejecutaron `test` y `clean verify`: 130 servidor + 14 cliente, sin fallos, errores ni pruebas omitidas. La entrega se separa en convenciones (#22) y catálogo protegido (#23), ambos desde `main`, sin cerrar Spec 001 ni validar visualmente JavaFX.

## 3. Decisiones y bloqueos previos (registro histórico; ver actualización vigente arriba)

La aprobación para redactar este documento no aprueba automáticamente estas propuestas técnicas.

| ID | Propuesta / pregunta | Punto de control |
|---|---|---|
| D-01 | **Aprobado:** comenzar solo con `WORK_NAME` → `Work.name`, `CLIENT_NAME` → `Work.client.name`, ambos texto. | Fechas e importes diferidos expresamente. Las claves de usuario pueden ser otras, como `NOMBRE_OBRA`; estos son identificadores de atributo del servidor. |
| D-02 | **Resuelta:** eliminar completamente operación/etapa documental. Cada paquete sigue asociado a un cliente; sus plantillas se pueden generar en cualquier orden y momento, sujetas a permisos/elegibilidad. | Sustituye la antigua propuesta `START/ESTIMATE/AGREEMENT/CLOSURE`. Quitar ese dato de los futuros DTO, filtros y restricciones; no crear el catálogo. Las fuentes heredadas se alinearán por separado, sin editar DOCX ni Spec 002 en esta tarea. |
| D-03 | **Aprobado para esta entrega:** mantener texto sin alterar contenido y únicamente nombres. Se conservan las propuestas futuras del responsable: fecha `{{dd}} de {{mes (1 - enero, ..., 12 - diciembre)}} del {{aaaa}}` (12 de octubre de 2029); importes con cuatro decimales y MXN/USD, visualización de 2 decimales, cálculo interno de 6 y redondeo hacia arriba de 1 centavo. | Fechas, importes, moneda y sus cálculos se difieren; las distintas precisiones y su relación con DECIMAL(15,2) deben cerrarse al retomar esa entrega. No se aplican ahora ni se interpretan como sintaxis de nuevos marcadores. |
| D-04 | **Aprobada:** docxtpl 0.20.2 mediante proceso Python local del backend, protocolo JSON por stdin/stdout, sin servicio adicional ni acceso a PostgreSQL. | Dependencias fijadas en `brica-server/src/main/python/requirements.txt`. Normalizar marcadores divididos solo en copia privada; validar sintaxis canónica antes de usar docxtpl. No implementar generación de Spec 002. |
| D-05 | **Aprobado A1/B1/C1/D1:** sesiones en memoria de servidor, cookies/CSRF, protección de todas las API de negocio, BCrypt y administración local. Un rol por usuario y permisos por rol según Diccionario/DAS. | Sin vencimiento por inactividad ni límite temporal absoluto. Contraseña mínima 8 caracteres, mayúscula y número; coste 10; máximo técnico 72 bytes sin truncar. Identificador de usuario exacto. Usuario ACTIVE/INACTIVE; rol 1/0. Comprobar permisos/estados vigentes por operación. |
| D-06 | **Prueba aislada aprobada:** ruta configurable del servidor, archivos construidos y metadatos persistentes. Puerto `DocumentFileStore`, adaptador `ServerDocumentFileStore` y guía `storage-test.md`. | La prueba local usa raíz configurable y volumen Docker exclusivamente de desarrollo. No decide servidor final, retención, limpieza ni respaldo. Los límites conservadores de procesamiento son controles del preliminar, no capacidad operativa aceptada. |
| D-07 | **Resuelta:** comparación exacta del nombre de paquete, dentro del mismo cliente. No recortar extremos ni convertir mayúsculas automáticamente. | Sustituye la normalización propuesta. Preservar las validaciones de presencia/longitud del Diccionario; operación/etapa ya no participa en unicidad. |
| D-08 | **Aprobado:** no se permiten operaciones futuras sobre una obra o cliente eliminado; se deben conservar todos los archivos históricos generados con su información. | La resolución nueva excluye obra y cliente eliminados. No implementar cascadas de borrado; la verificación de archivos históricos reales corresponde a fases posteriores. |
| D-09 | **Alcance confirmado:** cambios contractuales por convenio fuera del flujo, para una entrega futura. | No ampliar `general-information` ni asumir aprobada la corrección ordinaria de fechas. CA-04 ahora es genérico: falta un origen opcional real para aceptarlo con el mínimo de nombres obligatorios. |
| D-10 | **Estructura resuelta por Diccionario:** OBRA contiene `monto_contratado`, `monto_antes_iva` e `iva`, DECIMAL(15,2), nullable. Todavía no están implementados en `Work`. | No preguntar de nuevo si existirán. Falta planificar su incorporación y la regla de validación/formato; se mantiene el recorte aprobado de solo nombres hasta esa entrega. No usar JSON de Contrato como fuente alternativa. |
| D-11 | **Aprobado para este bloque:** alta, listado y renombrado; nombre visible de hasta 50 según Diccionario. Persistir clave, tipo, origen y ruta asignados por atributos aprobados. Sin edición semántica ni borrado por ahora. | Asociaciones V7 reales comprobadas; CA-05/40 mantienen recorrido manual pendiente. No ampliar la edición semántica de campos no vinculados. |
| D-12 | **Resuelta:** tipo documental es identidad suficiente para agrupar alternativas y exigir una selección por tipo. | Reutilizar `document_type`, sin nueva entidad ni catálogo de operaciones. Los valores se comparan exactamente, sin normalización automática. |
| D-13 | **Aprobado:** alinear `VERSION_PLANTILLA_CAMPO.marcador` de 50 a 54 en el Diccionario, conservando el resto del DOCX. | Aplicado mediante modificación mínima de una celda; verificar que futuras asociaciones SQL admitan 54. No se crea todavía esa tabla. |

Si un bloqueo no está resuelto, se pueden desarrollar las reglas aisladas cuyo alcance se apruebe, pero no habilitar la funcionalidad incompleta ni marcarla aceptada. Las decisiones nuevas sustituyen propuestas anteriores, no su evidencia histórica. El Diccionario aún describe `PAQUETE_DOCUMENTAL.operacion_etapa` y la Spec 002 aún exige compatibilidad por operación: dejar registrada esa discrepancia para una alineación separada, sin arrastrar el concepto eliminado al diseño nuevo.

## 4. Diseño y archivos previstos

Rutas y nombres propuestos, a ratificar con cada fase. No requieren un módulo Maven adicional.

La operación técnica de registro/idempotencia mencionada abajo no es una operación/etapa de negocio. D-02 elimina esta última clasificación, no la necesidad de identificar y recuperar una solicitud técnica.

### 4.1 Servidor

Raíz nueva: `brica-server/src/main/java/com/brica/documents/`.

| Subruta | Archivos principales / responsabilidad |
|---|---|
| `domain/` | `ReferenceField`, `ReferenceAttribute`, `ReferenceValue`, `ResolutionStatus`, `DocumentTemplate`, `TemplateVersion`, `TemplateVersionField`, `DocumentPackage`, `RegistrationOperation`; invariantes y estados. |
| `application/` | `RegisterReferenceFieldService`, `UpdateReferenceFieldService`, `ResolveReferenceValuesService`, `AnalyzeTemplateService`, `RegisterTemplateVersionService`, `RecoverRegistrationService`, `ConsultTemplateService`, `ChangeTemplateStatusService`, `SaveDocumentPackageService`, `ConfirmDocumentPackageService`, `ListApplicableDocumentsService`, `ResolveDocumentSelectionService`. |
| `application/port/` | `ReferenceFieldStore`, `TemplateStore`, `DocumentPackageStore`, `RegistrationOperationStore`, `DocumentFileStore`, `DocxAnalyzer`, `WorkReferenceSource`; puertos, sin dependencias de JavaFX. |
| `infrastructure/` | Repositorios/adaptadores JPA, `JpaWorkReferenceSource`, analizador DOCX y almacenamiento local. Metadatos de archivo con entidad/repositorio dedicados. |
| `api/` | `ReferenceFieldController`, `TemplateController`, `TemplateVersionController`, `DocumentPackageController`, `DocumentConfigurationController` y DTO explícitos de solicitud/respuesta. |

Otros archivos previstos:

- `brica-server/src/main/java/com/brica/server/shared/error/api/ApiExceptionHandler.java`: errores documentales, conflictos y recuperación sin exponer rutas físicas.
- `brica-server/src/main/java/com/brica/server/security/`: configuración y adaptación de permisos, solo tras D-05; fijar alcance para no romper accidentalmente las API existentes.
- `brica-server/src/main/resources/db/migration/V5__create_document_configuration.sql` y migraciones siguientes si se divide por fases: esquema nuevo; nunca editar V1–V4. Numeración ilustrativa, volver a comprobar antes de crear.
- `brica-server/src/main/resources/application.yml`: propiedades documentales y límites aprobados, sin secretos.
- `brica-server/pom.xml`: dependencias DOCX/seguridad aprobadas y sus pruebas; no modificar las versiones base sin necesidad autorizada.
- `compose.yml` y `brica-server/Dockerfile`: entorno Python y volumen de archivos exclusivamente de prueba; almacenamiento definitivo diferido por D-06. No modificar `.env`, cuentas ni configuración personal. Se conserva la opción explícita de HTTP local de la entrega F1 anterior.
- Cambios al flujo de fechas de Obras únicamente en una entrega aprobada para D-09, con DTO, servicio, API, cliente y pruebas propios.

### 4.2 Cliente

- `brica-client/src/main/java/com/brica/client/api/`: `ReferenceFieldsApiClient`, `TemplatesApiClient`, `DocumentPackagesApiClient`, `DocumentConfigurationApiClient` y DTO de análisis, solicitud técnica de registro, versión, paquete y valor; sin DTO de etapa de negocio.
- `brica-client/src/main/java/com/brica/client/presentation/`: `ReferenceFieldsController`, `TemplatesController`, `TemplateVersionDialogController`, `DocumentPackagesController`, `DocumentPackageEditorController`, `ReferenceValuesController`; separar diálogos del listado.
- `brica-client/src/main/resources/com/brica/client/presentation/`: FXML equivalentes, por ejemplo `reference-fields-view.fxml`, `templates-view.fxml`, `template-version-dialog.fxml`, `document-packages-view.fxml`, `document-package-editor.fxml`, `reference-values-view.fxml`.
- Modificar `MainController.java` y `main-view.fxml` para navegación documental. Ajustar `styles/app.css` solo si hacen falta estados/componentes coherentes con la interfaz existente.
- Integrar credenciales conforme a D-05 sin incrustarlas en cada cliente HTTP. Evitar un refactor general de todos los clientes como efecto lateral.

### 4.3 Pruebas y documentación

- `brica-server/src/test/java/com/brica/documents/{domain,application,api,infrastructure}/`: clases `*Test` equivalentes a los nuevos componentes.
- `brica-server/src/test/resources/documents/`: DOCX mínimos reproducibles y anonimizados, no archivos de producción. Incluir fixtures de seguridad y regiones no soportadas.
- `brica-client/src/test/java/com/brica/client/`: serialización, interpretación de errores y estado del flujo donde no requiera entorno gráfico.
- `specs/001/plan.md`: decisiones aprobadas y evidencia por fase. `README.md`: instrucciones realmente implementadas. Contrato con `specs/002/spec.md`: actualizar mediante aprobación cuando se implemente el consumidor.

### 4.4 Integración Python aprobada

1. **Descripción:** el responsable eligió docxtpl y aprobó la invocación como proceso local. El backend mantiene autorización, catálogo, clasificación por versión y acceso a datos; Python recibe únicamente los bytes del DOCX y límites de procesamiento.
2. **Efecto:** añade intérprete, empaquetado de dependencias, protocolo de entrada/salida, manejo de fallos y pruebas entre lenguajes. Mantener permisos y resolución de datos de negocio en Java, sin acceso directo desde JavaFX a Python o PostgreSQL.
3. **Solución aprobada:** adaptador `PythonDocxAnalyzer`, máximo dos procesos simultáneos, timeout configurable, salida acotada y errores sin rutas/trazas. El analizador inspecciona OOXML y permite únicamente claves canónicas; docxtpl verifica la copia normalizada. Las pruebas renderizan esa copia solo para demostrar compatibilidad/escape; no hay endpoint ni ejecución persistente de generación.
4. **Pros y contras:** reutiliza docxtpl y evita otro servicio, pero requiere instalar/verificar Python además de Java. Las restricciones de runs se resuelven con una copia normalizada y aliases internos, conservando original y formato fuera del marcador. Se rechazan Jinja libre, regiones no soportadas, macros, relaciones externas y ZIP/XML inseguros. La sintaxis pública sigue siendo `{{CLAVE}}`, incluso claves que empiezan por dígitos.

Referencias: [python-docx](https://python-docx.readthedocs.io/en/latest/) y [restricciones de docxtpl](https://docxtpl.readthedocs.io/en/latest/#restrictions), consultadas mediante Context7. Versiones fijadas y entorno aislado de pruebas instalado; Python también se empaqueta en Docker. La extracción del presupuesto sigue siendo otro flujo.

## 5. Modelo persistente e integridad

Diseño implementado en V7 para casos construidos; su aceptación manual y el despliegue definitivo de D-06 siguen pendientes:

1. **Campo:** ID, clave única, nombre visible, atributo aprobado, origen y tipo determinados por el servidor. Clave `^[A-Z0-9_]{1,50}$`. No evaluar rutas libres/reflexión arbitraria. Mantener la semántica de campos vinculados; cambiar nombre visible no rompe asociaciones.
2. **Plantilla:** identidad, nombre, tipo documental, condición activa y `deleted_at`. La eliminación es lógica y cierra las modificaciones; versiones y archivos permanecen consultables.
3. **Versión:** plantilla, número único por plantilla, archivo, configuración inmutable y resultado de registro. FK al campo y obligatoriedad por versión/campo; registrar todas las apariciones y su ubicación, no duplicar la obligatoriedad por aparición. Marcador de hasta **54 caracteres**, ya alineado en el Diccionario con aprobación D-13. El Diccionario define identidad compuesta `(plantilla, versión)`; cerrar el mapeo antes de F4, sin introducir un ID alternativo silenciosamente.
4. **Archivo:** nombre original para presentación, ruta relativa generada, tamaño, tipo comprobado y SHA-256; no confiar en extensión/MIME ni usar nombre original como ruta. DAS y Diccionario ya disponibles confirman estos metadatos; mantener sus longitudes al implementar F4.
5. **Operación de registro:** clave de idempotencia única en el alcance acordado, huella de solicitud, análisis revisado, decisiones confirmadas, estado `PENDING/COMPLETED/FAILED`, referencias recuperables y error seguro. Separar este estado del estado activo de la plantilla.
6. **Paquete:** cliente, nombre, estado `DRAFT/CONFIRMED` y revisión de concurrencia; relación única paquete/identidad de plantilla. Sin atributo operación/etapa ni secuencia de uso. Confirmados requieren cliente existente y al menos una plantilla elegible. Borradores toleran captura parcial y se conservan en servidor. Nunca devolverlos como aplicables.
7. **Restricciones:** FK y unicidad de clave, `(template_id, version_number)`, clave de idempotencia y `(package_id, template_id)`; unicidad exacta del nombre por cliente conforme a D-07. Índices por elegibilidad, cliente y recuperación de solicitudes pendientes, justificados con consultas reales.
8. **Transacciones:** composición del paquete y su revisión se guardan atómicamente. El sistema de archivos no participa en la transacción PostgreSQL: usar estado recuperable y publicación controlada, no asumir rollback del archivo.

Migrar sobre base vacía y desde V4 con clientes, obras y contratos existentes. No modificar esos valores ni sus relaciones. Probar `ddl-auto=validate`, FK, longitudes, rollback y que históricos no queden sujetos a cascadas de borrado.

## 6. Casos de uso y funciones

| Función propuesta | Validaciones / resultado |
|---|---|
| `listApprovedAttributes()` | Lista cerrada del servidor con identificador, origen, tipo y etiqueta; no exponer rutas arbitrarias. |
| `registerField(command)` / `updateField(id, command)` | Clave exacta, unicidad también en DB, atributo aprobado, semántica bloqueada al vincularse; nombre visible editable. |
| `resolveForWork(workId, fieldIds)` | Obra y cliente asociado vigentes según D-08, lectura transaccional consistente y sin escritura de negocio. Clasificar presente/faltante/inválido antes de convertir a texto. Fallo de lectura es error de consulta. |
| `formatReferenceValue(typedValue, type)` | Conversión común para consulta y futuro motor; origen conserva su tipo. `0` y `false` válidos están presentes, espacios únicamente están faltantes. |
| `analyzeDocx(content)` | Validar contenedor, regiones, sintaxis y catálogo; devolver huella, diagnósticos, ubicaciones, apariciones y propuesta de obligatoriedad. |
| `proposeRequiredness(previousVersion, detectedFields)` | Primera versión: exigir clasificación completa. Revisión: heredar conservados como propuesta editable, clasificar nuevos, excluir retirados. No tocar la versión previa. |
| `confirmVersion(command, confirmationId)` | Reanalizar bytes y asociaciones; comparar contra lo revisado. Si cambió, devolver nuevo análisis sin publicar y exigir nueva revisión. Rechazar duplicación conflictiva de la clave de idempotencia. |
| `getRegistrationStatus(id)` / `recoverRegistration(id)` | Retornar resultado original o estado; verificar hash, archivo, metadatos y vínculos antes de completar. Si no es recuperable, fallar con instrucciones de reintento. |
| `getVersionDetails(templateId, versionId)` | Número, estado, campos, marcadores, obligatoriedad y archivo disponible; modo histórico de solo lectura, también para plantilla eliminada. |
| `changeTemplateStatus(id, command)` / `softDeleteTemplate(id)` | No borrar referencias ni archivos. Rechazar modificación de eliminadas; excluir inactivas/eliminadas de selecciones nuevas. |
| `saveDraft(command)` / `discardDraft(id)` | Captura y selección persistentes; descartar no debe borrar versiones ni archivos de plantilla. Validar identidades enviadas y D-08. |
| `confirmPackage(command, expectedRevision)` / `updatePackage(...)` | Sin duplicados, nombre único por comparación exacta dentro del cliente, composición elegible y revisión vigente. Tras confirmar, cliente inmutable. No exigir operación/etapa. |
| `listApplicable(workId)` | Usar cliente asociado; solo paquetes confirmados y plantillas activas no eliminadas, con última versión completada verificable. Orden visual: nombre ascendente e ID para desempate, sin condicionar orden/momento de generación. |
| `resolveSelection(workId, packageId, selections)` | Revalidar aplicabilidad en servidor. Una plantilla/versión por documento; revisión última válida y perteneciente a esa plantilla. Devolver solo campos de la versión elegida; estático devuelve lista vacía. |

### Contratos API propuestos

Usar `/api/v1`, DTO explícitos y el contrato de errores actual. Confirmar nombres finales al implementar; no exponer entidades JPA.

| Recurso / operación | Ruta propuesta | Permiso |
|---|---|---|
| Atributos y campos | `GET /document-attributes`, `GET/POST /reference-fields`, `PUT /reference-fields/{id}` | Administración de configuración. |
| Valores administrativos | `POST /reference-values/resolve` | Consulta global de valores, separada de administración. |
| Plantillas y detalle | `GET/POST /document-templates`, `GET/PUT/DELETE /document-templates/{id}` | Administración. |
| Análisis / confirmación | `POST /document-templates/{id}/analyses`, `POST /document-templates/{id}/versions` | Administración; carga multipart y clave de confirmación según contrato aprobado. |
| Versiones / solicitudes técnicas de registro | `GET /document-templates/{id}/versions`, `GET /document-templates/{id}/versions/{versionId}`, `GET /document-registrations/{id}` | Administración. |
| Paquetes y borradores | `GET/POST /document-packages`, `GET/PUT /document-packages/{id}`, `DELETE /document-packages/{id}/draft`; confirmar mediante `confirm: true` en POST/PUT | Administración; edición/descarte con `ETag` / `If-Match`. |
| Opciones / selección para motor | `GET /works/{id}/document-options`, `POST /works/{id}/document-selection/resolve` | Consulta de valores; no concede generación o administración. Sin parámetro de operación/etapa. |

Cada ruta se entiende prefijada por `/api/v1`. Lista de campos no debe incluir valores de obras. El usuario con consulta sin administración necesita las opciones y los valores, no acceso al CRUD del catálogo. El permiso para generar se definirá con Spec 002.

Respuesta por campo: `marker`, `key`, `type`, `textValue`, `origin`, `required`, `status`; incluir identificadores de plantilla/versión y contexto de selección. Distinguir valor ausente de fallo de lectura. Para documento estático, indicar esa condición expresamente y devolver `fields: []`.

Errores: 400 entrada/archivo/marcadores inválidos; 401 no autenticado; 403 sin permiso; 404 recurso inexistente; 409 unicidad, confirmación desactualizada o selección no vigente; 412 revisión de paquete obsoleta; 428 falta precondición; 503 analizador/almacenamiento no disponibles o completado pendiente. El registro devuelve 200 con la operación y su estado, también en reenvíos, sin crear otra versión. Crear plantilla/paquete devuelve 201; 409 de revisión incluye análisis actualizado en `ProblemDetail`. Recuperación administrativa explícita mediante `POST /document-registrations/{id}/recover`.

## 7. DOCX, confirmación y recuperación

### Reconocimiento

- Soportar párrafos del cuerpo y tablas, incluidas anidadas, encabezados y pies de todas sus variantes. Reconstruir el texto de cada párrafo conservando el mapa a fragmentos y offsets para diagnosticar apariciones divididas entre runs.
- No unir párrafos para aceptar un marcador. Detectar fragmentos de delimitadores y marcadores repartidos entre párrafos; bloquear con ubicación.
- Inspeccionar partes/regiones fuera del soporte, por ejemplo cuadros de texto, notas y comentarios. No ignorar silenciosamente marcadores que una extracción de texto de alto nivel no devuelve.
- Validar llaves, mayúsculas, clave de 1–50 y coincidencia exacta; repetidos vinculan al mismo campo. Distinguir desconocido, mal formado y región no soportada.
- DOCX sin marcadores es candidato estático, no aprobación automática: un marcador inválido o fuera de soporte impide tratarlo como estático.
- Controles de procesamiento del preliminar: carga máxima 1 MiB, ZIP expandido 20 MiB, 512 entradas, timeout 20 segundos y dos procesos simultáneos. Rechazar relaciones externas, traversal, macros y XML inseguro. Configurables en el servidor; no representan capacidad ni despliegue definitivo aceptados.
- Mantener reglas de reconocimiento accesibles al motor futuro dentro del servidor; no duplicar un parser de reglas distintas en JavaFX o Spec 002.

### Confirmación y concurrencia — implementadas para casos construidos

1. Analizar archivo y emitir una referencia/huella del análisis que incluya bytes, asociaciones semánticas y propuesta revisada; definir también qué cambios descriptivos invalidan la revisión.
2. Recibir confirmación identificada, clasificación de todos los campos y aceptación explícita de estático cuando proceda. Volver a analizar en servidor.
3. Si el contenido o asociaciones no coinciden con lo revisado, devolver análisis actualizado y aviso; no consumir la nueva revisión como confirmada automáticamente. La siguiente confirmación debe identificar inequívocamente lo revisado y su intención.
4. Registrar durablemente la operación pendiente y sus insumos recuperables; publicar el archivo en área controlada y verificar hash. Asignar el número al completar bajo bloqueo por plantilla: así el orden de numeración coincide con el de versiones completadas.
5. Completar metadatos, asociaciones y publicación del archivo; marcar completado solo cuando todo sea verificable. Mantener la última versión completada seleccionable durante cualquier fallo.
6. Clave repetida con misma solicitud: devolver operación/resultado original. Misma clave con otro contenido: rechazar. Dos intenciones distintas simultáneas: números diferentes y una regla de ordenación inequívoca para la última versión completada. Proponer serializar el registro por plantilla y probar completado fuera de orden; no asumir que el mayor ID es siempre el último éxito.
7. Si el proceso se interrumpe en cualquier frontera archivo/DB, la recuperación inspecciona lo conservado y reanuda sin duplicar. Si falta evidencia, marca fallido; definir propietario de recuperación (inicio, tarea o acción administrativa) y limpieza segura tras aprobación de retención.

## 8. Fases ejecutables y criterios de salida

### F0 — Cerrar decisiones y baseline

- Confirmar D-01–D-12 según dependencia; registrar cuáles impiden avanzar y cuáles pueden diferirse.
- Revisar rama/diff sin tocar trabajo ajeno; comprobar Java, Maven y Docker. Ejecutar baseline de pruebas y guardar resultado, no atribuir fallos previos al cambio.
- Preparar DOCX representativos anonimizados, con encabezados/pies/tablas y versión estática.
- **Aprobado E1:** construir juntos una nueva plantilla, sobre otros documentos por elegir; no continuar el acta demostrativa como formato de negocio. Los cuatro fixtures existentes y su generador en `specs/001/fixtures/` se conservan como evidencia técnica. El presupuesto que posee el responsable es entrada para extracción; aún no se ha identificado/analizado aquí ni equivale a una plantilla de salida. Contenido, campos y obligatoriedad quedan por acordar.
- **Salida:** alcance técnico aprobado por fase, dependencias autorizadas y fixtures disponibles. Sin fuentes ausentes inventadas.

### F1 — Permisos y contratos base

- Implementar mecanismo aprobado, autenticación del escritorio y autorización separada por operación; acordar alcance sobre endpoints previos y política CSRF/sesión según transporte elegido.
- Definir DTO, errores y rutas; pruebas reales de 401/403 y aislamiento de configuración/consulta.
- Se permite avanzar reglas documentales aisladas en paralelo temporal a esta dependencia, pero no habilitarlas sin ella.
- **Salida:** usuarios con ninguno, consulta, administración y ambos permisos; solicitudes directas probadas y sin fugas de datos. CA-17/19/20/42 verificables.

### F2 — Esquema, catálogo y resolución

- Crear migraciones y modelos propuestos; validar base limpia y upgrade V4 con negocio existente.
- Implementar lista aprobada, CRUD permitido, lectura de origen, clasificación y conversión común; no escribir en Obras/Clientes desde este módulo.
- Resolver D-09 mediante entrega específica si se acepta ampliar corrección de fechas; sin esa entrega, registrar CA-04 parcialmente pendiente.
- **Salida:** clave de 50 y marcador de 54 admitidos; claves inválidas/duplicadas/rutas libres rechazadas; semántica vinculada protegida; valores de dos obras correctos, fallos de origen distinguibles.

### F3 — Análisis DOCX y revisión

- D-04 e integración local aprobadas expresamente. Se prueban fixtures construidos sin adoptar el acta como formato de negocio ni sustituir la plantilla conjunta pendiente.
- Implementar adaptador, fixtures y diagnóstico estructurado por aparición; validar todas las regiones y entradas hostiles.
- Proponer obligatoriedad, clasificar nuevos y confirmar estático explícitamente.
- **Salida:** CA-06/07/11/12/26/36/37 demostrados, sin registrar versiones ante análisis inválido.

### F4 — Archivos y registro de versiones (prueba aislada)

- D-06 permite ahora el adaptador de archivos del servidor y la prueba construida. No se cierra el despliegue definitivo mediante pruebas temporales ni se decide retención; ver `storage-test.md`.
- Implementar almacenamiento, metadatos, estados, nueva confirmación ante cambio, idempotencia y recuperación. Serializar/coordinar registros concurrentes por plantilla.
- Implementar última revisión seleccionable, historial inmutable, inactivación y borrado lógico.
- **Salida:** pruebas de fallos antes/después de cada escritura, pérdida de respuesta, reenvío y dos cargas simultáneas; última válida conservada y archivos históricos consultables. CA-08/09/21/22/27/28/29/33/39.

### F5 — Paquetes y configuración aplicable

- Implementar alta, borradores persistentes, reapertura/descarte, confirmación, edición con precondición y unicidad de nombres aprobada.
- Implementar opciones por obra y su cliente, orden visual estable y resolución de selección actual; revalidar cambios de catálogo, plantilla, paquete o versión entre llamadas. No imponer etapa ni secuencia de generación.
- **Salida:** CA-13/15/16/18/24/30/31/32/34/38, con cambios de composición atómicos y borradores excluidos. CA-14/23 se verifican aquí solo en el contrato; su historial real depende del motor.

### F6 — Escritorio

- Agregar navegación, pantallas y diálogos pequeños. Cargas/análisis/guardados asíncronos, mensajes accionables y actualizaciones en hilo JavaFX.
- Permitir volver de registrar plantilla al editor de paquete sin perder captura; persistir borrador antes de cerrar según acción del usuario.
- Mostrar origen, texto, estado y obligatoriedad; históricos de solo lectura; revisión obligatoria si el análisis cambia. Evitar solicitudes duplicadas por doble clic sin sustituir idempotencia del servidor.
- **Salida:** flujos abiertos y comprobados en Windows, sin bloqueos de interfaz; cierre y reapertura recuperan borrador. Registrar evidencia visual aparte de compilación.

### F7 — Aceptación y contrato con Spec 002

- Ejecutar suite completa y recorrido manual de la sección 10; contrastar datos antes/después y errores de permisos.
- Documentar contrato de selección y texto, estáticos y procedencia histórica. La selección nueva revalida elegibilidad; una generación futura ya confirmada debe conservar su versión aunque deje de ser seleccionable.
- Actualizar README solo con capacidades existentes y marcar pendientes por criterio.
- **Salida:** todos los criterios propios de configuración verificados; dependencias de motor, fuentes, formatos/orígenes y permisos claramente resueltas o pendientes. No declarar aceptación completa con pendientes bloqueantes.

Orden aprobado en esta reanudación: verificar F0/F1/F2 hasta su alcance automatizable y continuar F3 → F4 → F5 → F6 con casos construidos. Las comprobaciones manuales pendientes permanecen abiertas y no se declaran realizadas para habilitar el avance técnico. **F6 es el límite: no implementar F7, extracción ni motor de Spec 002.**

## 9. Matriz de trazabilidad y pruebas

| Criterios | Requisito / fase | Evidencia mínima |
|---|---|---|
| CA-01/02/05/26/40 | CD-001, F2/F3 | Dominio + API + DB: atributo aprobado, clave exacta y única, 50/54, semántica inmutable y nombre editable. |
| CA-03/04/10 | CD-002/006, F2/F5 | Dos obras/clientes, fecha ausente y recarga tras corrección, obligatoriedad solo por versión; D-09 explícita. |
| CA-06/07/11/12 | CD-003, F3 | Fixtures cuerpo/tablas/encabezados/pies, runs divididos, regiones no soportadas, párrafos distintos y diagnósticos. |
| CA-08/09 | CD-004/007, F4 | Versión distinta, conservación previa, fallo de archivo y metadatos sin publicación parcial. |
| CA-13/15/16 | CD-005/006, F5 | Aplicabilidad por cliente, vacío/duplicado/inelegible rechazados y solo activa/última válida; generación sin etapa ni secuencia obligatoria. |
| CA-14/23 | CD-005/006, F5/F7 + Spec 002 | Paquete editable y contrato de procedencia aquí; historial de una generación real pendiente del motor. |
| CA-17/19/20/42 | CD-007, F1/F7 | Usuarios reales y llamadas API sin autenticar, sin permisos, con consulta, administración y ambos; cero cambios no autorizados. |
| CA-18/24 | CD-006, F5 | Una selección por documento; rechazo de versión antigua/ajena, plantilla inactiva/eliminada, paquete ajeno y cambios entre consulta/resolución. |
| CA-21/22 | CD-004/006, F4/F7 | Última completada para nuevas selecciones, eliminadas solo lectura. Vínculos de generaciones previas: integración futura con Spec 002. |
| CA-25/41 | CD-002/006, F2/F7 | Tipos antes de formato, espacios/null/cero/falso/inválido/error; igualdad de texto consulta/contrato. Importe real depende D-10. |
| CA-27/28/29/33 | CD-003/004/007, F4 | Mismo nombre con bytes distintos, catálogo cambiado, reinicio en fronteras, respuesta perdida, reenvío, claves conflictivas y concurrencia real PostgreSQL. |
| CA-30/31/32/34 | CD-005, F5 | Dos editores, revisión obsoleta, varios nombres exactos por cliente, cliente fijo, nombre/ID como orden visual, sin exigir secuencia de uso. |
| CA-35 | CD-002/005, F7 | Evidencia manual separada para registro de versión y edición de composición, contrastando obra/cliente antes y después. |
| CA-36/37/39 | CD-003/004, F3/F4/F6 | Clasificación completa/herencia revisable, estático explícito e históricos inmutables con detalle. |
| CA-38 | CD-005, F5/F6 | Borrador persistido, cierre/reapertura, completar/descartar y exclusión de generación. |

Capas de pruebas: unitarias para invariantes, resolución, formato y propuestas; MVC para validación/errores/permisos; Testcontainers PostgreSQL 18 para restricciones, migración, transacciones y concurrencia; HTTP real para reenvíos y selección; validación JavaFX en Windows. No reemplazar pruebas de concurrencia de DB por mocks.

## 10. Verificación, entrega y pendientes

### Comandos previstos desde la raíz, en WSL

```bash
./mvnw -pl brica-server test
./mvnw -pl brica-client test
./mvnw test
./mvnw clean verify
git diff --check
```

Docker activo para integración. Antes de Maven, preparar Python y exportar `BRICA_DOCUMENT_PYTHON` como indica [manual-test.md](manual-test.md): las regresiones Python forman parte de Maven y no se omiten si faltan dependencias. No configurar Testcontainers contra datos de producción. Maven no carga `.env`; solo el backend manual necesita sus `DB_*`. Registrar resultados sin declarar una comprobación no ejecutada.

### Recorrido manual de aceptación en Windows

1. Iniciar backend y cliente; usar usuarios autorizados con datos de prueba identificables. Capturar obra/cliente antes del recorrido.
2. Para el preliminar, crear campos de nombre de obra/cliente y consultar dos obras. Fechas/importes y CA-04/41 se verifican al aprobar su entrega, no durante este recorte.
3. Analizar DOCX con repetidos, tablas, encabezados/pies y runs divididos; revisar clasificación completa, confirmar versión 1 y comprobar conservación de negocio (**primera evidencia CA-35**).
4. Intentar desconocidos, mal formados y región no soportada; bloquear confirmación. Probar documento estático con confirmación explícita.
5. Registrar versión 2 con campos conservados/nuevos/retirados. Consultar ambas versiones y comprobar que solo la última completada se ofrece. Simular carga fallida y recuperación sin duplicado.
6. Guardar paquete incompleto como borrador, cerrar y reabrir; agregar plantilla desde el editor, volver sin perder captura y confirmar paquete válido.
7. Editar composición y nombre; comprobar conflicto entre dos sesiones, orden y conservación de obra/cliente (**segunda evidencia CA-35**, independiente).
8. Consultar paquetes de la obra según su cliente; excluir ajenos/borradores e inactivas. Comprobar nombres por comparación exacta dentro del cliente y ausencia de filtros por etapa o secuencia de generación. Resolver selección única, verificar campos exactos, valores como texto y rechazo si la configuración cambia.
9. Probar peticiones directas con y sin autenticación y permisos independientes. Confirmar que el rol de consulta no puede modificar configuración y que el administrador sin permiso de valores no obtiene valores.
10. Ejecutar con el motor real cuando exista: generación con versión 1, registro de versión 2 y edición del paquete; demostrar que la generación previa conserva paquete y solo versiones utilizadas. Hasta entonces, esos escenarios quedan pendientes de integración, no aprobados.

### Definición de terminado

Corresponde al objetivo completo, no al mínimo preliminar. El aplazamiento de D-06, fechas/importes y plantilla real mantiene pendientes sus evidencias; no equivale a aprobar el módulo ni a borrar criterios de aceptación.

- Comportamiento y restricciones aprobados implementados en servidor, consumidos por cliente sin duplicar reglas ni datos de negocio.
- Migraciones nuevas verificadas en vacío y actualización desde V4; datos previos, archivos y vínculos conservados.
- DOCX representativos analizados y versiones consultables; pendientes/fallidas no elegibles y recuperación/idempotencia/concurrencia verificadas.
- Borradores reanudables, edición sin pérdida de cambios y resolución aplicable correcta con formato común.
- Permisos reales configurados y probados; sin ello no hay aceptación completa ni habilitación del módulo.
- Evidencia automática y manual registrada, incluidos CA-35 y validación JavaFX en Windows; pendientes por criterio visibles, con responsable/dependencia a acordar.
- Sin secretos, dependencias no aprobadas, cambios de arquitectura encubiertos ni modificaciones de migraciones aplicadas. No commit, push o PR sin solicitud.

### Evidencia de esta tarea de planificación

- Revisados código, configuración, migraciones y reglas del repositorio; se detectó ERS vacío y ausencia de Diccionario. DAS excluido por instrucción expresa.
- Consultado Context7 para configuración de seguridad de Spring Boot (referencia disponible 4.1.0; proyecto 4.1.1) y alcance/limitaciones de Apache POI XWPF. No se eligió ni añadió una versión de POI; ratificar documentación exacta al implementar.
- Esta tarea cambia documentación solamente. No se ha implementado el módulo ni se han ejecutado pruebas Maven o validación JavaFX por redactar este plan.
- Verificados `git diff --check`, diff de la documentación existente, enlaces locales de los cuatro documentos afectados y presencia de los 42 criterios en la matriz de trazabilidad; comprobado también el archivo nuevo, no incluido todavía en el diff de Git. No se alteraron los cambios locales de `AGENTS.md` ni la corrección previa del título de la Spec 001.

La evidencia anterior es histórica de la redacción del plan, no el estado de ejecución. Las comprobaciones, decisiones y tareas actuales están en [review.md](review.md).
