# Instrucciones para trabajar en BRICA

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
## Propósito y estado del sistema

BRICA centraliza la información de clientes y obras de una constructora. La obra es el eje del dominio: sus datos y documentos se reutilizan en operaciones administrativas y documentales. El repositorio contiene un servidor Spring Boot con API REST y un cliente de escritorio JavaFX; el cliente no accede directamente a PostgreSQL.

El alcance funcional continúa evolucionando. Antes de implementar una función, confirma que existe en el código y en la especificación activa de la tarea: los requisitos del ERS y el diseño del DAS describen también capacidades futuras (por ejemplo, convenios, estimaciones, aprobaciones y generación/exportación documental completa) que no deben darse por implementadas por el solo hecho de estar documentadas.

## Fuentes de verdad y documentación

Consulta las fuentes según su propósito:

1. Para el comportamiento ya implementado y la configuración efectiva: código de la rama actual, `pom.xml` de cada módulo, `compose.yml`, `.env.example`, `brica-server/src/main/resources/application.yml` y migraciones Flyway.
2. Para principios de arquitectura y flujo de contribución: `docs/constitution.md`, [`CONTRIBUTING.md`](CONTRIBUTING.md) y este archivo.
3. Para requisitos y significado del negocio: `docs/informative/ERS_BRICA.docx` (ERS), `docs/informative/DAS_BRICA.docx` (DAS) y `docs/informative/DiccionarioDatos_BRICA.docx`.
4. Para estado funcional, puesta en marcha y verificaciones: `README.md`, `docs/installation-guide.md`, `specs/` y los README de módulo.

El ERS, el DAS y el Diccionario son documentos iniciales: expresan intención y modelo lógico, no garantizan que una función o columna exista hoy. Los README de módulo pueden describir una arquitectura objetivo. Si una fuente contradice el código o el esquema vigente, no inventes una reconciliación ni cambies silenciosamente el alcance: explica la discrepancia y consulta al responsable cuando afecte una decisión de negocio, arquitectura o datos. Para cambios de esquema, comprueba además la última migración y las entidades/consultas actuales.

## Entorno y comandos

- Proyecto Maven multimódulo con Wrapper: `brica-server` y `brica-client`. Java 25; Spring Boot 4.1.1 en servidor, JavaFX 25.0.2 en escritorio y PostgreSQL 18 en Compose. Las versiones efectivas están en los POM y archivos de configuración.
- Ejecuta los comandos desde la raíz. En Linux/WSL utiliza `./mvnw`; en Windows PowerShell, `./mvnw.cmd`.
- PostgreSQL local: configura `.env` a partir de `.env.example` y ejecuta `docker compose up -d postgres`. Compose publica por defecto el puerto host `5433`; Maven no carga `.env` automáticamente.
- Backend fuera de Docker: exporta `DB_URL`, `DB_USER` y `DB_PASSWORD`, y ejecuta `./mvnw -pl brica-server spring-boot:run`. Para las funciones documentales Python, instala `brica-server/src/main/python/requirements.txt` en un entorno aislado y configura `BRICA_DOCUMENT_PYTHON` y `BRICA_DOCUMENT_SCRIPT`, siguiendo `README.md`. La imagen del servidor ya prepara su entorno Python.
- Alternativa de servidor en contenedor: `docker compose up -d --build brica-server`. No levantes simultáneamente otro backend en el mismo puerto.
- Escritorio en Windows: `./mvnw.cmd -pl brica-client javafx:run`. La URL se configura con `BRICA_SERVER_URL` o `brica.server.url`; la propiedad JVM tiene precedencia.
- HTTP está deshabilitado por defecto. `BRICA_ALLOW_HTTP=true` es solo para desarrollo local sin TLS y debe configurarse en los procesos correspondientes. No lo presentes como configuración de despliegue.
- Verificaciones habituales: `./mvnw test`, `./mvnw verify` y `git diff --check`. Las pruebas de integración utilizan PostgreSQL mediante Testcontainers y requieren Docker. La comprobación visual de JavaFX debe hacerse en Windows; la compilación no la sustituye.
- No ejecutes `clean`, comandos que eliminen volúmenes/datos, ni limpiezas amplias salvo autorización expresa. Los volúmenes de PostgreSQL y documentos son persistentes; no uses `docker compose down -v` para resolver problemas.

## Arquitectura y convenciones de código

### Backend (`brica-server`)

- Está organizado por funcionalidad bajo `com.brica` (por ejemplo `clients`, `works`, `contracts`, `budgets`, `documents`, `extraction` y `server.security`). Respeta la estructura real del módulo al trabajar; no reorganices paquetes para ajustarlos a diagramas o árboles objetivo.
- En los módulos de negocio, conserva el flujo `api` → `application` → `domain`; `infrastructure` adapta persistencia y servicios externos. Los puertos requeridos por aplicación se ubican donde indique el módulo actual (habitualmente `application.port`). Mantén el dominio independiente de Spring, JPA, HTTP y herramientas externas en la medida en que lo permita el diseño existente.
- `api` traduce HTTP a casos de uso y usa requests/responses propios; evita exponer entidades JPA como contrato de API. Valida entradas en el servidor, aplica reglas y autorización en la capa correspondiente y conserva el formato de errores existente (`ProblemDetail` / `ApiExceptionHandler`).
- Seguridad no depende de ocultar opciones en JavaFX: autoriza en el servidor. Respeta autenticación por sesión, CSRF, permisos y política de transporte existentes.
- Mantén paginación, baja lógica, idempotencia y control de concurrencia (ETag/`If-Match` o revisión) donde ya existan. No elimines mecanismos por simplificar una operación.
- La infraestructura de archivos administra el almacenamiento; la base de datos guarda metadatos. No incorpores acceso directo a archivos o base desde el cliente.

