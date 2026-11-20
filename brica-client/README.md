# Cliente BRICA

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
`brica-client` es la aplicación de escritorio JavaFX de BRICA. Consume la API REST de `brica-server` para presentar operaciones del sistema; no se conecta directamente a PostgreSQL ni reemplaza las reglas de negocio o autorizaciones del servidor.

El cliente forma parte de un reactor Maven con el módulo hermano `brica-server`. La configuración compartida y los comandos del Wrapper están en la raíz del repositorio. Este README describe las convenciones del cliente y su organización objetivo. Para instalar y recorrer el sistema consulta la [guía de instalación](../docs/installation-guide.md) y el [README principal](../README.md).

## Qué está implementado en el módulo

El código actual está bajo `src/main/java/com/brica/client/`, organizado por funcionalidad y responsabilidades compartidas. El árbol objetivo incluye también clases y capacidades que aún no existen; consulta las rutas y el alcance actual antes de usarlas.

| Área funcional actual | Alcance |
|---|---|
| Acceso y sesión | Inicio/cierre de sesión contra el servidor, sesión HTTP, cookies y protección CSRF. |
| Clientes | Consulta paginada, alta, edición y baja lógica desde la interfaz. |
| Obras | Consulta, captura y corrección; adjuntos, revisión ETag y acceso al flujo de extracción. |
| Documentos | Configuración de campos de referencia, plantillas, versiones y paquetes. No genera ni exporta documentos finales. |
| Extracción | Interfaz para iniciar y revisar propuestas del servidor, mapear datos y confirmar operaciones explícitamente. El cliente no analiza archivos localmente. |

La existencia de `contracts`, `agreements`, `estimates` o `generation` en el árbol objetivo no implica que haya pantallas o clientes API funcionales para esas capacidades. La generación, previsualización renderizada y exportación documental no están disponibles en esta versión. El editor de presupuesto aparece dentro del flujo de revisión de extracción y no constituye hoy un módulo independiente.

## Cómo orientarse en el código

La organización objetivo agrupa las clases por responsabilidad transversal y funcionalidad. Dentro de cada módulo, `api` encapsula HTTP y DTOs, `application` coordina estado o tareas de presentación cuando es necesario, y `presentation` contiene las vistas/controladores JavaFX. No todos los módulos necesitan las tres capas.

```text
com/brica/client/
├── config/       # Configuración local, sin reglas de negocio
├── core/         # Mecanismos reutilizables: HTTP, navegación y UI asíncrona
├── shell/        # Ventana principal, navegación y tablero
├── auth/         # Experiencia de inicio/cierre de sesión
├── clients/      # Interfaz y API de clientes
├── works/        # Interfaz y API de obras
├── documents/    # Configuración documental
├── extraction/   # Revisión de propuestas producidas por el servidor
└── shared/       # Solo utilidades de presentación realmente compartidas
```

Este mapa resume la dirección arquitectónica, no un inventario exhaustivo. Los paquetes actuales separan los clientes HTTP y la presentación por funcionalidad; las refactorizaciones deben preservar comportamiento y contratos.

Rutas útiles en el estado actual:

| Ruta | Qué buscar |
|---|---|
| `src/main/java/com/brica/client/BricaClientApplication.java` | Arranque JavaFX y ciclo entre acceso y escritorio. |
| `src/main/java/com/brica/client/core/http/` | Sesión y comprobación de salud del servidor. |
| `src/main/java/com/brica/client/config/ClientConfig.java` | URL del servidor y permiso de HTTP local. |
| `src/main/java/com/brica/client/{clients,works,documents,extraction}/` | Clientes HTTP, DTOs y presentación agrupados por funcionalidad. |
| `src/main/resources/com/brica/client/` | Vistas FXML por funcionalidad y estilos compartidos. |
| `src/test/java/com/brica/client/` | Pruebas de API, sesión, contratos FXML y comportamiento JavaFX. |
| `pom.xml` | Dependencias JavaFX, Jackson, JUnit y ejecución del cliente. |

## Convenciones y decisiones de diseño

