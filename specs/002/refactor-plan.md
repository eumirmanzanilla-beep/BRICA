# Plan de refactor y flujo de creación de obra

**Funcionalidad:** reorganización funcional del cliente y flujo guiado de creación de obra  
**Fecha:** 2026-11-23  
**Estado:** R0–R7 ejecutadas; aceptación técnica y visual final pendiente.

**Spec relacionada:** [spec.md](spec.md) · [Plan de extracción](plan.md) · [Seguimiento](review.md)  
**Autorización y límites:** el responsable autorizó verificar R0, actualizar este plan y retirar los artefactos F0. Esta revisión no inicia cambios de aplicación, migraciones, acceso a datos reales, despliegues ni commits. El flujo de obra y la configuración documental por permisos se mantienen como alcance de las fases siguientes.

> Este documento complementa la Spec 002 por solicitud expresa. No redefine su alcance de extracción ni declara aceptados sus criterios. La creación de obra y el refactor de paquetes son un cambio distinto; la integración con Extracción se limita a conservar su acceso desde Obras y sus garantías ya implementadas.

## 1. Resultado, alcance y fuentes

### Resultado

El cliente JavaFX se organizará conforme a las rutas funcionales descritas en los README del cliente y servidor. El flujo de creación de obra será un asistente temporal, claro y reversible:

1. Datos iniciales: seleccionar cliente vigente, nombre y descripción.
2. Crear obra: la obra se registra mediante la API existente.
3. Adjuntos opcionales: contrato y presupuesto base.
4. Captura manual: atributos vigentes de la obra en un formulario ordenado.
5. Revisión: valores registrados en modo de solo lectura.
6. Guardar: actualizar la información de la obra con ETag.
7. Configuración documental posterior: paquete, plantilla y versión dentro del contexto de una obra existente, si el usuario tiene permisos documentales.

El progreso de la interfaz vive únicamente mientras el asistente está abierto. Cerrar el asistente, cerrar la aplicación o reiniciar el cliente descarta las selecciones y cambios aún no guardados en el servidor.

### Incluido

- Validar en servidor nombres de obra legibles: espacios, acentos, números y puntuación de escritura normal; obligatorios y de máximo 255 caracteres.
- Reutilizar la API y el almacenamiento existente para adjuntos `CONTRACT` y `BASE_BUDGET`, sin iniciar extracción automáticamente.
- Crear un asistente de JavaFX con pasos visibles, navegación hacia atrás y mensajes orientados a corrección.
- Conservar ETag, `If-Match`, sesión, CSRF, autorización y operaciones asíncronas existentes.
- Separar la acción documental de la creación de obra:
  - Crear o completar una obra: `CLIENTS_READ` y `WORKS_WRITE`.
  - Consultar/configurar documentos para una obra: `WORKS_READ` y `DOCUMENT_CONFIG_ADMIN`.
- Crear plantillas originadas en esta experiencia con `documentType = "Documental"` sin exponer ese campo al usuario.
- Precargar el nombre de versión desde el nombre del DOCX sin extensión y conservar la regla técnica actual de nombres para paquete, plantilla y versión.
- Reorganizar los módulos actuales del cliente en `core`, `auth`, `shell`, `clients`, `works`, `documents`, `extraction` y `shared`.
- Reubicar FXML, CSS y pruebas junto a su módulo funcional; actualizar todos los consumidores, rutas `FXMLLoader`, `fx:controller`, imports, pruebas y referencias de recursos en el mismo cambio.
- Revisar el comportamiento de Extracción sincronizado recientemente, incluido el worker Windows, sin alterar sus reglas funcionales al efectuar el movimiento de paquetes.

### Excluido

- Persistencia de borradores del asistente de obra, recuperación después de reiniciar y cambios de esquema/Flyway.
- Crear contratos o presupuestos de negocio desde los adjuntos.
- Extracción automática durante la creación de obra.
- Generación, renderizado, vista previa de salida o exportación documental.
- Cambios de permisos, roles, endpoints, formato de errores o contratos REST existentes.
- Integrar los experimentos F0 en la aplicación o restaurar su directorio sin una tarea y validación específicas.
- Consultar o modificar datos reales, bases de datos locales existentes, volúmenes o archivos documentales reales.

### Fuentes verificadas

- `AGENTS.md`, `CONTRIBUTING.md`, `brica-client/README.md`, `brica-server/README.md`.
- Código actual de cliente, servidor, FXML, CSS, pruebas y migraciones V1–V18.
- [Spec 001](../001/spec.md), [Spec 002](spec.md), [plan.md](plan.md), [review.md](review.md).
- ERS, DAS y Diccionario de Datos como intención de negocio, sin asumir que todo lo descrito esté implementado.
- El banco F0 retirado, que incluía `F0Probe.java`, `expected.json`, `evaluate.py`, `audit_dependencies.py` y ejemplos de política.

