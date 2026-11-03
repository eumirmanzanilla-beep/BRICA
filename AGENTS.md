# BRICA SYSTEM

Sistema interno para una constructora que centraliza obras, clientes, contratos y documentación. Usa un cliente de escritorio conectado a un servidor central; la Obra es el núcleo del negocio

## Stack
- Lenguaje: Java 25; Maven multimódulo con Wrapper
- Framework:    Spring Boot 4.1.1 en servidor; JavaFX 25.0.2 en escritorio   
- Base de datos: PostgreSQL 18, JPA/Hibernate y migraciones Flyway
- Tests: JUnit Jupiter, Spring Boot Test y Testcontainers con Docker

## Comandos
Desde la raíz, en PowerShell configura `.env` para Compose y `DB_*` en el entorno del backend local. Maven no carga `.env` automáticamente
- `docker compose up -d postgres`   - Levanta la Base de Datos local
- `.\mvnw.cmd -pl brica-server spring-boot:run`   - Arranca en backend
- `.\mvnw.cmd -pl brica-client javafx:run`  - Arranca el escritorio en otra terminal
- `.\mvnw.cmd test` - Ejecuta todos los test; deben pasar antes de cada commit. Integración requiere Docker activo
- `git diff --check`    -Comprueba espacios antes del PR; no sustituye un linter, que aún no está configurado
- `.\mvnw.cmd clean verify` -Compila, empaqueta y verifica ambos módulos.

## Estructura del proyecto
- `brica-server/`  - API REST, aplicación, dominio y persistencia, organizados por funcionalidad.
- `brica-client/`  - Interfaz JavaFX, FXML y consumo de la API.
- `brica-server/src/main/resources/db/migration/`  - Migraciones SQL versionadas.
- `docs/adr/`   - Decisiones arquitectónicas existentes.

## Convenciones
- Sigue los nombres en inglés del código actual: `PascalCase` para clases, `camelCase` para métodos y variables.
- Backend: `api` → ``application`` → ``domain``; infraestructura implementa los puertos. El cliente accede solo a la API.
- Valida entradas y reglas en el servidor; usa las excepciones existentes y ``ApiExceptionHandler`` para errores HTTP.
- Evita bloquear JavaFX; actualiza la interfaz desde su hilo y conserva el control de concurrencia existente.
- Tests en ``src/test/java``, con paquetes equivalentes y nombres ``*Test``; separa responsabilidades antes de agrandar controladores.

## No hagas
- No añadas dependencias ni cambies la arquitectura sin consultarlo.
- No edites migraciones ya aplicadas: crea una nueva.
- No subas secretos ni ``.env``; ``.env.example`` debe contener solo valores de ejemplo.
- No ejecutes`` reset --hard``, ``clean``, ``force-push`` ni borres datos o volúmenes sin petición explícita

## Flujo de trabajo
- Antes de una tarea no trivial, revisa rama, estado y código actual; propón un plan y espera mi OK.
- Si no estás seguro al 90 %, pregunta; no inventes. Explica brevemente el porqué de decisiones nuevas.
- Haz cambios acotados y entrega rutas e imports coherentes; informa qué comprobaste y qué quedó pendiente.

## Documentación
- Consulta ``pom.xml``, ``compose.yml`` y ``brica-server/src/main/resources/application.yml`` para configuración vigente.
- Lee ``README.md`` y los ``ADR`` cuando apliquen; pueden ser históricos. Si contradicen el código, señala la diferencia y consulta.
- Para requisitos y datos, consulta ``ERS``, ``DAS`` y ``Diccionario de Datos de BRICA`` cuando estén disponibles; solicítalos si son necesarios y faltan.