Estas convenciones describen el sistema hacia el que se organiza el cliente; se aplican al código existente y guían nuevas clases sin presentar el árbol objetivo como implementación completa:

- Los paquetes funcionales agrupan código de una misma capacidad dentro de un solo ejecutable JavaFX. No son servicios separados ni módulos Maven independientes.
- `api` contiene clientes HTTP y modelos de transporte propios. `presentation` traduce acciones del usuario a esas llamadas; la UI no accede a persistencia ni expone entidades del servidor.
- El cliente refleja permisos para adaptar la experiencia, pero la autorización efectiva siempre corresponde al servidor. No dupliques reglas autoritativas en JavaFX.
- La sesión compartida conserva cookies y CSRF; respeta su ciclo de vida, el manejo de errores y el cierre. No registres credenciales ni detalles sensibles de respuestas HTTP.
- La red y el trabajo prolongado se ejecutan fuera del hilo JavaFX. Actualiza controles en el hilo JavaFX y conserva cancelación, estados de carga y recuperación ante error.
- Mantén separadas las responsabilidades de `shell`, `auth` y los módulos funcionales. Lleva una utilidad a `core` o `shared` solo cuando tenga consumidores reales en más de un contexto.
- Ubica los recursos de cada vista de forma coherente con su módulo objetivo y mantén `fx:controller`, `fx:id`, handlers, includes y rutas de `FXMLLoader` sincronizados durante cualquier movimiento.
- La configuración documental actual administra campos, plantillas, versiones y paquetes. No presentes generación ni exportación final como una capacidad del cliente mientras el servidor no la implemente.
- Los nombres Java y paquetes usan inglés; paquetes en minúsculas, clases en `PascalCase`, y métodos/campos en `camelCase`.
- No hay formatter o linter obligatorio declarado en el POM. Conserva el estilo circundante; ejecuta pruebas y `git diff --check` como verificaciones, no como sustituto de revisión.

## Comandos habituales

Ejecuta Maven desde la raíz del repositorio. En Linux/WSL usa `./mvnw`; en Windows PowerShell, `./mvnw.cmd`.

| Tarea | Linux/WSL | Windows PowerShell |
|---|---|---|
| Ejecutar el escritorio | `./mvnw -pl brica-client javafx:run` | `./mvnw.cmd -pl brica-client javafx:run` |
| Ejecutar pruebas del cliente | `./mvnw -pl brica-client test` | `./mvnw.cmd -pl brica-client test` |
| Empaquetar/verificar el cliente y dependencias del reactor | `./mvnw -pl brica-client verify` | `./mvnw.cmd -pl brica-client verify` |
| Ejecutar una prueba concreta | `./mvnw -pl brica-client -Dtest=NombreTest test` | `./mvnw.cmd -pl brica-client -Dtest=NombreTest test` |

Antes de ejecutar el cliente, inicia `brica-server` y comprueba que responda. La URL se selecciona en este orden: propiedad JVM `brica.server.url`, variable `BRICA_SERVER_URL` y, como valor predeterminado, `http://localhost:8080`. El cliente bloquea HTTP por defecto. `BRICA_ALLOW_HTTP=true` solo se admite para desarrollo local sin TLS y debe configurarse en el proceso del cliente y, cuando corresponda, en el servidor; no es una configuración de despliegue. Maven no carga `.env` automáticamente.

El proyecto usa Java 25 y JavaFX 25; consulta las versiones efectivas en los POM. Las pruebas de contrato FXML o la compilación no sustituyen la comprobación visual de JavaFX, que debe realizarse en Windows. No ejecutes `clean` como solución rutinaria.

## Estructura objetivo propuesta

El siguiente árbol conserva la organización arquitectónica descrita para el cliente. **No es un inventario del código implementado ni una instrucción para crear carpetas vacías.** Los nombres de clases son ilustrativos; confirma cada clase y capacidad en `src/main/java` antes de referirla como existente. La tabla de estado de arriba describe lo implementado actualmente.