## 2. Estado comprobado y discrepancias

| Área o ruta | Situación comprobada | Impacto |
|---|---|---|
| Rama | Línea base de R0: `fix/spec-002-source-extraction`, limpia antes de la edición y dos commits por delante de su remoto local. `2a800d8` añadió este plan sobre `c979921`. | Los consumidores de Extracción entran al refactor; la sincronización del plan no cambió código de aplicación. |
| Cliente actual | API y controladores se concentran en `com.brica.client.api` y `com.brica.client.presentation`. | Se requiere mover clases, pruebas y recursos de forma coordinada. |
| README del cliente | Define paquetes funcionales y recursos separados; aclara que el árbol es dirección arquitectónica y no inventario. | Se adopta para las clases existentes afectadas, sin crear paquetes vacíos ni módulos Maven nuevos. |
| Creación de obra actual | `WorksController` crea la obra con diálogo breve; `WorkCaptureDialog` ya encadena adjuntos, captura y revisión. | Se transforma en asistente explícito; no se duplican endpoints ni almacenamiento. |
| Obras en servidor | `Work.validate` usa `InputRules.validName`, regla técnica sin espacios ni acentos. La columna ya es `VARCHAR(255)`. | Solo cambia la validación de dominio y pruebas; no se necesita migración. |
| Documentos | Paquetes por obra/cliente, plantillas, versiones, análisis DOCX y confirmación ya existen. `documentType` es obligatorio en API. | La UI enviará `Documental` de forma interna; el servidor no cambia. |
| Permisos | `WORKS_WRITE`, `CLIENTS_READ`, `WORKS_READ` y `DOCUMENT_CONFIG_ADMIN` están definidos y aplicados por servicios existentes. | Se adapta la experiencia del cliente; el servidor conserva la autorización efectiva. |
| Extracción sincronizada | Incluye reintentos, historial, UI, pruebas de API y aislamiento Windows mediante `WindowsSourceLaunch` y `windows-launch.ps1`. | Se deben mover sus API/UI, actualizar imports y el recurso CSS sin alterar el flujo ni debilitar el aislamiento. |
| Experimentos F0 | R0 verificó y después retiró por autorización el banco sintético, incluidos el generador, las expectativas, la auditoría y los ejemplos de política. | Sus resultados de 2026-11-17 y R0 permanecen en `review.md` como evidencia histórica, no ejecutable ni equivalente a aceptación de la aplicación. |

## 3. Decisiones confirmadas y pendiente

| ID | Decisión | Estado |
|---|---|---|
| D-01 | El progreso del asistente de obra se conserva únicamente mientras el asistente y la aplicación siguen abiertos. | Confirmada. |
| D-02 | Contrato y presupuesto son adjuntos opcionales; se prioriza captura manual y no se inicia extracción automática. | Confirmada. |
| D-03 | El valor de tipo documental para plantillas creadas desde la interfaz será `Documental`, enviado sin solicitarlo ni mostrarlo al usuario. | Confirmada. |
| D-04 | Acciones de obra y administración documental requieren permisos separados. | Confirmada. |
| D-05 | `CLIENTS_READ` + `WORKS_WRITE` permite crear/completar obra. `WORKS_READ` + `DOCUMENT_CONFIG_ADMIN` permite configurar documentos de una obra existente. | Confirmada como experiencia; la autorización de servidor no cambia. |
| D-06 | El nombre de obra es texto de presentación y no usa la regla técnica de nombres documentales. Paquete, plantilla y versión conservan la regla técnica existente. | Confirmada. |
| D-07 | Los experimentos F0 no se incorporan como código de producción. | Confirmada. |
| D-08 | Retirar por completo los artefactos F0 y conservar únicamente la evidencia histórica documental. | Resuelta en R0. |
| D-09 | Tras el refactor, mover `ServerSession` y `ServerHealthClient` desde `api` a `core/http`. | Propuesta técnica: ahora no hay cambios locales que lo bloqueen; debe verificarse que todos los clientes, incluido Extracción, conservan una única sesión compartida. |

## 4. Diseño y cambios previstos

### 4.1 Flujo y contratos

