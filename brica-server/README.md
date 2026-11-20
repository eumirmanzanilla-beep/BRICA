# Servidor BRICA

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
`brica-server` es el backend de BRICA: una aplicación Spring Boot que ofrece la API REST, aplica reglas y permisos, coordina persistencia e integraciones y administra archivos. El escritorio JavaFX consume esta API; no se conecta directamente a PostgreSQL.

El servidor forma parte de un reactor Maven con el módulo hermano `brica-client`. Su configuración compartida y los comandos del Wrapper están en la raíz del repositorio. Este README se centra en el código y desarrollo del backend, no en el recorrido de uso del sistema; para instalarlo y usarlo consulta la [guía de instalación](../docs/installation-guide.md) y el [README principal](../README.md).

## Qué está implementado en el módulo

El código de producción está bajo `src/main/java/com/brica/` y actualmente se organiza en estos paquetes funcionales:

| Módulo | Alcance actual |
|---|---|
| `clients` | Registro, consulta, listado paginado, edición y baja lógica de clientes. |
| `works` | Obras y sus datos; adjuntos asociados, control de revisión/concurrencia y baja lógica. |
| `contracts` | Registro y consulta de contratos asociados con una obra. |
| `budgets` | Presupuestos y revisiones; conserva revisiones inmutables y permite señalar la principal. |
| `documents` | Campos de referencia, análisis de plantillas DOCX, versiones, paquetes y resolución de valores. **No genera ni exporta documentos finales.** |
| `extraction` | Análisis de fuentes admitidas, evidencia y revisión humana; borradores y confirmaciones explícitas por destino. Una propuesta extraída no cambia por sí sola los datos vigentes. |
| `server` | Responsabilidades compartidas del servidor, incluidas seguridad, salud, errores, validación y configuración transversal. |

La presencia de `agreements`, `estimates` o `generation` en el árbol objetivo más abajo no significa que esos paquetes estén implementados: no existen como módulos en el código actual. La generación, previsualización renderizada y exportación de documentos de salida quedan fuera del alcance disponible.

## Cómo orientarse en el código

En general, cada módulo de negocio separa sus responsabilidades en capas. El patrón concreto varía, pero sirve como mapa inicial:

```text
<modulo>/
├── api/             # Controladores y modelos de entrada/salida HTTP
├── application/     # Casos de uso, coordinación y reglas de aplicación
│   └── port/        # Contratos de persistencia o servicios requeridos, cuando aplica
├── domain/          # Modelo y reglas del dominio
└── infrastructure/  # Adaptadores JPA, archivos y procesos externos
```

El flujo típico es `api → application → domain`; infraestructura implementa los puertos y conecta el caso de uso con JPA u otros servicios. No todos los paquetes tienen exactamente las cuatro carpetas. Por ejemplo, `server` contiene preocupaciones transversales con su propia organización. Conviene seguir el módulo existente antes de crear una capa o paquete nuevo.

Rutas útiles dentro de este módulo:

| Ruta | Qué buscar |
|---|---|
| `src/main/java/com/brica/` | Código de producción agrupado por funcionalidad. |
| `src/main/resources/application.yml` | Límites y valores por defecto del servidor; secretos y credenciales se inyectan desde el entorno. |
| `src/main/resources/db/migration/` | Historial Flyway. La última migración presente es V18. Las migraciones aplicadas se conservan intactas; los cambios de esquema se agregan con una nueva versión. |
| `src/main/python/` | Procesadores documentales llamados desde el servidor y sus dependencias. No son un servicio separado. |
| `src/test/java/` | Pruebas unitarias, de integración y de seguridad, organizadas por paquete. |
| `src/test/python/` | Pruebas de los procesadores Python. |
| `Dockerfile` | Construcción multi-stage del backend y preparación del entorno de ejecución. |
| `pom.xml` | Dependencias propias del servidor; las versiones compartidas se definen en el POM raíz. |

## Convenciones observadas

Estas son pautas que se ven en el código y en la configuración del proyecto, no un reglamento aplicado por un analizador automático:

