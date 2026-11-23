# Plan de refactor y flujo de creación de obra

**Funcionalidad:** reorganización funcional del cliente y flujo guiado de creación de obra  
**Fecha:** 2026-11-23  
**Estado:** Propuesto; pendiente de aprobación para implementar  
**Spec relacionada:** [spec.md](spec.md) · [Plan de extracción](plan.md) · [Seguimiento](review.md)  
**Autorización y límites:** el responsable autorizó redactar este plan y solicitó un flujo de obra con configuración documental separada por permisos. No autoriza todavía cambios de código, migraciones, datos reales, despliegues, commits ni eliminación de `specs/002/experiments/`.

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
- Borrar todo `specs/002/experiments/` o integrar sus programas en la aplicación sin una decisión y una validación específicas.
- Consultar o modificar datos reales, bases de datos locales existentes, volúmenes o archivos documentales reales.

### Fuentes verificadas

- `AGENTS.md`, `CONTRIBUTING.md`, `brica-client/README.md`, `brica-server/README.md`.
- Código actual de cliente, servidor, FXML, CSS, pruebas y migraciones V1–V18.
- [Spec 001](../001/spec.md), [Spec 002](spec.md), [plan.md](plan.md), [review.md](review.md).
- ERS, DAS y Diccionario de Datos como intención de negocio, sin asumir que todo lo descrito esté implementado.
- `specs/002/experiments/`, incluidos `F0Probe.java`, `expected.json`, `evaluate.py`, `audit_dependencies.py` y los ejemplos de política.

## 2. Estado comprobado y discrepancias

| Área o ruta | Situación comprobada | Impacto |
|---|---|---|
| Rama | `fix/spec-002-source-extraction`, limpia y un commit por delante de su remoto: `c979921`. | Los cambios de Extracción ya no son cambios locales que deban aislarse; sus consumidores entran al refactor. |
| Cliente actual | API y controladores se concentran en `com.brica.client.api` y `com.brica.client.presentation`. | Se requiere mover clases, pruebas y recursos de forma coordinada. |
| README del cliente | Define paquetes funcionales y recursos separados; aclara que el árbol es dirección arquitectónica y no inventario. | Se adopta para las clases existentes afectadas, sin crear paquetes vacíos ni módulos Maven nuevos. |
| Creación de obra actual | `WorksController` crea la obra con diálogo breve; `WorkCaptureDialog` ya encadena adjuntos, captura y revisión. | Se transforma en asistente explícito; no se duplican endpoints ni almacenamiento. |
| Obras en servidor | `Work.validate` usa `InputRules.validName`, regla técnica sin espacios ni acentos. La columna ya es `VARCHAR(255)`. | Solo cambia la validación de dominio y pruebas; no se necesita migración. |
| Documentos | Paquetes por obra/cliente, plantillas, versiones, análisis DOCX y confirmación ya existen. `documentType` es obligatorio en API. | La UI enviará `Documental` de forma interna; el servidor no cambia. |
| Permisos | `WORKS_WRITE`, `CLIENTS_READ`, `WORKS_READ` y `DOCUMENT_CONFIG_ADMIN` están definidos y aplicados por servicios existentes. | Se adapta la experiencia del cliente; el servidor conserva la autorización efectiva. |
| Extracción sincronizada | Incluye reintentos, historial, UI, pruebas de API y aislamiento Windows mediante `WindowsSourceLaunch` y `windows-launch.ps1`. | Se deben mover sus API/UI, actualizar imports y el recurso CSS sin alterar el flujo ni debilitar el aislamiento. |
| Experimentos F0 | Contienen corpus sintético y políticas; no son producción ni módulo Maven. | Se preservan hasta una decisión explícita. No se importan desde código de producción. |
| Experimentos F0 — inconsistencia | `container_profiles_test.py` importa `container.create_profile`, pero `experiments/container/` fue eliminado en `c979921`. `experiments/README.md` y `review.md` aún lo mencionan. | El test y las referencias no son ejecutables ni exactos en el árbol actual. Requieren una iteración de decisión y limpieza documental, no reutilización implícita ni borrado del directorio completo. |

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
| D-08 | Retener, extraer o eliminar los archivos F0 obsoletos tras la eliminación del perfil de contenedor. | Confirmada |
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
| `specs/002/experiments/` | Conservar sin integración productiva durante este cambio. |
| `specs/002/experiments/container_profiles_test.py` | Revisar en una fase explícita; actualmente es huérfano y no debe ejecutarse como prueba válida. |
| `specs/002/experiments/README.md` y `specs/002/review.md` | No modificar durante el refactor de cliente. En una limpieza aprobada se corregirán las referencias a `container/` eliminada o se restaurará su fuente histórica si se decide conservarla. |

## 5. Iteraciones atómicas y cobertura del alcance