```text
JavaFX WorksController
    -> WorkCreationWizard (estado local WorkCreationDraft)
        -> WorksApiClient.create
        -> WorksApiClient.upload (0..2 adjuntos)
        -> WorksApiClient.updateInformation (If-Match)
    -> ContextDocumentsWindow (acción separada, obra ya guardada)
        -> DocumentsApiClient.savePackage
        -> DocumentsApiClient.createTemplate(..., "Documental")
        -> DocumentsApiClient.analyze / confirm

JavaFX ExtractionWindow
    -> ExtractionApiClient
    -> servidor de extracción existente, sin modificación semántica
```

- No se crean endpoints ni DTO HTTP nuevos.
- `WorkCreationDraft` no contiene datos persistentes, credenciales ni bytes de archivos; conserva valores de interfaz y rutas seleccionadas mientras la instancia exista.
- Las cargas de archivos y llamadas de red se mantienen fuera del hilo JavaFX. Las actualizaciones de controles regresan al hilo JavaFX.
- El servidor conserva la validación, permisos, límites de adjuntos, hash, ETag, control de concurrencia y `ProblemDetail`.
- La revisión del asistente muestra texto y valores formateados, no controles editables deshabilitados.
- Los marcadores DOCX desconocidos no generan campos de referencia. La interfaz conserva y muestra el diagnóstico accionable devuelto por el servidor.

### 4.2 Nombre de obra

`Work` tendrá una validación específica de nombre:

- Requerido; `null`, vacío y solo espacios se rechazan.
- Máximo 255 caracteres.
- Permite Unicode de presentación, incluidas letras acentuadas, espacios y números.
- No normaliza ni impone la expresión regular de `InputRules.validName`.

La regla `InputRules.validName` permanece sin cambios para nombre de paquete, plantilla y versión.

### 4.3 Paquetes, plantillas y versiones

- En el contexto de una obra no se podrá editar el cliente ni la obra asociados: el servidor resuelve el cliente vigente de la obra.
- La interfaz mostrará la progresión Paquete → Plantilla → Versión y sus requisitos.
- `ContextDocumentsWindow` y `TemplatesController` ocultarán cualquier captura o edición visible de tipo documental y enviarán `Documental`.
- En una plantilla existente que tuviera otro tipo, la interfaz no lo sustituirá al editar otros datos sin una operación explícita autorizada. El valor fijo aplica a nuevas plantillas creadas por este flujo.
- El nombre sugerido de versión se obtiene del archivo DOCX sin extensión; el usuario puede modificarlo antes de confirmar.
- La clasificación obligatoria/opcional se realiza por versión y campo detectado, tal como exige el servidor actual.

### 4.4 Reorganización física del cliente

Las rutas siguientes reflejan movimientos reales previstos; los archivos de origen dejarán de existir una vez actualizado cada consumidor.

#### Infraestructura, autenticación y utilidades

| Origen | Destino | Acción |
|---|---|---|
| `client/api/ServerSession.java` | `client/core/http/ServerSession.java` | Mover |
| `client/api/ServerHealthClient.java` | `client/core/http/ServerHealthClient.java` | Mover |
| `client/api/LoginException.java` | `client/auth/api/LoginException.java` | Mover |
| `client/presentation/LoginController.java` | `client/auth/presentation/LoginController.java` | Mover |
| `client/presentation/AsyncDialogSave.java` | `client/core/ui/AsyncDialogSave.java` | Mover |
| `client/presentation/UiPagination.java` | `client/core/ui/UiPagination.java` | Mover |
| `client/presentation/NameInputRules.java` | `client/shared/presentation/NameInputRules.java` | Mover |
| `client/presentation/MainController.java` | `client/shell/presentation/MainController.java` | Mover |
| `client/presentation/DashboardController.java` | `client/shell/presentation/DashboardController.java` | Mover |

#### Clientes

| Origen | Destino | Acción |
|---|---|---|
| `client/api/ClientsApiClient.java` | `client/clients/api/ClientsApiClient.java` | Mover |
| `client/api/ClientDto.java` | `client/clients/api/ClientDto.java` | Mover |
| `client/presentation/ClientsController.java` | `client/clients/presentation/ClientsController.java` | Mover |

#### Obras