- Los identificadores y paquetes Java usan inglés; los paquetes están en minúsculas, las clases en `PascalCase` y los métodos/campos en `camelCase`.
- Las solicitudes y respuestas HTTP tienen tipos propios del módulo. Evita exponer directamente entidades de persistencia como contrato de API.
- Los controladores traducen HTTP y delegan la lógica; permisos y reglas autoritativas se aplican también en el servidor, no solo en la interfaz.
- Las pruebas Java están bajo `src/test/java`, reflejan los paquetes de producción y normalmente usan el sufijo `Test`. Las pruebas de integración usan PostgreSQL con Testcontainers y requieren Docker.
- Hay pruebas Python bajo `src/test/python`; los procesadores y sus dependencias se mantienen en `src/main/python`.
- Flyway es la fuente de evolución del esquema; Hibernate valida el esquema (`ddl-auto: validate`). No se debe modificar una migración histórica para cambiar el esquema vigente.
- El POM no declara actualmente un formatter o linter obligatorio. Mantén el estilo del archivo y módulo cercano; las pruebas y `git diff --check` son comprobaciones habituales, pero no sustituyen revisión de código.
- Los límites de carga, almacenamiento y extracción son parte de la seguridad. Antes de elevarlos o habilitar el worker en otro entorno, revisa `application.yml`, el `Dockerfile` y las pruebas correspondientes.

## Comandos habituales

Ejecuta Maven desde la raíz del repositorio. En Linux/WSL usa `./mvnw`; en Windows PowerShell, `./mvnw.cmd`.

| Tarea | Linux/WSL | Windows PowerShell |
|---|---|---|
| Ejecutar Spring Boot localmente | `./mvnw -pl brica-server spring-boot:run` | `./mvnw.cmd -pl brica-server spring-boot:run` |
| Ejecutar pruebas del servidor | `./mvnw -pl brica-server test` | `./mvnw.cmd -pl brica-server test` |
| Empaquetar/verificar servidor y dependencias del reactor | `./mvnw -pl brica-server verify` | `./mvnw.cmd -pl brica-server verify` |
| Ejecutar una prueba concreta | `./mvnw -pl brica-server -Dtest=NombreTest test` | `./mvnw.cmd -pl brica-server -Dtest=NombreTest test` |
| Ejecutar pruebas del procesador Python | `python3 -m unittest discover -s brica-server/src/test/python -v` | Requiere Python y dependencias instaladas; el comando puede ejecutarse desde PowerShell con `python`. |
| Ejecutar el backend en Docker junto con PostgreSQL | `docker compose up -d --build brica-server` | Igual |
| Consultar logs del contenedor | `docker compose logs -f brica-server` | Igual |

Para ejecutar Spring Boot fuera de Docker, prepara PostgreSQL y exporta `DB_URL`, `DB_USER` y `DB_PASSWORD` al entorno del proceso. Maven no carga `.env` automáticamente. Si se ejecutan flujos documentales localmente, prepara el entorno Python descrito en el [README principal](../README.md) y configura `BRICA_DOCUMENT_PYTHON` y `BRICA_DOCUMENT_SCRIPT`. El worker de extracción requiere un `BRICA_EXTRACTION_CGROUP_ROOT` delegado; si está vacío, el procesamiento se cierra de forma segura.

`verify` puede ejecutar pruebas de integración y requiere Docker disponible para Testcontainers. Para las funciones Python locales también se requiere el entorno indicado. No ejecutes `clean` como solución rutinaria: consulta las instrucciones del repositorio y conserva los artefactos/datos persistentes.

## Estructura objetivo propuesta

El siguiente árbol conserva el diseño objetivo documentado para el sistema completo. **No es un inventario del código que existe hoy ni una instrucción para crear carpetas vacías.** Incluye módulos futuros y nombres ilustrativos; confirma cada clase contra `src/main/java` antes de referenciarla como implementada. Para el árbol actual, usa la tabla de módulos y rutas de arriba.