Cada fase se revisará, compilará y verificará antes de iniciar la siguiente. La cobertura indicada es de requisitos y rutas del plan; el proyecto no declara actualmente una meta automatizada de porcentaje de líneas.

| Fase | Entrega atómica | Archivos y áreas | Evidencia de salida | Dependencia |
|---|---|---|---|---|
| R0 | Línea base y Experimentos F0 | Estado Git, `experiments/`, `WindowsSourceLaunch`, pruebas de Extracción | Inventario de referencias rotas y decisión explícita de conservar/limpiar F0; no eliminar el directorio por inferencia | Aprobación de este plan |
| R1 | Nombre de obra legible | `Work.java`, pruebas de validación/API de Obras | Espacios, acentos y números aceptados; vacío y >255 rechazados; sin migración | R0 |
| R2 | Núcleo, autenticación y API por módulo | Movimientos de `core/http`, `core/ui`, `auth`, `clients`, `works`, `documents`, `extraction`; imports de todos los consumidores | Compilación y pruebas API; una única `ServerSession.shared` usada por todos los clientes | R1 |
| R3 | Recursos y controladores funcionales | Movimientos de shell, clientes, obras, documentos, extracción, FXML y CSS | FXML carga con nuevos controladores/rutas; no quedan referencias a los recursos migrados bajo `presentation/` | R2 |
| R4 | Asistente de nueva obra | `WorkCreationDraft`, `WorkCreationWizard`, `WorksController`, formulario y CSS | Datos iniciales, adjuntos opcionales, captura, revisión, volver y guardado con ETag | R3 |
| R5 | Configuración documental contextual | `ContextDocumentsWindow`, controladores documentales, `DocumentsApiClient`, FXML de documentos | Paquete ligado a obra, tipo interno `Documental`, versión desde DOCX y clasificación de campos | R4 |
| R6 | Experiencia por permisos | Puntos de entrada de Obras y Documentos, pruebas de interfaz posibles | Usuario de obra no abre documentos; usuario documental opera sobre obra existente; usuario integral continúa el flujo | R5 |
| R7 | Regresión y aceptación técnica | Cliente, servidor, F0 que permanezca aplicable, revisión visual Windows | Suites aplicables, `git diff --check`, búsqueda de rutas/imports obsoletos y recorrido manual | R6 |

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
| Experimentos F0 conservados | `python -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v` solo después de resolver o excluir el test huérfano | Entorno aislado, sin datos reales | No presentar como aceptación de aplicación ni ejecutar contra documentos de negocio. |
| Integridad del refactor | Búsqueda de imports `com.brica.client.api.*`, recursos `presentation/` migrados y `git diff --check` | Raíz | Solo permanecen rutas antiguas justificadas; sin errores de espacios ni rutas muertas. |
| Recorrido JavaFX | Iniciar cliente en Windows con cuentas de prueba autorizadas | Windows | Validar pasos, botones, navegación, FXML, carga asíncrona, permisos y mensajes. |
| Documentación DOCX | Cargar un DOCX sintético con marcadores conocidos y uno con marcador no registrado | Windows; no usar archivos reales | Nombre sugerido correcto, clasificación completa, diagnóstico accionable y ninguna creación automática de campos. |

## 7. Riesgos, bloqueos y próximos pasos

- **Refactor transversal:** mover API compartida y FXML puede romper imports, módulos, controladores o rutas. Mitigación: mover por fase, actualizar consumidores en el mismo cambio y ejecutar pruebas FXML/JavaFX.
- **Sesión compartida:** mover `ServerSession` debe preservar exactamente una sesión HTTP, cookies y CSRF. Mitigación: adaptar todos los clientes y ejecutar `ServerSessionTest` tras el movimiento.
- **Cambios de interfaz:** la compilación no valida experiencia de JavaFX. Mitigación: recorrido manual obligatorio en Windows.
- **Concurrencia:** el asistente debe conservar ETag y mostrar conflicto recuperable; no debe sobrescribir información. Mitigación: reutilizar `WorksApiClient.updateInformation` y su error existente.
- **Permisos:** ocultar acciones no autoriza. Mitigación: conservar las anotaciones y controles de servidor; probar usuario sin permisos.
- **Experimentos F0:** su contenido no equivale a la implementación Windows ni a un despliegue aprobado. `container_profiles_test.py` está roto por una eliminación previa; no se debe ignorar el error ni afirmar que la protección de contenedor sigue cubierta.
- **Datos y almacenamiento:** no consultar DB real ni modificar volúmenes. Los adjuntos de pruebas se crearán únicamente mediante la UI/API sobre entorno de prueba autorizado.
- **Siguiente paso sujeto a aprobación:** ejecutar R0; decidir el tratamiento preciso de las referencias huérfanas de `experiments/container` antes de ejecutar o eliminar pruebas F0.