| Origen | Destino | Acción |
|---|---|---|
| `client/api/WorksApiClient.java` | `client/works/api/WorksApiClient.java` | Mover; conservar contrato HTTP y añadir solo coordinación necesaria |
| `client/api/WorkDto.java` | `client/works/api/WorkDto.java` | Mover |
| `client/presentation/WorksController.java` | `client/works/presentation/WorksController.java` | Mover y adaptar acciones/permiso |
| `client/presentation/WorkCaptureDialog.java` | `client/works/presentation/WorkCreationWizard.java` | Renombrar y transformar |
| `client/presentation/WorkInformationForm.java` | `client/works/presentation/WorkInformationForm.java` | Mover y ajustar distribución |
| `client/presentation/WorkDocumentInformationDialog.java` | `client/works/presentation/WorkDocumentInformationDialog.java` | Mover |
| `client/presentation/WorkAttachmentActions.java` | `client/works/presentation/WorkAttachmentActions.java` | Mover |
| `client/presentation/WorkDatePicker.java` | `client/works/presentation/WorkDatePicker.java` | Mover |
| `client/presentation/WorkActionIcon.java` | `client/works/presentation/WorkActionIcon.java` | Mover |
| — | `client/works/presentation/WorkCreationDraft.java` | Crear |

#### Documentos

| Origen | Destino | Acción |
|---|---|---|
| `client/api/DocumentsApiClient.java` | `client/documents/api/DocumentsApiClient.java` | Mover |
| `client/api/DocumentDtos.java` | `client/documents/api/DocumentDtos.java` | Mover |
| `client/presentation/DocumentConfigurationController.java` | `client/documents/presentation/DocumentConfigurationController.java` | Mover |
| `client/presentation/DocumentPackagesController.java` | `client/documents/presentation/DocumentPackagesController.java` | Mover y añadir contexto fijo de obra |
| `client/presentation/TemplatesController.java` | `client/documents/presentation/TemplatesController.java` | Mover y ocultar tipo |
| `client/presentation/TemplateVersionDialogController.java` | `client/documents/presentation/TemplateVersionDialogController.java` | Mover y mejorar instrucciones |
| `client/presentation/ReferenceFieldsController.java` | `client/documents/presentation/ReferenceFieldsController.java` | Mover |
| `client/presentation/ReferenceValuesController.java` | `client/documents/presentation/ReferenceValuesController.java` | Mover |
| `client/presentation/ContextDocumentsWindow.java` | `client/documents/presentation/ContextDocumentsWindow.java` | Mover y adaptar el flujo contextual |
| `client/presentation/DocumentInput.java` | `client/documents/presentation/DocumentInput.java` | Mover |
| `client/presentation/DocumentUi.java` | `client/documents/presentation/DocumentUi.java` | Mover |

#### Extracción y presupuesto dentro de revisión

| Origen | Destino | Acción |
|---|---|---|
| `client/api/ExtractionApiClient.java` | `client/extraction/api/ExtractionApiClient.java` | Mover; actualizar import de sesión compartida |
| `client/api/ExtractionDtos.java` | `client/extraction/api/ExtractionDtos.java` | Mover |
| `client/presentation/extraction/ExtractionWindow.java` | `client/extraction/presentation/ExtractionWindow.java` | Mover sin alterar sus comandos de revisión |
| `client/presentation/extraction/TableMappingDialog.java` | `client/extraction/presentation/TableMappingDialog.java` | Mover |
| `client/presentation/extraction/BudgetEditor.java` | `client/extraction/presentation/BudgetEditor.java` | Mover: sigue siendo parte de la revisión de extracción, no un módulo de presupuestos independiente |

#### Recursos

| Origen | Destino |
|---|---|
| `resources/.../presentation/main-view.fxml` | `resources/.../shell/main-view.fxml` |
| `resources/.../presentation/dashboard-view.fxml` | `resources/.../shell/dashboard-view.fxml` |
| `resources/.../presentation/login-view.fxml` | `resources/.../auth/login-view.fxml` |
| `resources/.../presentation/clients-view.fxml` | `resources/.../clients/clients-view.fxml` |
| `resources/.../presentation/works-view.fxml` | `resources/.../works/works-view.fxml` |
| `resources/.../presentation/document-configuration-view.fxml` | `resources/.../documents/document-configuration-view.fxml` |
| `resources/.../presentation/document-packages-view.fxml` | `resources/.../documents/document-packages-view.fxml` |
| `resources/.../presentation/templates-view.fxml` | `resources/.../documents/templates-view.fxml` |
| `resources/.../presentation/template-version-dialog.fxml` | `resources/.../documents/template-version-dialog.fxml` |
| `resources/.../presentation/reference-fields-view.fxml` | `resources/.../documents/reference-fields-view.fxml` |
| `resources/.../presentation/reference-values-view.fxml` | `resources/.../documents/reference-values-view.fxml` |
| `resources/.../presentation/styles/app.css` | `resources/.../styles/app.css` |