```text
BRICA/ # Raíz del repositorio que reúne el servidor, el cliente y la configuración de desarrollo.
├── README.md # Presenta el proyecto, su estado real y la forma de ejecutarlo.
├── AGENTS.md # Conserva las instrucciones de trabajo del proyecto.
├── CONTRIBUTING.md # Explica las convenciones de contribución, ramas y cambios.
├── pom.xml # POM padre que define los módulos Maven, versiones y configuración compartida.
├── mvnw # Ejecuta Maven Wrapper en Linux y WSL.
├── mvnw.cmd # Ejecuta Maven Wrapper en Windows.
├── .mvn/ # Contiene los archivos necesarios para Maven Wrapper.
├── .gitignore # Excluye compilados, secretos y archivos locales del control de versiones.
├── .dockerignore # Excluye archivos innecesarios del contexto de Docker.
├── .env.example # Documenta variables de entorno de ejemplo sin incluir secretos.
├── compose.yml # Define los servicios locales, especialmente PostgreSQL.
├── docs/ # Reúne requisitos, especificaciones y documentación general del proyecto.
├── brica-client/ # Aplicación de escritorio JavaFX que consume la API del servidor.
└── brica-server/ # Aplicación Spring Boot que aplica reglas, expone la API y administra los datos.
    ├── pom.xml # Declara dependencias y empaquetado específicos del servidor.
    ├── Dockerfile # Construye la imagen ejecutable del servidor.
    └── src/ # Agrupa el código y los recursos según la estructura estándar de Maven.
        ├── main/ # Contenido incluido en la aplicación de producción.
        │   ├── java/ # Código fuente Java del backend.
        │   │   └── com/brica/ # Paquete raíz común para el escaneo de Spring Boot.
        │   │       ├── BricaServerApplication.java # Inicia Spring Boot y define el paquete base de la aplicación.
        │   │       │
        │   │       ├── clients/ # Administra la identidad y los datos de los clientes.
        │   │       │   ├── api/ # Traduce las operaciones de clientes entre HTTP y casos de uso.
        │   │       │   │   ├── ClientController.java # Expone registro, consulta, listado, edición y baja lógica.
        │   │       │   │   ├── ClientResponse.java # Devuelve al cliente JavaFX una vista sin exponer la entidad persistida.
        │   │       │   │   ├── RegisterClientRequest.java # Recibe y valida los datos de alta.
        │   │       │   │   └── UpdateClientRequest.java # Recibe los cambios permitidos en una edición.
        │   │       │   ├── application/ # Coordina los casos de uso y sus reglas de operación.
        │   │       │   │   ├── RegisterClientService.java # Registra un cliente válido.
        │   │       │   │   ├── UpdateClientService.java # Actualiza un cliente existente.
        │   │       │   │   ├── ConsultClientService.java # Recupera un cliente para consulta.
        │   │       │   │   ├── ListClientsService.java # Lista clientes con paginación.
        │   │       │   │   ├── DeleteClientService.java # Aplica la baja lógica sin borrar el historial.
        │   │       │   │   └── port/ # Define las dependencias que los casos de uso necesitan de la persistencia.
        │   │       │   │       └── ClientStore.java # Contrato para guardar y consultar clientes.
        │   │       │   ├── domain/ # Contiene el modelo y las reglas propias del cliente.
        │   │       │   │   └── Client.java # Representa la identidad y el estado del cliente.
        │   │       │   └── infrastructure/ # Implementa el almacenamiento del módulo.
        │   │       │       ├── ClientRepository.java # Define las consultas de clientes con Spring Data JPA.
        │   │       │       └── JpaClientStore.java # Adapta el repositorio JPA al puerto ClientStore.
        │   │       │
        │   │       ├── works/ # Administra las obras y sus datos comunes reutilizables.
        │   │       │   ├── api/ # Traduce solicitudes HTTP sobre obras y adjuntos.
        │   │       │   │   ├── WorkController.java # Expone registro, consulta, corrección, listado y baja.
        │   │       │   │   ├── WorkAttachmentController.java # Recibe y consulta adjuntos asociados con la obra.
        │   │       │   │   ├── RegisterWorkRequest.java # Valida los datos para registrar una obra.
        │   │       │   │   ├── CorrectWorkRequest.java # Define los datos corregibles de una obra.
        │   │       │   │   └── WorkResponse.java # Presenta los datos y la revisión vigente al cliente.
        │   │       │   ├── application/ # Ejecuta las operaciones de obras y protege su consistencia.
        │   │       │   │   ├── RegisterWorkService.java # Crea una obra asociada con un cliente válido.
        │   │       │   │   ├── CorrectWorkService.java # Corrige la obra respetando su revisión y la condición If-Match.
        │   │       │   │   ├── ConsultWorkService.java # Obtiene una obra concreta.
        │   │       │   │   ├── ListWorksService.java # Recupera obras paginadas.
        │   │       │   │   ├── DeleteWorkService.java # Realiza la baja lógica de una obra.
        │   │       │   │   ├── WorkAttachmentService.java # Asocia y consulta los adjuntos de obra.
        │   │       │   │   ├── WorkRevision.java # Representa la versión usada por el control optimista.
        │   │       │   │   └── port/ # Define el acceso requerido a obras y sus adjuntos.
        │   │       │   │       ├── WorkStore.java # Contrato para guardar y consultar obras.
        │   │       │   │       └── WorkAttachmentStore.java # Contrato para relacionar archivos con obras.
        │   │       │   ├── domain/ # Conserva las reglas y estados de la obra.
        │   │       │   │   ├── Work.java # Representa la obra como núcleo del proceso.
        │   │       │   │   ├── WorkStatus.java # Enumera los estados válidos de la obra.
        │   │       │   │   └── WorkAttachment.java # Representa la relación entre obra y archivo adjunto.
        │   │       │   └── infrastructure/ # Implementa las consultas y el almacenamiento JPA de obras.
        │   │       │       ├── WorkRepository.java # Define consultas Spring Data sobre obras.
        │   │       │       ├── JpaWorkStore.java # Implementa el puerto WorkStore.
        │   │       │       └── JpaWorkAttachmentStore.java # Implementa el almacenamiento de asociaciones con adjuntos.
        │   │       │
        │   │       ├── contracts/ # Administra los contratos asociados con las obras.
        │   │       │   ├── api/ # Expone operaciones HTTP y modelos de entrada y salida contractual.
        │   │       │   │   ├── ContractController.java # Recibe el registro y la consulta de contratos.
        │   │       │   │   ├── RegisterContractRequest.java # Valida la información contractual capturada.
        │   │       │   │   └── ContractResponse.java # Entrega una vista del contrato sin exponer la entidad.
        │   │       │   ├── application/ # Coordina los casos de uso contractuales.
        │   │       │   │   ├── RegisterContractService.java # Registra un contrato ligado a la obra.
        │   │       │   │   └── port/ # Define la persistencia requerida por el caso de uso.
        │   │       │   │       └── ContractStore.java # Contrato para guardar y recuperar contratos.
        │   │       │   ├── domain/ # Modela el contrato y sus datos propios.
        │   │       │   │   ├── Contract.java # Representa el contrato asociado con una obra.
        │   │       │   │   └── ContractData.java # Agrupa datos contractuales variables sin convertir el JSON en DTO HTTP.
        │   │       │   └── infrastructure/ # Implementa la persistencia contractual.
        │   │       │       ├── ContractRepository.java # Define consultas JPA de contratos.
        │   │       │       └── JpaContractStore.java # Adapta JPA al puerto ContractStore.
        │   │       │
        │   │       ├── budgets/ # Administra presupuestos y conceptos económicos confirmados.
        │   │       │   ├── api/ # Expone las operaciones HTTP de presupuestos ya revisados.
        │   │       │   │   ├── BudgetController.java # Registra y consulta presupuestos de una obra.
        │   │       │   │   ├── RegisterBudgetRequest.java # Recibe conceptos y montos revisados para persistirlos.
        │   │       │   │   └── BudgetResponse.java # Muestra presupuesto, conceptos e importes guardados.
        │   │       │   ├── application/ # Coordina la validación y persistencia del presupuesto confirmado.
        │   │       │   │   ├── RegisterBudgetService.java # Guarda los conceptos aceptados por el usuario.
        │   │       │   │   └── port/ # Define la persistencia requerida por Presupuestos.
        │   │       │   │       └── BudgetStore.java # Contrato para guardar y consultar presupuestos.
        │   │       │   ├── domain/ # Contiene los conceptos y reglas económicas del presupuesto.
        │   │       │   │   ├── Budget.java # Representa el presupuesto vigente de una obra.
        │   │       │   │   └── BudgetItem.java # Representa un concepto, cantidad, unidad y valores asociados.
        │   │       │   └── infrastructure/ # Implementa el almacenamiento del presupuesto.
        │   │       │       ├── BudgetRepository.java # Define consultas JPA de presupuestos.
        │   │       │       └── JpaBudgetStore.java # Implementa BudgetStore con JPA.
        │   │       │
        │   │       ├── extraction/ # Lee documentos de entrada y propone datos para revisión humana.
        │   │       │   ├── api/ # Recibe archivos fuente y entrega propuestas sin confirmar datos de negocio.
        │   │       │   │   ├── ExtractionController.java # Inicia una extracción y consulta sus resultados.
        │   │       │   │   ├── StartExtractionRequest.java # Identifica obra, tipo de fuente y opciones de lectura.
        │   │       │   │   └── ExtractionResponse.java # Entrega datos detectados, origen, advertencias e incertidumbre.
        │   │       │   ├── application/ # Coordina lectura, normalización y revisión de resultados extraídos.
        │   │       │   │   ├── ExtractSourceService.java # Selecciona el lector apropiado y produce una propuesta.
        │   │       │   │   ├── ReviewExtractionService.java # Registra correcciones y decisión del usuario antes de confirmar.
        │   │       │   │   └── port/ # Define lectores y almacenamiento de trabajos de extracción.
        │   │       │   │       ├── SourceReader.java # Contrato común para extraer datos de una fuente admitida.
        │   │       │   │       └── ExtractionStore.java # Persiste una propuesta si debe retomarse o auditarse.
        │   │       │   ├── domain/ # Modela propuestas y procedencia, sin sustituir entidades de negocio.
        │   │       │   │   ├── ExtractionJob.java # Representa estado, archivo y tipo de extracción.
        │   │       │   │   ├── ExtractedField.java # Registra valor sugerido, ubicación y confianza o advertencia.
        │   │       │   │   └── ExtractionStatus.java # Define estados de análisis y revisión.
        │   │       │   └── infrastructure/ # Implementa lectores por formato y persistencia del proceso.
        │   │       │       ├── SpreadsheetSourceReader.java # Lee hojas de cálculo preservando conceptos y procedencia.
        │   │       │       ├── PdfTextSourceReader.java # Lee texto y estructura disponibles en PDF.
        │   │       │       ├── PdfOcrSourceReader.java # Extrae texto localmente cuando el PDF es escaneado o mixto.
        │   │       │       ├── DocxSourceReader.java # Lee fuentes DOCX destinadas a extraer datos de negocio.
        │   │       │       └── JpaExtractionStore.java # Guarda trabajos y propuestas que deban persistirse.
        │   │       │
        │   │       ├── agreements/ # Administra convenios y cambios vigentes a partir de una fecha.
        │   │       │   ├── api/ # Expone el registro y la consulta HTTP de convenios.
        │   │       │   │   ├── AgreementController.java # Recibe altas y consultas de convenios.
        │   │       │   │   └── RegisterAgreementRequest.java # Recibe vigencia y condiciones modificadas.
        │   │       │   ├── application/ # Aplica reglas temporales y coordina el registro.
        │   │       │   │   ├── RegisterAgreementService.java # Guarda un convenio sin alterar periodos históricos.
        │   │       │   │   ├── ResolveEffectiveTermsService.java # Resuelve las condiciones aplicables a una fecha dada.
        │   │       │   │   └── port/ # Define consultas de convenios por obra y fecha.
        │   │       │   │       └── AgreementStore.java # Contrato para registrar y buscar convenios vigentes.
        │   │       │   ├── domain/ # Modela vigencias y modificaciones contractuales.
        │   │       │   │   ├── Agreement.java # Representa un convenio asociado con una obra.
        │   │       │   │   └── AgreementChange.java # Representa una condición modificada y su valor.
        │   │       │   └── infrastructure/ # Implementa la persistencia y búsqueda temporal.
        │   │       │       └── JpaAgreementStore.java # Busca convenios aplicables a una fecha o periodo.
        │   │       │
        │   │       ├── estimates/ # Administra las estimaciones y sus periodos durante la ejecución.
        │   │       │   ├── api/ # Expone registro y consulta HTTP de estimaciones.
        │   │       │   │   ├── EstimateController.java # Recibe operaciones sobre periodos de estimación.
        │   │       │   │   └── RegisterEstimateRequest.java # Recibe periodo y cantidades por concepto.
        │   │       │   ├── application/ # Valida estimaciones y coordina su registro.
        │   │       │   │   ├── RegisterEstimateService.java # Registra un periodo con sus conceptos y condiciones vigentes.
        │   │       │   │   └── port/ # Define consultas y persistencia de estimaciones.
        │   │       │   │       └── EstimateStore.java # Contrato para guardar y recuperar estimaciones.
        │   │       │   ├── domain/ # Modela estimaciones, periodos y cantidades.
        │   │       │   │   ├── Estimate.java # Representa la estimación de una obra en un periodo.
        │   │       │   │   └── EstimateItem.java # Representa la cantidad estimada de un concepto.
        │   │       │   └── infrastructure/ # Implementa las consultas y escritura de estimaciones.
        │   │       │       └── JpaEstimateStore.java # Implementa EstimateStore mediante JPA.
        │   │       │
        │   │       ├── documents/ # Configura campos, plantillas, versiones y paquetes documentales.
        │   │       │   ├── api/ # Expone configuración y consultas HTTP del catálogo documental.
        │   │       │   │   ├── TemplateController.java # Administra plantillas y versiones.
        │   │       │   │   ├── ReferenceFieldController.java # Administra el catálogo de campos de referencia.
        │   │       │   │   ├── ReferenceValueController.java # Consulta valores disponibles para los campos.
        │   │       │   │   ├── DocumentPackageController.java # Configura paquetes y consulta su composición.
        │   │       │   │   └── DocumentConfigurationController.java # Expone opciones y validaciones de configuración.
        │   │       │   ├── application/ # Coordina administración, análisis y selección de configuración.
        │   │       │   │   ├── RegisterTemplateVersionService.java # Registra una versión sin sobrescribir versiones anteriores.
        │   │       │   │   ├── TemplateAnalysisService.java # Detecta marcadores de una plantilla antes de registrarla.
        │   │       │   │   ├── TemplateManagementService.java # Administra estados y versiones de plantillas.
        │   │       │   │   ├── DocumentPackageService.java # Administra la composición de paquetes.
        │   │       │   │   ├── DocumentSelectionService.java # Valida paquetes, plantillas y versiones seleccionables.
        │   │       │   │   ├── ResolveReferenceValuesService.java # Resuelve valores de campos para previsualización y generación.
        │   │       │   │   └── port/ # Aísla la configuración de JPA, archivos y analizadores DOCX.
        │   │       │   │       ├── DocumentConfigurationStore.java # Persiste paquetes, plantillas y versiones.
        │   │       │   │       ├── ReferenceFieldStore.java # Persiste definiciones de campos de referencia.
        │   │       │   │       ├── DocxAnalyzer.java # Analiza marcadores sin acoplar servicios al proceso Python.
        │   │       │   │       └── WorkReferenceSource.java # Consulta datos de obra a través de un contrato limitado.
        │   │       │   ├── domain/ # Conserva reglas y entidades de configuración documental.
        │   │       │   │   ├── DocumentTemplate.java # Representa una plantilla documental y su estado.
        │   │       │   │   ├── TemplateVersion.java # Conserva el contenido y configuración de una versión concreta.
        │   │       │   │   ├── TemplateVersionField.java # Relaciona campos detectados y obligatoriedad por versión.
        │   │       │   │   ├── ReferenceField.java # Define una clave de referencia y su tipo.
        │   │       │   │   ├── ReferenceValue.java # Representa un valor resuelto y su origen.
        │   │       │   │   └── DocumentPackage.java # Representa el paquete y su composición.
        │   │       │   └── infrastructure/ # Implementa JPA y el análisis técnico de plantillas.
        │   │       │       ├── JpaDocumentConfigurationStore.java # Implementa persistencia de paquetes y plantillas.
        │   │       │       ├── JpaReferenceFieldStore.java # Implementa la persistencia del catálogo de campos.
        │   │       │       ├── JpaWorkReferenceSource.java # Obtiene datos de obra para resolver referencias.
        │   │       │       └── PythonDocxAnalyzer.java # Invoca el analizador de marcadores DOCX existente.
        │   │       │
        │   │       ├── generation/ # Ejecuta generaciones documentales y conserva sus resultados.
        │   │       │   ├── api/ # Expone borradores, previsualización, confirmación y exportación por HTTP.
        │   │       │   │   ├── DocumentGenerationController.java # Inicia, retoma, previsualiza y confirma generaciones.
        │   │       │   │   ├── GeneratedDocumentController.java # Consulta y exporta documentos producidos.
        │   │       │   │   └── GenerationResponse.java # Informa estado, datos pendientes, versiones fijadas y salidas.
        │   │       │   ├── application/ # Coordina la ejecución y sus transacciones sin definir formatos físicos.
        │   │       │   │   ├── StartGenerationService.java # Crea una generación con paquete y versiones elegidas.
        │   │       │   │   ├── SaveGenerationDraftService.java # Guarda información parcial para continuar después.
        │   │       │   │   ├── PreviewGenerationService.java # Detecta faltantes y produce una vista previa.
        │   │       │   │   ├── ConfirmGenerationService.java # Produce salidas definitivas sin reemplazar generaciones previas.
        │   │       │   │   ├── ExportGeneratedDocumentsService.java # Entrega uno o varios archivos ya generados.
        │   │       │   │   └── port/ # Define el acceso a datos y motores que requiere una generación.
        │   │       │   │       ├── GenerationStore.java # Persiste borradores, selecciones y resultados.
        │   │       │   │       ├── GenerationContextSource.java # Obtiene datos vigentes y datos de periodo mediante interfaces públicas.
        │   │       │   │       └── DocumentRenderer.java # Sustituye campos y renderiza sin fijar la biblioteca en el caso de uso.
        │   │       │   ├── domain/ # Modela la ejecución y sus documentos con versiones fijadas.
        │   │       │   │   ├── DocumentGeneration.java # Conserva estado, captura y selección de una ejecución.
        │   │       │   │   ├── GenerationStatus.java # Delimita borrador, vista previa, confirmación y estados de error.
        │   │       │   │   └── GeneratedDocument.java # Relaciona salida, versión de plantilla y archivo producido.
        │   │       │   └── infrastructure/ # Implementa persistencia, armado del contexto y motor documental.
        │   │       │       ├── JpaGenerationStore.java # Guarda generaciones y sus documentos producidos.
        │   │       │       ├── BricaGenerationContextSource.java # Reúne datos de obra, convenio y estimación para el periodo.
        │   │       │       └── DocxDocumentRenderer.java # Genera DOCX respetando cuerpo, encabezados y pies.
        │   │       │
        │   │       ├── files/ # Administra archivos físicos y sus metadatos para todos los módulos.
        │   │       │   ├── application/ # Coordina operaciones de almacenamiento e integridad.
        │   │       │   │   ├── StoreFileService.java # Almacena un archivo y registra su hash SHA-256.
        │   │       │   │   ├── ReadFileService.java # Recupera un archivo y verifica disponibilidad e integridad.
        │   │       │   │   └── port/ # Define acceso a bytes y metadatos sin fijar tecnología.
        │   │       │   │       ├── FileStorage.java # Contrato de lectura y escritura de contenido físico.
        │   │       │   │       └── FileMetadataStore.java # Contrato para persistir y consultar metadatos.
        │   │       │   ├── domain/ # Representa la identidad y los atributos de un archivo administrado.
        │   │       │   │   └── StoredFile.java # Conserva nombre, tipo, tamaño, ruta y hash del archivo.
        │   │       │   └── infrastructure/ # Implementa disco local administrado y metadatos en PostgreSQL.
        │   │       │       ├── LocalFileStorage.java # Escribe bajo una ruta configurable fuera del código fuente.
        │   │       │       └── JpaFileMetadataStore.java # Persiste los metadatos mediante JPA.
        │   │       │
        │   │       ├── security/ # Protege operaciones y administra usuarios, roles y autorizaciones.
        │   │       │   ├── api/ # Recibe autenticación y administración HTTP de seguridad.
        │   │       │   │   ├── AuthenticationController.java # Atiende las operaciones de autenticación.
        │   │       │   │   ├── SecurityAdministrationController.java # Administra usuarios y roles autorizados.
        │   │       │   │   └── ApprovalRequestController.java # Recibe solicitudes y decisiones de aprobación.
        │   │       │   ├── application/ # Aplica políticas de acceso y coordina la administración.
        │   │       │   │   ├── AccessPolicy.java # Evalúa los permisos de la operación solicitada.
        │   │       │   │   ├── SecurityAdministrationService.java # Administra cuentas, roles y permisos.
        │   │       │   │   ├── ResolveApprovalRequestService.java # Aprueba o rechaza solicitudes registrando la decisión.
        │   │       │   │   └── port/ # Define las operaciones requeridas sobre cuentas y solicitudes.
        │   │       │   │       ├── AccountStore.java # Consulta y persiste cuentas de usuarios.
        │   │       │   │       └── ApprovalRequestStore.java # Guarda y recupera solicitudes de autorización.
        │   │       │   ├── domain/ # Modela usuarios y reglas de autorización.
        │   │       │   │   ├── AppUser.java # Representa una cuenta interna.
        │   │       │   │   ├── Role.java # Agrupa permisos y nivel jerárquico.
        │   │       │   │   ├── Permission.java # Identifica una operación autorizable.
        │   │       │   │   └── ApprovalRequest.java # Registra solicitante, resolutor, estado y fechas.
        │   │       │   └── infrastructure/ # Configura filtros y adaptadores JPA de autenticación.
        │   │       │       ├── SecurityConfiguration.java # Configura la cadena de seguridad HTTP.
        │   │       │       ├── PasswordConfiguration.java # Proporciona el codificador seguro de contraseñas.
        │   │       │       ├── JpaAccountStore.java # Implementa el puerto de cuentas con JPA.
        │   │       │       └── JpaApprovalRequestStore.java # Implementa la persistencia de solicitudes.
        │   │       │
        │   │       └── shared/ # Contiene solo mecanismos usados realmente por varios módulos.
        │   │           ├── error/ # Homogeneiza las respuestas HTTP de error.
        │   │           │   └── GlobalExceptionHandler.java # Traduce fallos de aplicación a respuestas HTTP coherentes.
        │   │           └── validation/ # Reúne validaciones verdaderamente comunes, no reglas de un módulo.
        │   │               └── NameValidator.java # Aplica una regla compartida de nombres si la usan varios contextos.
        │   ├── python/ # Aloja el adaptador Python existente para inspeccionar plantillas DOCX.
        │   │   ├── brica_docx.py # Analiza contenido y marcadores DOCX invocado por el adaptador Java.
        │   │   └── requirements.txt # Fija las dependencias Python necesarias para ejecutar el analizador.
        │   └── resources/ # Recursos empacados con el servidor.
        │       ├── application.yml # Configura valores base; las credenciales se obtienen del entorno.
        │       └── db/ # Recursos de la base de datos.
        │           └── migration/ # Mantiene una única secuencia Flyway para toda la base.
        │               ├── V1__create_cliente_table.sql # Migración histórica de clientes; se conserva intacta.
        │               ├── V2__create_works_table.sql # Migración histórica de obras; se conserva intacta.
        │               ├── V3__create-contracts-table.sql # Migración histórica de contratos; se conserva intacta.
        │               ├── V4__add_soft_delete_to_clients_and_works.sql # Migración histórica de baja lógica; se conserva intacta.
        │               └── V...__descripcion.sql # Representa cada migración posterior, numerada sin reutilizar versiones.
        └── test/ # Aloja pruebas y fixtures excluidos del artefacto de producción.
            ├── java/com/brica/ # Refleja los paquetes funcionales en las pruebas Java.
            │   ├── clients/ # Comprueba operaciones y API de clientes.
            │   ├── works/ # Comprueba obras, adjuntos y control optimista.
            │   ├── contracts/ # Comprueba contratos y su vínculo con obras.
            │   ├── budgets/ # Comprueba conceptos y confirmación de presupuestos.
            │   ├── extraction/ # Comprueba lectores, procedencia y revisión humana.
            │   ├── agreements/ # Comprueba vigencias y condiciones aplicables.
            │   ├── estimates/ # Comprueba periodos y conceptos estimados.
            │   ├── documents/ # Comprueba campos, plantillas, versiones y paquetes.
            │   ├── generation/ # Comprueba borradores, selección fijada y reproducción.
            │   ├── files/ # Comprueba integridad y almacenamiento de archivos.
            │   └── security/ # Comprueba autenticación y autorización.
            └── resources/ # Conserva datos de entrada controlados para las pruebas.
                └── documents/ # Guarda fixtures DOCX y documentos de prueba deterministas.
```
