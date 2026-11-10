# BRICA SYSTEM

Sistema interno para una constructora que centraliza obras, clientes, contratos y documentación. Usa un cliente de escritorio conectado a un servidor central. La Obra es el núcleo del negocio.

## Stack

- Java 25 y Maven multimódulo con Wrapper.
- Spring Boot 4.1.1 en servidor y JavaFX 25.0.2 en escritorio.
- PostgreSQL 18, JPA/Hibernate y migraciones Flyway.
- JUnit Jupiter, Spring Boot Test y Testcontainers con Docker.

## Comandos

Trabaja desde la raíz del repositorio. Usa WSL para desarrollo y pruebas, y Windows para validar la interfaz JavaFX. Configura `.env` para Compose y `DB_*` en el entorno del backend local. Maven no carga `.env` automáticamente.

- `docker compose up -d postgres` — Levanta la base de desarrollo.
- `./mvnw -pl brica-server spring-boot:run` — Arranca el backend desde WSL.
- `./mvnw test` — Ejecuta las pruebas antes de cada commit. Las pruebas de integración requieren Docker activo.
- `./mvnw clean verify` — Compila, empaqueta y verifica ambos módulos.
- `git diff --check` — Comprueba problemas de espacios antes del PR.
- `.\mvnw.cmd -pl brica-client javafx:run` — Arranca el cliente desde PowerShell para revisar la interfaz.

## Estructura del proyecto

- `brica-server/` — API REST, aplicación, dominio e infraestructura, organizados por funcionalidad.
- `brica-client/` — Interfaz JavaFX, FXML y consumo de la API.
- `brica-server/src/main/resources/db/migration/` — Migraciones SQL versionadas.

## Convenciones

- Sigue los nombres en inglés del código actual. Usa `PascalCase` para clases y `camelCase` para métodos y variables.
- El backend sigue `api` → `application` → `domain`. Infraestructura implementa los puertos y el cliente accede a los datos mediante la API.
- Valida entradas y reglas en el servidor. Usa las excepciones existentes y `ApiExceptionHandler` para errores HTTP.
- Evita bloquear JavaFX. Actualiza la interfaz desde su hilo y conserva el control de concurrencia existente.
- Coloca las pruebas en `src/test/java`, con paquetes equivalentes y nombres `*Test`. Separa responsabilidades antes de agrandar controladores.

## Límites

- Consulta antes de añadir dependencias o cambiar la arquitectura.
- Conserva las migraciones ya aplicadas. Agrega una nueva para evolucionar el esquema.
- Mantén los secretos, `.env` y las configuraciones personales con credenciales fuera de Git. Usa solo valores de ejemplo en `.env.example`.
- Ejecuta `git reset --hard`, `git clean`, force-push o borrados de datos y volúmenes solo ante una petición explícita.

## Flujo de trabajo

- Antes de una tarea no trivial, revisa rama, estado y código actual. Presenta un plan y espera mi OK antes de editar. Si el alcance cambia de forma sustancial, confirma el plan actualizado.
- Pide aclaración cuando la información disponible no permita resolver una duda de alcance, regla de negocio o decisión técnica. Explica brevemente el porqué de las decisiones nuevas.
- Haz cambios acotados y conserva el trabajo local ajeno a la tarea. Entrega rutas e imports coherentes e informa qué comprobaste y qué quedó pendiente.
- En OpenCode, usa `/feature` para planear, implementar y verificar cambios, y `/review` para revisar el diff en modo de solo lectura. Haz commits, push o abre PR solo cuando se solicite.

## Referencias del proyecto

- Consulta la constitución, prevista en `docs/constitution.md`, antes de tomar decisiones que afecten principios de BRICA. Para un cambio funcional, ubica y lee su spec antes de planear.
- La **Spec 002** cubre el motor documental, desde la extracción del PDF hasta la generación. La **Spec 001** cubre plantillas, versiones, paquetes y campos de referencia. Para conectar ambos flujos, revisa las decisiones vigentes de la Spec 001 sobre documentos estáticos y valores como texto. Revisa también su dependencia de permisos antes de dar el módulo por aceptado.
- Verifica la configuración efectiva en `pom.xml`, `compose.yml` y `brica-server/src/main/resources/application.yml`. Consulta el README y los ADR aplicables. Si una referencia discrepa del código, señala la diferencia y consulta.
- Usa el ERS, el DAS y el Diccionario de Datos para resolver requisitos, diseño y datos. Si falta una fuente necesaria, solicítala antes de decidir.

## Skills y MCP

- Consulta las skills pertinentes en `.opencode/skills/`. Usa `brica-refactor` para reacomodos de código, `brica-postgres` para persistencia y migraciones, y `brica-document-generation` para el flujo documental. Si están disponibles, las skills de REST y JPA de Spring Boot 4 complementan las convenciones de BRICA.
- Usa Context7 para consultar documentación de las versiones que usa el proyecto y GitHub MCP para leer issues, PR y referencias del repositorio. Si DBHub está disponible, úsalo para inspección de solo lectura tras confirmar que apunta al PostgreSQL 18 de BRICA en Docker Desktop.