Se actualizarán las rutas de `BricaClientApplication`, `MainController`, cualquier `FXMLLoader`, `fx:controller`, includes y carga de CSS. No se mantendrán duplicados en `resources/.../presentation/`.

#### Pruebas

| Origen | Destino |
|---|---|
| `test/.../api/ServerSessionTest.java` | `test/.../core/http/ServerSessionTest.java` |
| `test/.../api/LoginExceptionTest.java` | `test/.../auth/api/LoginExceptionTest.java` |
| `test/.../api/DocumentsApiClientTest.java` | `test/.../documents/api/DocumentsApiClientTest.java` |
| `test/.../api/ExtractionApiClientTest.java` | `test/.../extraction/api/ExtractionApiClientTest.java` |
| `test/.../presentation/DocumentInputTest.java` | `test/.../documents/presentation/DocumentInputTest.java` |
| `test/.../presentation/DocumentFxSmoke.java` | `test/.../documents/presentation/DocumentFxSmoke.java` |
| `test/.../presentation/DocumentFxmlContractTest.java` | `test/.../documents/presentation/DocumentFxmlContractTest.java` |
| `test/.../presentation/ReferenceFieldDeleteFxSmoke.java` | `test/.../documents/presentation/ReferenceFieldDeleteFxSmoke.java` |
| `test/.../presentation/NameInputRulesTest.java` | `test/.../shared/presentation/NameInputRulesTest.java` |
| `test/.../presentation/NavigationFxSmoke.java` | `test/.../shell/presentation/NavigationFxSmoke.java` |
| — | `test/.../works/presentation/WorkCreationDraftTest.java` |
| — | `test/.../works/presentation/WorkCreationWizardTest.java`, si la infraestructura JavaFX actual permite comprobar el flujo sin introducir un harness persistente nuevo |

### 4.5 Servidor y Experimentos F0

| Archivo | Cambio previsto |
|---|---|
| `brica-server/src/main/java/com/brica/works/domain/Work.java` | Reemplazar la regla técnica de nombre por validación de texto de presentación. |
| `brica-server/src/test/java/com/brica/works/domain/WorkValidationTest.java` | Cubrir nombres con espacio, acentos y números; entradas vacías y longitud máxima. |
| `brica-server/src/test/java/com/brica/works/api/WorkControllerTest.java` | Ajustar solo si la cobertura HTTP actual no representa las nuevas entradas válidas o inválidas. |
| `specs/002/experiments/` | Retirado por autorización en R0; no se integra en producción ni se conserva como verificación ejecutable. |
| `specs/002/review.md` y `specs/002/plan.md` | Documentación alineada para distinguir evidencia histórica de verificaciones disponibles. |

## 5. Iteraciones atómicas y cobertura del alcance

Cada fase se revisará, compilará y verificará antes de iniciar la siguiente. La cobertura indicada es de requisitos y rutas del plan; el proyecto no declara actualmente una meta automatizada de porcentaje de líneas.

| Fase | Entrega atómica | Archivos y áreas | Evidencia de salida | Dependencia |
|---|---|---|---|---|
| R0 | Línea base y Experimentos F0 (ejecutada) | Estado Git, banco F0, `WindowsSourceLaunch`, pruebas de Extracción | F0 retirado por autorización y resultados registrados en §6 como evidencia histórica | Autorizada |
| R1 | Nombre de obra legible (ejecutada) | `Work.java`, pruebas de validación/API de Obras | Espacios, acentos, números y puntuación aceptados; vacío y >255 rechazados; sin migración | R0 |
| R2 | Núcleo, autenticación y API por módulo (ejecutada) | Movimientos de `core/http`, `core/ui`, `auth`, `clients`, `works`, `documents`, `extraction`; imports de todos los consumidores | Compilación y pruebas API; una única `ServerSession.shared` usada por todos los clientes | R1 |
| R3 | Recursos y controladores funcionales (ejecutada) | Movimientos de shell, clientes, obras, documentos, extracción, FXML y CSS | Contrato FXML y pruebas del cliente aprobados; no quedan referencias a los recursos migrados bajo `presentation/` | R2 |
| R4 | Asistente de nueva obra (ejecutada) | `WorkCreationDraft`, `WorkCreationWizard`, `WorksController`, formulario y CSS | Datos iniciales, adjuntos opcionales, captura, revisión, volver y guardado con ETag | R3 |
| R5 | Configuración documental contextual (ejecutada) | `ContextDocumentsWindow`, controladores documentales, `DocumentsApiClient`, FXML de documentos | Paquete ligado a obra, tipo interno `Documental`, versión desde DOCX y clasificación de campos | R4 |
| R6 | Experiencia por permisos (ejecutada) | Puntos de entrada de Obras y Documentos, pruebas de interfaz posibles | Usuario de obra no abre documentos; usuario documental opera sobre obra existente; usuario integral continúa el flujo | R5 |
| R7 | Regresión y aceptación técnica (ejecutada; aceptación pendiente) | Cliente, servidor, F0 que permanezca aplicable, revisión visual Windows | Suites aplicables, `git diff --check`, búsqueda de rutas/imports obsoletos y recorrido manual | R6 |