```text
BRICA/ # Raíz del repositorio que reúne el cliente, el servidor y la configuración compartida.
├── README.md # Presenta el proyecto y el modo de ejecutar cliente y servidor.
├── AGENTS.md # Conserva las instrucciones de desarrollo del proyecto.
├── CONTRIBUTING.md # Explica las convenciones de colaboración.
├── pom.xml # POM padre que agrupa brica-client y brica-server en el reactor Maven.
├── mvnw # Ejecuta Maven Wrapper desde Linux o WSL.
├── mvnw.cmd # Ejecuta Maven Wrapper desde Windows.
├── .mvn/ # Contiene la configuración del Wrapper compartida por ambos módulos.
├── compose.yml # Levanta los servicios locales requeridos por el servidor.
├── docs/ # Guarda la documentación del sistema completo.
├── brica-server/ # Implementa API REST, reglas de negocio y persistencia.
└── brica-client/ # Implementa el escritorio JavaFX y su comunicación con el servidor.
    ├── pom.xml # Declara JavaFX, serialización, pruebas y ejecución del cliente.
    └── src/ # Sigue el diseño estándar de directorios Maven.
        ├── main/ # Incluye código y recursos empaquetados con el cliente.
        │   ├── java/com/brica/client/ # Paquete raíz del ejecutable JavaFX.
        │   │   ├── BricaClientApplication.java # Crea la ventana principal, abre acceso y escritorio, y gestiona el cierre.
        │   │   │
        │   │   ├── config/ # Lee configuración local del cliente sin conocer procesos de negocio.
        │   │   │   └── ClientConfig.java # Obtiene la URL del servidor y opciones de conexión admitidas.
        │   │   │
        │   │   ├── core/ # Contiene mecanismos compartidos por varias pantallas del escritorio.
        │   │   │   ├── http/ # Gestiona transporte HTTP, sesión y errores comunes de red.
        │   │   │   │   ├── ServerSession.java # Mantiene cookies, CSRF, usuario actual y ciclo de la sesión.
        │   │   │   │   ├── ServerHealthClient.java # Consulta si el servidor responde.
        │   │   │   │   └── ApiProblem.java # Representa un error HTTP legible sin acoplar la UI al cuerpo JSON.
        │   │   │   ├── navigation/ # Resuelve carga de vistas y transición entre secciones.
        │   │   │   │   ├── ViewLoader.java # Carga FXML y obtiene el controlador con rutas consistentes.
        │   │   │   │   └── NavigationService.java # Abre secciones y respeta avisos de cambios sin guardar.
        │   │   │   └── ui/ # Reúne patrones visuales usados realmente por varios módulos.
        │   │   │       ├── AsyncUi.java # Devuelve resultados asíncronos al hilo JavaFX antes de actualizar controles.
        │   │   │       ├── UiPagination.java # Reutiliza el control de páginas de tablas remotas.
        │   │   │       ├── AsyncDialogSave.java # Coordina guardado asíncrono de diálogos reutilizables.
        │   │   │       └── ErrorPresenter.java # Traduce fallos HTTP o de red a mensajes comprensibles.
        │   │   │
        │   │   ├── shell/ # Presenta la ventana principal, el menú y el tablero de inicio.
        │   │   │   └── presentation/ # Contiene controladores de navegación general, no lógica de cada función.
        │   │   │       ├── MainController.java # Abre secciones, indica selección y aplica visibilidad de navegación.
        │   │   │       └── DashboardController.java # Presenta accesos y estado general de la aplicación.
        │   │   │
        │   │   ├── auth/ # Administra la experiencia de inicio y cierre de sesión del usuario interno.
        │   │   │   ├── api/ # Conserva contratos de la API de autenticación usados por la pantalla.
        │   │   │   │   └── LoginException.java # Distingue un rechazo de acceso de otros fallos de conexión.
        │   │   │   └── presentation/ # Presenta el acceso sin definir permisos en el cliente.
        │   │   │       └── LoginController.java # Captura credenciales y solicita autenticación a ServerSession.
        │   │   │
        │   │   ├── clients/ # Presenta la administración de clientes consumiendo la API del servidor.
        │   │   │   ├── api/ # Define cliente HTTP y DTOs de clientes, independientes de controles JavaFX.
        │   │   │   │   ├── ClientsApiClient.java # Envía las solicitudes de clientes y convierte respuestas.
        │   │   │   │   └── ClientDto.java # Representa los datos intercambiados con la API de clientes.
        │   │   │   └── presentation/ # Implementa tabla, formularios y estados visibles de clientes.
        │   │   │       ├── ClientsController.java # Gestiona tabla, selección y acciones de clientes.
        │   │   │       └── ClientFormDialog.java # Captura y valida datos visibles de alta y edición.
        │   │   │
        │   │   ├── works/ # Presenta la consulta, registro y corrección de obras.
        │   │   │   ├── api/ # Aísla el contrato HTTP de obras del código de pantalla.
        │   │   │   │   ├── WorksApiClient.java # Consulta y modifica obras, adjuntos y revisión ETag.
        │   │   │   │   └── WorkDto.java # Representa la obra recibida y enviada mediante la API.
        │   │   │   ├── application/ # Coordina datos de pantalla cuando intervienen varias solicitudes.
        │   │   │   │   └── ClientCatalogLoader.java # Prepara la lista de clientes seleccionables para un formulario de obra.
        │   │   │   └── presentation/ # Implementa tabla, formularios, fechas y acciones de obra.
        │   │   │       ├── WorksController.java # Coordina la pantalla de obras sin implementar acceso HTTP.
        │   │   │       ├── WorkCaptureDialog.java # Reúne la captura y revisión visible antes de guardar.
        │   │   │       ├── WorkInformationForm.java # Edita los datos generales de la obra.
        │   │   │       ├── WorkDocumentInformationDialog.java # Captura datos de obra usados en documentación.
        │   │   │       ├── WorkAttachmentActions.java # Presenta operaciones de carga y consulta de adjuntos.
        │   │   │       ├── WorkDatePicker.java # Configura controles de fecha de la obra.
        │   │   │       └── WorkActionIcon.java # Define iconografía de acciones específicas de obras.
        │   │   │
        │   │   ├── contracts/ # Permite registrar y consultar contratos vinculados a una obra.
        │   │   │   ├── api/ # Aísla solicitudes y respuestas HTTP contractuales.
        │   │   │   │   ├── ContractsApiClient.java # Consume registro y consulta de contratos.
        │   │   │   │   └── ContractDto.java # Representa datos de contrato expuestos por el servidor.
        │   │   │   └── presentation/ # Implementa la captura y consulta contractual.
        │   │   │       ├── ContractsController.java # Presenta contratos de la obra y sus acciones.
        │   │   │       └── ContractFormDialog.java # Captura datos y muestra errores de registro.
        │   │   │
        │   │   ├── budgets/ # Presenta presupuestos y la revisión de sus conceptos.
        │   │   │   ├── api/ # Consume los presupuestos confirmados y sus conceptos.
        │   │   │   │   ├── BudgetsApiClient.java # Envía presupuestos revisados y consulta los guardados.
        │   │   │   │   └── BudgetDto.java # Representa datos y conceptos intercambiados con la API.
        │   │   │   ├── application/ # Mantiene el estado temporal del presupuesto en edición.
        │   │   │   │   └── BudgetDraft.java # Conserva conceptos corregidos antes de confirmar el guardado.
        │   │   │   └── presentation/ # Muestra conceptos y permite su corrección manual.
        │   │   │       ├── BudgetsController.java # Presenta presupuestos asociados con la obra.
        │   │   │       └── BudgetEditor.java # Edita conceptos, unidades, cantidades e importes propuestos.
        │   │   │
        │   │   ├── extraction/ # Permite cargar fuentes, revisar sugerencias y confirmar datos detectados.
        │   │   │   ├── api/ # Aísla las solicitudes HTTP del proceso de extracción del servidor.
        │   │   │   │   ├── ExtractionApiClient.java # Carga archivos y consulta propuestas de extracción.
        │   │   │   │   └── ExtractionDtos.java # Describe estados, procedencia y resultados recibidos.
        │   │   │   ├── application/ # Conserva cambios locales sin persistir datos no confirmados.
        │   │   │   │   └── ExtractionReview.java # Agrupa correcciones y decisiones del usuario sobre sugerencias.
        │   │   │   └── presentation/ # Expone carga, comparación y mapeo manual de datos extraídos.
        │   │   │       ├── ExtractionWindow.java # Muestra archivo, avance, hallazgos y acciones de revisión.
        │   │   │       └── TableMappingDialog.java # Permite mapear columnas cuando la estructura no coincide.
        │   │   │
        │   │   ├── agreements/ # Presenta convenios y cambios de condiciones de una obra.
        │   │   │   ├── api/ # Consume las operaciones de convenios del servidor.
        │   │   │   │   ├── AgreementsApiClient.java # Registra y consulta convenios por obra.
        │   │   │   │   └── AgreementDto.java # Transporta vigencia y condiciones modificadas.
        │   │   │   └── presentation/ # Implementa la vista y captura de convenios.
        │   │   │       ├── AgreementsController.java # Presenta historial y vigencia de convenios.
        │   │   │       └── AgreementFormDialog.java # Captura fecha de entrada en vigor y modificaciones.
        │   │   │
        │   │   ├── estimates/ # Presenta los periodos y conceptos de estimaciones.
        │   │   │   ├── api/ # Consume las operaciones de estimaciones del servidor.
        │   │   │   │   ├── EstimatesApiClient.java # Registra y consulta periodos de estimación.
        │   │   │   │   └── EstimateDto.java # Transporta periodo, importes y conceptos.
        │   │   │   └── presentation/ # Captura y consulta estimaciones de obra.
        │   │   │       ├── EstimatesController.java # Presenta las estimaciones sucesivas.
        │   │   │       └── EstimateFormDialog.java # Captura cantidades y datos de un periodo.
        │   │   │
        │   │   ├── documents/ # Administra campos de referencia, plantillas, versiones y paquetes.
        │   │   │   ├── api/ # Aísla los contratos HTTP del catálogo documental.
        │   │   │   │   ├── DocumentsApiClient.java # Consume configuración, plantillas, campos y paquetes.
        │   │   │   │   └── DocumentDtos.java # Describe solicitudes y respuestas de configuración documental.
        │   │   │   ├── application/ # Coordina estado de edición y protección frente a cambios sin guardar.
        │   │   │   │   └── DocumentConfigurationDraft.java # Conserva cambios locales antes de confirmar configuración.
        │   │   │   └── presentation/ # Implementa pantallas y diálogos de configuración documental.
        │   │   │       ├── DocumentConfigurationController.java # Organiza la configuración documental.
        │   │   │       ├── ContextDocumentsWindow.java # Abre la configuración documental en el contexto de una obra o cliente.
        │   │   │       ├── TemplatesController.java # Lista y administra plantillas.
        │   │   │       ├── TemplateVersionDialogController.java # Revisa versiones, marcadores y obligatoriedad.
        │   │   │       ├── DocumentPackagesController.java # Edita paquetes y su composición.
        │   │   │       ├── ReferenceFieldsController.java # Edita el catálogo de campos.
        │   │   │       ├── ReferenceValuesController.java # Consulta y corrige valores de referencia.
        │   │   │       ├── DocumentInput.java # Aplica reglas de captura usadas por estas pantallas.
        │   │   │       └── DocumentUi.java # Presenta estados y mensajes documentales comunes.
        │   │   │
        │   │   ├── generation/ # Presenta la elaboración, revisión y exportación documental de una obra.
        │   │   │   ├── api/ # Consume los endpoints de generación y descarga del servidor.
        │   │   │   │   ├── GenerationApiClient.java # Inicia, guarda, previsualiza, confirma y consulta generaciones.
        │   │   │   │   └── GenerationDto.java # Transporta borrador, selección fijada, pendientes y resultados.
        │   │   │   ├── application/ # Conserva estado de formulario y cambios todavía no enviados.
        │   │   │   │   └── GenerationDraft.java # Mantiene selección y captura local entre pantallas de la sesión.
        │   │   │   └── presentation/ # Implementa el flujo visible de generación y salida de archivos.
        │   │   │       ├── GenerationsController.java # Lista generaciones previas de la obra y retoma borradores.
        │   │   │       ├── GenerationWizard.java # Guía selección, captura, previsualización y confirmación.
        │   │   │       ├── GenerationPreviewDialog.java # Muestra vista previa y campos faltantes.
        │   │   │       └── GeneratedDocumentsController.java # Presenta resultados y permite exportarlos.
        │   │   │
        │   │   └── shared/ # Conserva únicamente validaciones de interfaz realmente repetidas.
        │   │       └── presentation/ # Reúne utilidades pequeñas de entrada usadas por varios módulos.
        │   │           └── NameInputRules.java # Filtra y comprueba nombres de varios formularios.
        │   └── resources/com/brica/client/ # Recursos JavaFX cargados desde el classpath.
        │       ├── shell/ # Vistas generales de navegación e inicio.
        │       │   ├── main-view.fxml # Define la ventana principal y su controlador.
        │       │   └── dashboard-view.fxml # Define el tablero de inicio.
        │       ├── auth/ # Vistas del acceso de usuarios.
        │       │   └── login-view.fxml # Define el formulario de ingreso.
        │       ├── clients/ # Vistas de administración de clientes.
        │       │   └── clients-view.fxml # Define la tabla y acciones de clientes.
        │       ├── works/ # Vistas de administración de obras.
        │       │   └── works-view.fxml # Define la tabla y acciones de obras.
        │       ├── contracts/ # Vistas contractuales cuando se implemente el flujo.
        │       │   └── contracts-view.fxml # Define la consulta y registro de contratos.
        │       ├── budgets/ # Vistas de presupuestos cuando se implemente el flujo.
        │       │   └── budgets-view.fxml # Define la consulta y edición de presupuestos.
        │       ├── extraction/ # Vistas de carga y revisión cuando resulten útiles como FXML.
        │       │   └── extraction-view.fxml # Define la revisión de resultados y mapeos.
        │       ├── agreements/ # Vistas de convenios cuando se implemente el flujo.
        │       │   └── agreements-view.fxml # Define el historial de convenios.
        │       ├── estimates/ # Vistas de estimaciones cuando se implemente el flujo.
        │       │   └── estimates-view.fxml # Define la consulta de periodos.
        │       ├── documents/ # FXML de campos, plantillas, versiones y paquetes.
        │       │   ├── document-configuration-view.fxml # Define la navegación de configuración documental.
        │       │   ├── templates-view.fxml # Define el catálogo de plantillas.
        │       │   ├── template-version-dialog.fxml # Define el diálogo de versiones.
        │       │   ├── document-packages-view.fxml # Define la composición de paquetes.
        │       │   ├── reference-fields-view.fxml # Define el catálogo de campos.
        │       │   └── reference-values-view.fxml # Define la consulta de valores.
        │       ├── generation/ # FXML de generación cuando sus pantallas se diseñen.
        │       │   └── generations-view.fxml # Define el listado y acceso a borradores de generación.
        │       └── styles/ # Estilos visuales compartidos del escritorio.
        │           └── app.css # Define estilos globales reutilizados entre pantallas.
        └── test/ # Pruebas del cliente y recursos de prueba ajenos al ejecutable final.
            └── java/com/brica/client/ # Refleja paquetes de producción en pruebas relevantes.
                ├── core/ # Comprueba sesión, errores HTTP, navegación y tareas asíncronas.
                ├── auth/ # Comprueba acceso y tratamiento de rechazos.
                ├── clients/ # Comprueba mapeo HTTP y formularios de clientes cuando se necesite.
                ├── works/ # Comprueba revisión ETag y captura de obra cuando se necesite.
                ├── budgets/ # Comprueba edición de conceptos y suma visible cuando se necesite.
                ├── extraction/ # Comprueba revisión de propuestas y mapeos cuando se necesite.
                ├── documents/ # Comprueba controladores y contratos FXML documentales.
                └── generation/ # Comprueba guardar, retomar y confirmar el estado visible.
```