### Cliente (`brica-client`)

- Conserva la separación existente entre `api`, `config` y `presentation`. Los clientes HTTP/DTOs consumen la API; no dupliques reglas autoritativas del servidor ni conectes JavaFX directamente a PostgreSQL.
- No bloquees el hilo JavaFX con red, disco o trabajo prolongado. Usa el patrón asíncrono existente y actualiza controles en el hilo JavaFX. Conserva sesión, CSRF, manejo de errores y cancelación/cierre tal como los módulos los implementan.
- Al modificar vistas/controladores, revisa FXML, rutas de recursos, `fx:id`, handlers y pruebas de contrato FXML/smoke aplicables. Mantén la presentación y la captura separadas de la lógica de negocio.

### Nombres, datos y pruebas

- El código nuevo sigue los nombres ingleses y el estilo del contexto: paquetes en minúsculas, clases/tipos `PascalCase`, métodos y variables `camelCase`, constantes `UPPER_SNAKE_CASE`. Usa nombres de dominio y sufijos ya establecidos en el módulo; no renombres contratos públicos o columnas históricas sin una migración y justificación.
- Las pruebas Java viven en `src/test/java`, reflejan los paquetes de producción y usan nombres `*Test`; Python documental tiene pruebas bajo `brica-server/src/test/python`. Reutiliza fixtures existentes y prueba reglas en la capa apropiada. Las pruebas con Testcontainers no deben depender de una base local con datos.
- No añadas dependencias, cambies arquitectura, contratos o comportamiento de negocio sin autorización. Prefiere cambios pequeños y pruebas focalizadas; documenta las limitaciones que no puedas verificar.

## Base de datos y documentos

- PostgreSQL se valida con Hibernate (`ddl-auto: validate`); Flyway es dueño de la evolución del esquema. Añade una migración nueva con número/descripción únicos y conserva intactas todas las migraciones existentes, incluidas las que parezcan corregibles. No cambies el esquema solo para coincidir con el Diccionario inicial.
- Antes de una migración, compara el esquema desplegado esperado, entidades, repositorios y datos que podrían existir. Considera instalación nueva y actualización. No borres ni recrees una base o volumen para hacer pasar una prueba.
- Los archivos físicos se guardan en almacenamiento administrado por el servidor y se relacionan con sus metadatos. Mantén integridad, límites de tamaño, permisos y trazabilidad; no registres una operación como completada si su almacenamiento falló.
- En el flujo documental, conserva versiones/historial y no sobrescribas documentos históricos. Distingue configuración de paquetes/plantillas/campos, análisis de fuentes y generación definitiva. No afirmes que hay previsualización renderizada o exportación disponible si no está implementada en la rama.
- Los nombres de base de datos heredados pueden conservar idioma distinto al código Java. Respétalos salvo que un cambio de esquema aprobado justifique migrarlos.

## Flujo de trabajo

- Antes de una tarea no trivial, revisa rama, estado de Git y código relevante; presenta un plan y espera aprobación antes de editar.
- Trata cambios modificados y archivos sin seguimiento como trabajo del usuario: no los sobrescribas, reviertas ni incluyas incidentalmente. Limita las ediciones a lo autorizado.
- Si los requisitos, el ERS, el DAS, el Diccionario y la implementación dejan una regla ambigua, pregunta antes de decidir una regla de negocio. Explica brevemente las decisiones nuevas y por qué.
- Ejecuta pruebas y comprobaciones proporcionales al cambio. Informa los comandos ejecutados, resultados y verificaciones pendientes; no afirmes que una comprobación pasó si no se ejecutó.
- No hagas commit, push, abras PR ni ejecutes operaciones destructivas salvo solicitud expresa. Para tareas que correspondan, consulta `.opencode/commands/feature.md` y `.opencode/commands/review.md`.

### Contribuciones y flujos asistidos

- Consulta [`CONTRIBUTING.md`](CONTRIBUTING.md) antes de preparar issues, ramas, commits o PR: contiene sus formatos, trazabilidad, verificaciones y requisitos de autorización. No crees ni cambies de rama sin revisar el estado local y preservar el trabajo existente; la guía no autoriza por sí sola commits, push, PR, integración ni despliegue.
- Al elaborar una nueva especificación, usa [`specs/templates/spec-template.md`](specs/templates/spec-template.md) como base para `spec.md`; cuando correspondan un plan y seguimiento, usa también [`plan-template.md`](specs/templates/plan-template.md) y [`review-template.md`](specs/templates/review-template.md) para `plan.md` y `review.md`. Adapta su contenido al alcance, conserva las decisiones pendientes y registra evidencias y verificaciones reales sin presentar propuestas como aprobadas.
- En trabajo asociado a una spec existente, consulta sus documentos y actualiza el seguimiento cuando corresponda y esté dentro del alcance autorizado, preservando su historial y los cambios locales ajenos. No crees una spec para cada cambio menor ni sobrescribas plantillas o documentos existentes.
- Estas instrucciones son pasos del agente, no procesos de CI/CD que se ejecuten por sí solos. Comprueba la configuración de la rama y consulta `CONTRIBUTING.md` antes de afirmar que existe alguna automatización; informa las verificaciones manuales y pendientes.

## Seguridad y cuidado de datos

- Nunca añadas secretos, credenciales reales o `.env` al control de versiones. `.env.example` solo lleva valores de ejemplo.
- No consultes ni modifiques datos reales sin autorización explícita. Al inspeccionar datos, confirma que es la instancia local/de prueba y utiliza acceso de solo lectura si basta.
- No borres volúmenes Docker, archivos persistentes, migraciones, trabajo local ni historial Git para resolver fallos sin autorización expresa y un alcance confirmado.