### Cobertura funcional por fase

| Requisito | Fases que lo cubren |
|---|---|
| Estructura funcional definida por README | R2–R3 |
| Nombre de obra libre y validado | R1 |
| Datos iniciales y cliente existente | R4 |
| Adjuntos opcionales sin extracción automática | R4 |
| Captura manual completa y organizada | R4 |
| Revisión no editable y retorno sin pérdida en sesión | R4 |
| Paquete, plantilla y versión por obra | R5 |
| Tipo documental oculto `Documental` | R5 |
| Nombre de versión desde DOCX y clasificación de marcadores | R5 |
| Separación de permisos de Obra/Documentos | R6 |
| Compatibilidad de Extracción y aislamiento Windows sincronizado | R0, R2, R3, R7 |
| No generación/exportación ni borradores persistentes nuevos | R4–R7 |

## 6. Verificación

| Comprobación | Comando o recorrido | Entorno | Criterio |
|---|---|---|---|
| Validación de Obras | `./mvnw.cmd -pl brica-server -Dtest=WorkValidationTest,WorkControllerTest test` | PowerShell, sin consultar DB real | Pruebas focalizadas pasan. |
| Cliente por módulo | `./mvnw.cmd -pl brica-client test` | PowerShell | Compila, carga FXML y pasan pruebas no afectadas. |
| Servidor completo | `./mvnw.cmd -pl brica-server test` | PowerShell; Docker si Testcontainers lo requiere | Regresiones de servidor pasan. |
| Reactor | `./mvnw.cmd test` y, si el entorno está preparado, `./mvnw.cmd verify` | Raíz; Docker/Python según POM | Integración aplicable pasa; informar limitaciones reales. |
| Experimentos F0 retirados | No aplica: no hay pruebas F0 ejecutables en el árbol. | — | Los resultados previos son históricos y no aceptan la aplicación. |
| Integridad del refactor | Búsqueda de imports `com.brica.client.api.*`, recursos `presentation/` migrados y `git diff --check` | Raíz | Solo permanecen rutas antiguas justificadas; sin errores de espacios ni rutas muertas. |
| Recorrido JavaFX | Iniciar cliente en Windows con cuentas de prueba autorizadas | Windows | Validar pasos, botones, navegación, FXML, carga asíncrona, permisos y mensajes. |
| Documentación DOCX | Cargar un DOCX sintético con marcadores conocidos y uno con marcador no registrado | Windows; no usar archivos reales | Nombre sugerido correcto, clasificación completa, diagnóstico accionable y ninguna creación automática de campos. |

### Evidencia de R0 — 2026-11-23

- Cliente: `.\mvnw.cmd -pl brica-client '-Dtest=ExtractionApiClientTest' test`: 1 prueba aprobada.
- Servidor: `.\mvnw.cmd -pl brica-server '-Dtest=ExtractionJobTest,ExtractionRequestLimitFilterTest,ExtractionPermissionsTest,ExtractionExecutionServiceTest,SourceWorkerTest,IsolatedSourceProcessorTest' test`: 16 aprobadas; omitidas la exclusiva de Linux y la del worker Windows empaquetado, que requiere un JAR ejecutable y `brica.test.windows.packaged=true`. La prueba Windows de rechazo del classpath sin empaquetar sí pasó.
- Worker empaquetado: `.\mvnw.cmd -pl brica-server '-DskipTests' package` generó el JAR ejecutable. `.\mvnw.cmd -pl brica-server '-Dtest=IsolatedSourceProcessorTest#packagedWindowsWorkerReadsContract' '-Dbrica.test.windows.packaged=true' test`: 1 prueba aprobada; el worker leyó un contrato DOCX sintético en Windows. No sustituye la prueba de integración ni el recorrido JavaFX.
- Política F0: antes de retirar el banco, `wsl.exe -d Ubuntu -- python3 -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v` aprobó 3 ejemplos.
- Banco host completo y auditoría OSV no repetidos: la preparación documentada (classpath de laboratorio y raíz OCR en `/tmp/opencode`) no está presente en esta distribución. Tampoco se ejecutaron las pruebas de integración con PostgreSQL. No se consultó ninguna base ni documento real.

### Evidencia de R1 — 2026-11-23

- `Work` valida el nombre como texto obligatorio de hasta 255 caracteres mediante `InputRules.requiredText`; la regla técnica `InputRules.validName` sigue intacta para paquete, plantilla y versión.
- `.\mvnw.cmd -pl brica-server '-Dtest=WorkValidationTest,WorkControllerTest' test`: 8 pruebas aprobadas. El dominio rechaza `null`, vacío/solo espacios y más de 255 caracteres; HTTP acepta un nombre legible y rechaza uno de más de 255.
- No se añadió migración: `works.name` ya es `VARCHAR(255)`.

### Evidencia de R2 — 2026-11-23

- Se movieron las API, DTO y utilidades a `core`, `auth`, `clients`, `works`, `documents`, `extraction` y `shared`; no quedan referencias Java a `com.brica.client.api`.
- `AsyncDialogSave` y `UiPagination` son utilidades públicas de `core/ui`. `AsyncDialogSave` conserva la obtención de la causa de un error sin depender de `DocumentUi`, que se mueve con Documentos en R3.
- `ClientsApiClient`, `WorksApiClient`, `DocumentsApiClient` y `ExtractionApiClient` obtienen la misma instancia por `ServerSession.shared(base)`. Las sobrecargas que reciben una sesión se mantienen para pruebas.
- `.\mvnw.cmd -pl brica-client test`: 23 pruebas aprobadas, incluidas sesión, autenticación, API de documentos y extracción, contrato FXML y reglas de nombre. Las smoke JavaFX no se ejecutan automáticamente en esta suite.

### Evidencia de R3 — 2026-11-23

- Se movieron controladores, diálogos y utilidades de presentación a `shell`, `auth`, `clients`, `works`, `documents` y `extraction`; los recursos FXML se ubicaron con cada módulo y la hoja CSS quedó en `resources/com/brica/client/styles/app.css`.
- Se actualizaron `BricaClientApplication`, `MainController`, los controladores FXML, los cargadores relativos de diálogos y las pruebas para las nuevas rutas. La prueba de contrato resuelve controladores, handlers e inclusiones a partir de la URL del FXML, no de su paquete de prueba.
- `.\mvnw.cmd -pl brica-client test`: 23 pruebas aprobadas. La búsqueda no encontró rutas, paquetes ni recursos migrados bajo `com.brica.client.presentation`.
- Pendiente: recorrido manual en Windows de las pantallas movidas y de las smoke JavaFX optativas; la compilación y el contrato FXML no sustituyen esa comprobación.

### Evidencia de R4 — 2026-11-23

- `WorkCreationWizard` mantiene el progreso solo dentro de su instancia: crea la obra con la API existente, permite elegir contrato y presupuesto base opcionales, carga únicamente los archivos elegidos y no inicia Extracción.
- El asistente conserva la ruta de archivo y los valores locales en `WorkCreationDraft`, no bytes ni credenciales. Volver conserva la captura; cerrar descarta selecciones que aún no se enviaron al servidor.
- La revisión renderiza texto no editable; el guardado reutiliza `WorksApiClient.updateInformation` con el ETag recibido después de crear o consultar la obra. Los nombres de obra se validan como texto de presentación, no con la regla técnica documental.
- `.\mvnw.cmd -pl brica-client test`: 24 pruebas aprobadas, incluida `WorkCreationDraftTest`. No se añadió una prueba automatizada del asistente JavaFX porque la infraestructura actual solo ofrece smoke gráfico optativo.

### Evidencia de R5 — 2026-11-23

- La acción documental desde una obra usa `ContextDocumentsWindow.showForWork`: el cliente no proporciona el identificador de cliente al consultar o crear paquetes de ese contexto; el servidor recibe el `workId` y resuelve el cliente vigente.
- Las creaciones de plantilla de la interfaz usan `DocumentsApiClient.DOCUMENTAL_TYPE` internamente. La captura y edición del tipo ya no son visibles; al editar una plantilla histórica se conserva su tipo previamente registrado.
- Al seleccionar un DOCX para una versión, el diálogo precarga su nombre base sin la extensión. La revisión sigue exigiendo clasificar cada campo detectado y conserva el diagnóstico de marcadores no registrados; no crea campos ni genera, previsualiza o exporta documentos.
- `.\mvnw.cmd -pl brica-client test`: 26 pruebas aprobadas, incluidas la verificación del tipo interno y el nombre sugerido de versión. Sigue pendiente el recorrido visual en Windows con DOCX sintéticos.

### Evidencia de R6 — 2026-11-23

- `DocumentContextAccess` expresa la condición de interfaz para configurar documentos en una obra existente: `WORKS_READ` y `DOCUMENT_CONFIG_ADMIN`. La autorización efectiva de la API permanece en el servidor.
- La entrada documental desde Clientes se retiró; `ContextDocumentsWindow` ya solo admite un contexto de obra fijo. Obras deshabilita su acción documental y explica el permiso faltante cuando no se cumplen ambos permisos.
- La navegación de Catálogo de campos exige la misma combinación y su handler evita abrirla si la acción se invoca fuera de un botón habilitado. Así, un usuario de Obras no obtiene una entrada documental y un usuario documental con lectura de Obras trabaja desde la obra existente.
- `.\mvnw.cmd -pl brica-client test`: 27 pruebas aprobadas, incluida la matriz de permisos de `DocumentContextAccessTest`. Sigue pendiente la comprobación manual de cuentas de prueba en Windows.

### Evidencia de R7 — 2026-11-23

- `.\mvnw.cmd -pl brica-client verify`: **27 pruebas aprobadas** y JAR del cliente generado. Incluye contrato FXML, sesión/CSRF, API documental, nombres, estado temporal del asistente y matriz de acceso documental.
- `git diff --check` pasó. Las búsquedas no encontraron paquetes o recursos migrados bajo `com.brica.client.presentation`, `com.brica.client.api`, `presentation/`, `WorkCaptureDialog` ni el acceso documental desde Clientes.
- `.\mvnw.cmd test`: el reactor no pasó: servidor ejecutó 227 pruebas, con 3 fallos y 20 errores; el módulo cliente se omitió al abortar el reactor. La causa dominante fue la ausencia del ejecutable Python configurado para el analizador DOCX (`DOCX_ANALYZER_UNAVAILABLE`), que produjo errores documentales en cascada. También falló `ServerDocumentFileStoreTest` con `AccessDenied` sobre un directorio temporal de pruebas.
- La tercera falla del reactor era una expectativa obsoleta: `DocumentConfigurationIntegrationTest` esperaba que Obras rechazara nombres técnicos. Se alineó con R1 para que Obras acepte texto legible y se verificó de forma aislada con Testcontainers: `.\mvnw.cmd -pl brica-server '-Dtest=DocumentConfigurationIntegrationTest#shouldKeepTechnicalNamesForDocumentsAcceptReadableWorkNamesAndRejectReversedDatesThroughHttp' test` aprobó 1 prueba.
- No se repitió el reactor completo después de esa corrección: seguirá bloqueado hasta preparar el entorno Python documental y resolver el acceso temporal. No se ejecutaron F0 (retirados en R0), recorrido JavaFX gráfico, cuentas de prueba autorizadas ni DOCX sintéticos; siguen siendo comprobaciones manuales pendientes.

## 7. Riesgos, bloqueos y próximos pasos

- **Refactor transversal:** mover API compartida y FXML puede romper imports, módulos, controladores o rutas. Mitigación: mover por fase, actualizar consumidores en el mismo cambio y ejecutar pruebas FXML/JavaFX.
- **Sesión compartida:** mover `ServerSession` debe preservar exactamente una sesión HTTP, cookies y CSRF. Mitigación: adaptar todos los clientes y ejecutar `ServerSessionTest` tras el movimiento.
- **Cambios de interfaz:** la compilación no valida experiencia de JavaFX. Mitigación: recorrido manual obligatorio en Windows.
- **Concurrencia:** el asistente debe conservar ETag y mostrar conflicto recuperable; no debe sobrescribir información. Mitigación: reutilizar `WorksApiClient.updateInformation` y su error existente.
- **Permisos:** ocultar acciones no autoriza. Mitigación: conservar las anotaciones y controles de servidor; probar usuario sin permisos.
- **Experimentos F0:** el banco fue retirado. No usar sus resultados históricos como cobertura actual de la implementación Windows ni del despliegue.
- **Datos y almacenamiento:** no consultar DB real ni modificar volúmenes. Los adjuntos de pruebas se crearán únicamente mediante la UI/API sobre entorno de prueba autorizado.
- **Cierre pendiente:** preparar el entorno Python documental según `README.md`, investigar el `AccessDenied` de almacenamiento temporal y volver a ejecutar el reactor. Después, recorrer JavaFX en Windows con cuentas y DOCX sintéticos autorizados; ninguna de estas tareas queda aceptada por las pruebas actuales.
