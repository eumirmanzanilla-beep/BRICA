# BRICA

> **Nota de integración:** esta documentación describe también el trabajo de la rama documental/funcional aún no integrado en `main`, incluida la extracción y la reorganización JavaFX. Consulta el código de tu rama antes de usar rutas o capacidades.

BRICA es un sistema interno para una constructora que centraliza clientes, obras, datos y archivos usados durante la operación. **La obra es el eje del sistema.** Un escritorio JavaFX consume la API REST; el servidor Spring Boot aplica las reglas, autorización y persistencia. El cliente no accede directamente a PostgreSQL.

## Requisitos y tecnologías

| Tecnología | Uso |
|---|---|
| Java 25 y Maven Wrapper | Compilar y ejecutar los módulos Java. |
| Spring Boot 4.1.1 | API REST y lógica del servidor. |
| JavaFX 25.0.2 y FXML | Aplicación de escritorio. |
| PostgreSQL 18, JPA/Hibernate y Flyway | Persistencia y evolución versionada del esquema. |
| Docker Compose | Entorno local de PostgreSQL y servidor. |
| Python 3 y dependencias fijadas | Procesamiento documental invocado por el servidor; requerido en desarrollo local para funciones/pruebas que lo utilizan. |
| JUnit, Spring Boot Test y Testcontainers | Pruebas Java; las pruebas de integración requieren Docker. |

Instala Git, JDK 25 y Docker Desktop. Para compilar no instales Maven globalmente: utiliza el Wrapper incluido. Python no es necesario para el recorrido instalado completamente en Docker; sí para ejecutar localmente las tareas Python del servidor. Consulta la [guía de instalación paso a paso](docs/installation-guide.md) para requisitos, cuentas, puertos y resolución de problemas.

## Inicio

Antes de instalar, confirma que tienes acceso al repositorio y cuál rama o versión debes probar. Clona el repositorio, entra en su raíz y comprueba el estado:

```bash
git clone https://github.com/eumirmanzanilla-beep/BRICA.git
cd BRICA
git switch <rama-publicada>
git status --short --branch
```

Sustituye `<rama-publicada>` por la rama entregada por el responsable. No todas las ramas locales están publicadas en GitHub. La guía de instalación actual contiene un ejemplo con `feat/23-documents-secure-reference-catalog`; si instalas otra rama, selecciona la versión correspondiente en lugar de asumir que ese nombre sigue vigente.

### Puesta en marcha local

El flujo de referencia usa PostgreSQL y servidor en Docker, y escritorio JavaFX en Windows. En PowerShell, desde la raíz:

```powershell
Copy-Item .env.example .env
# Editar .env y reemplazar la contraseña de ejemplo.
docker compose up -d --build brica-server
```

No publiques `.env`. Compose aplica las migraciones Flyway al iniciar; verifica el servidor en `http://localhost:8080/api/v1/health`. Una instalación nueva crea roles, no usuarios. Crea las cuentas interactivamente con `security-admin`, siguiendo [el paso de alta de cuentas](docs/installation-guide.md#6-crear-las-cuentas-del-escritorio).

En otra terminal PowerShell, abre el escritorio:

```powershell
$env:BRICA_ALLOW_HTTP="true" # Solo desarrollo local sin TLS.
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

`BRICA_ALLOW_HTTP=true` se permite exclusivamente para desarrollo local; el servidor exige HTTPS por defecto. Para ejecutar el backend fuera de Docker, configura `DB_URL`, `DB_USER` y `DB_PASSWORD` en el entorno (Maven no carga `.env`). Los comandos de Python local y el recorrido completo de instalación están en [`docs/installation-guide.md`](docs/installation-guide.md) y en la sección de desarrollo de este README.

### Recorrido funcional disponible

1. Inicia sesión con la cuenta creada; registra un cliente y una obra.
2. Desde la obra, administra información y adjunta archivos fuente autorizados. El servidor admite PDF, XLS/XLSX y DOCX sin macros, con límites definidos en la configuración.
3. El código de esta rama incluye un flujo de extracción para revisar candidatos/evidencia, guardar borradores y confirmar datos de obra, cliente o presupuesto por separado. Los resultados requieren revisión humana.
4. Configura campos de referencia, plantillas DOCX, versiones y paquetes asociados a cliente u obra; consulta los valores que resuelve una selección.
5. Se pueden consultar/descargar adjuntos de entrada. **No están disponibles la previsualización de documentos de salida, su generación definitiva ni su exportación.** La configuración o resolución de campos no produce un documento.

La extracción existe en el código, pero su verificación integral y aceptación siguen pendientes; consulta `specs/002/review.md` antes de depender de ese flujo. Tampoco se ha completado la aceptación manual Windows/Word ni la preparación de despliegue público.

## Organización del proyecto

```text
./                                      # Raíz del repositorio
├── README.md                           # Entrada al proyecto, instalación y estado funcional
├── AGENTS.md                           # Instrucciones operativas para desarrollo y agentes
├── CONTRIBUTING.md                     # Flujo de issues, commits, PR y estado de CI/CD
├── MEMORY.md                           # Diario de trabajo entre sesiones; no normativo
├── pom.xml                             # Reactor Maven y configuración compartida
├── mvnw, mvnw.cmd, .mvn/               # Maven Wrapper para Linux/WSL y Windows
├── compose.yml                         # PostgreSQL y servidor para desarrollo local
├── .env.example                        # Variables de ejemplo; no contiene credenciales reales
├── docs/                               # Instalación, constitución y fuentes de requisitos/diseño/datos
├── specs/                              # Especificaciones, planes, seguimiento y plantillas SDD
├── brica-server/                       # API, dominio, aplicación, persistencia e integraciones
│   └── src/main/resources/db/migration/ # Migraciones SQL versionadas de Flyway (hasta V18 en esta rama)
└── brica-client/                       # Escritorio JavaFX, clientes HTTP, FXML y estilos
```

El servidor está organizado por funcionalidad: `clients`, `works`, `contracts`, `budgets`, `documents`, `extraction` y `server`. Sus módulos suelen separar `api`, `application`, `domain` e `infrastructure`; sigue el patrón específico existente en vez de imponer una estructura nueva.

El cliente real se organiza principalmente en `api`, `config` y `presentation`. Sus vistas FXML y estilos están en `brica-client/src/main/resources/com/brica/client/presentation/`. Los README de módulo pueden presentar estructuras objetivo; no todas las clases que muestran existen necesariamente en esta rama.

### Integraciones principales

| Flujo | Responsabilidad |
|---|---|
| JavaFX → API REST | Sesión/CSRF y llamadas HTTP asíncronas. La UI no es frontera de seguridad. |
| API → aplicación/dominio | Validaciones, casos de uso, permisos y controles de concurrencia. |
| Servidor → PostgreSQL | JPA/Hibernate valida el esquema; Flyway crea/evoluciona migraciones. |
| Servidor → almacenamiento local | Bytes de archivos en almacenamiento del servidor; metadatos en PostgreSQL. Respaldar ambos juntos. |
| Servidor → Python | Análisis documental mediante proceso local configurado, no acceso a base desde Python. |
| Pruebas → Testcontainers | PostgreSQL desechable para integración; no usar datos de la instalación local. |

La configuración efectiva está en los POM, `compose.yml`, `.env.example` y `brica-server/src/main/resources/application.yml`. No hay workflows de GitHub Actions ni pipeline de despliegue automático en esta rama; consulta [`CONTRIBUTING.md`](CONTRIBUTING.md).

Variables principales:

| Variable | Dónde se usa | Propósito |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | `.env` / Compose | Base y acceso de PostgreSQL; no son las cuentas de BRICA. Puerto host por defecto `5433`. |
| `BRICA_SERVER_PORT` | `.env` / Compose | Puerto publicado del servidor; por defecto `8080`. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Proceso Maven local | Conexión del backend local; Maven no importa `.env`. |
| `BRICA_SERVER_URL` / `brica.server.url` | Cliente JavaFX | URL base de API; la propiedad JVM prevalece sobre la variable. |
| `BRICA_ALLOW_HTTP` | Servidor y cliente | Habilita HTTP solo en desarrollo local; el valor seguro por defecto lo mantiene desactivado. |
| `BRICA_DOCUMENT_PYTHON`, `BRICA_DOCUMENT_SCRIPT`, `BRICA_DOCUMENT_STORAGE` | Servidor | Python analizador y ruta de almacenamiento documental. En Docker se configuran dentro de la imagen. |

El procesamiento de extracción tiene además configuración `BRICA_EXTRACTION_*` en `application.yml`; en particular, revisa el requisito de aislamiento (`BRICA_EXTRACTION_CGROUP_ROOT`) antes de habilitarlo en un entorno distinto al local. No copies valores o secretos de una instalación personal.

## Archivos relevantes

| Ruta | Por qué es relevante |
|---|---|
| `pom.xml`, `brica-server/pom.xml`, `brica-client/pom.xml` | Módulos Maven, versiones, dependencias y plugins de ejecución. |
| `compose.yml`, `.env.example` | Topología local y nombres de variables de entorno de ejemplo. Maven no carga `.env`. |
| `brica-server/src/main/resources/application.yml` | Límites, seguridad, conexión, almacenamiento y configuración de procesos del servidor. |
| `brica-server/src/main/resources/db/migration/` | Historial del esquema; crea migraciones nuevas, no edites las aplicadas. |
| `brica-server/src/main/java/com/brica/` | API, casos de uso, reglas de dominio y adaptadores, organizados por funcionalidad. |
| `brica-server/src/main/python/requirements.txt` | Versiones Python fijadas para los analizadores. |
| `brica-server/src/test/` | Pruebas Java/Python y fixtures de integración/documentos. |
| `brica-client/src/main/java/com/brica/client/` | Clientes HTTP, configuración y presentación JavaFX. |
| `brica-client/src/main/resources/` | FXML, CSS y otros recursos del escritorio. |
| `AGENTS.md` | Convenciones, fuentes de verdad, seguridad y límites de trabajo del repositorio. |
| `CONTRIBUTING.md` | Cómo describir issues, ramas, commits y PR; CI/CD actual y recomendaciones futuras. |
| `brica-client/README.md`, `brica-server/README.md` | Mapas de módulos; contrastar sus estructuras objetivo con el código real. |
| `specs/README.md`, `specs/templates/` | Flujo SDD y plantillas que integran `spec.md`, `plan.md` y `review.md`. |
| `specs/001/` | Configuración documental: catálogo, plantillas, versiones, paquetes, plan y aceptación. |
| `specs/002/` | Extracción de fuentes: alcance, plan, decisiones, pruebas experimentales y seguimiento. |
| `docs/ERS_BRICA.docx`, `docs/DAS_BRICA.docx`, `docs/DiccionarioDatos_BRICA.docx` | Requisitos y modelo iniciales; no sustituyen el código ni el esquema Flyway vigente. |
| `docs/installation-guide.md`, `docs/constitution.md` | Guía reproducible de instalación y principios del proyecto. |
| `MEMORY.md` | Contexto temporal entre sesiones; puede quedar desactualizado y no es fuente normativa. |

## Flujo de construcción (SDD)

BRICA usa **Specification-Driven Development (SDD)** para hacer explícitos el comportamiento deseado, el plan de cambio y la evidencia. Los tres documentos tienen propósitos distintos y se mantienen vinculados:

1. **`spec.md` — qué y por qué.** Antes de implementar, delimita resultado, actores, alcance/exclusiones, reglas, requisitos verificables, criterios de aceptación, dependencias y dudas. Los requisitos y criterios deben tener identificadores estables.
2. **`plan.md` — cómo y en qué orden.** Diagnostica el código/configuración existentes, propone decisiones/rutas, fases, migraciones, riesgos y pruebas. Diferencia propuestas de decisiones aprobadas; no autoriza por sí solo cambios de arquitectura, dependencias, datos o despliegue.
3. **Aprobación e implementación por bloques.** Revisa el estado Git, presenta plan y espera aprobación antes de trabajo no trivial. Implementa unidades acotadas; actualiza el plan si cambia el alcance autorizado.
4. **`review.md` — qué ocurrió realmente.** Registra cronológicamente archivos tocados, decisiones, pruebas y resultados exactos, tareas/criterios cerrados o pendientes, aceptación manual y bloqueos. No convierte implementación en aceptación ni elimina los fallos históricos.
5. **Cierre o continuación.** Contrasta criterios de la spec con evidencia de review; conserva pendientes explícitos y coordina el README cuando cambie el alcance funcional visible.

La guía y las plantillas copiables están en [`specs/README.md`](specs/README.md) y [`specs/templates/`](specs/templates/). Para una funcionalidad nueva, crea `specs/NNN-nombre/spec.md`, `plan.md` y `review.md` en el mismo directorio; no sobrescribas una spec existente sin revisar su historial.

## Convenciones de construcción

- **Flujo GitHub:** busca/define issue, rama por resultado, commits atómicos y PR revisable. Usa los formatos y ejemplos de [`CONTRIBUTING.md`](CONTRIBUTING.md). En esta rama no existe CI/CD automatizado: las verificaciones deben informarse manualmente.
- **Código:** nombres nuevos en inglés; paquetes en minúsculas, clases `PascalCase`, métodos/variables `camelCase`, constantes `UPPER_SNAKE_CASE`. Respeta los límites del módulo y separa solicitudes/respuestas HTTP de entidades persistidas.
- **Arquitectura:** el servidor valida datos y permisos. Mantén el flujo existente `api → application → domain`; infraestructura implementa persistencia e integraciones. JavaFX consume la API, no PostgreSQL, y no bloquea el hilo UI.
- **Concurrencia e historial:** conserva ETag/`If-Match`, idempotencia, borrado lógico y versiones donde existan. Los documentos históricos no se sobrescriben silenciosamente.
- **Base de datos:** usa Flyway para evolucionar PostgreSQL. Agrega una migración nueva con número único; no edites migraciones ya aplicadas ni borres volúmenes para resolver problemas.
- **Pruebas:** Java en `src/test/java`, paquetes equivalentes y nombres `*Test`; Python documental en `brica-server/src/test/python`. Usa datos sintéticos/autorizados. Testcontainers requiere Docker.
- **Verificación habitual** desde la raíz:

  ```bash
  ./mvnw test
  ./mvnw verify
  git diff --check
  ```

  En PowerShell usa `./mvnw.cmd`. JavaFX también requiere validación visual en Windows; compilar no sustituye ese recorrido.
- **Seguridad:** nunca subas `.env`, credenciales, documentos reales o datos sensibles. HTTP con `BRICA_ALLOW_HTTP=true` solo para desarrollo local; no habilita un despliegue seguro.
- **Cambios asistidos:** revisa rama/estado, presenta plan para tareas no triviales, preserva trabajo local y reporta con precisión qué verificaste. No hagas commit, push, PR ni operaciones destructivas sin autorización.

Para convenciones detalladas consulta [`AGENTS.md`](AGENTS.md), la [constitución](docs/constitution.md) y la [guía de contribución](CONTRIBUTING.md).

## Estado actual del programa

El programa avanza por funcionalidades integrables; esta vista resume el recorrido sin fijar una meta final:

1. **Fundación:** aplicación Java multimódulo, servidor REST y cliente JavaFX; persistencia PostgreSQL/Flyway, validación y pruebas automatizadas.
2. **Operación central:** clientes y obras con consulta/edición, eliminación lógica y control de concurrencia. La obra agrupa datos y adjuntos.
3. **Acceso y protección:** sesión de servidor, CSRF, permisos por operación y administración local de usuarios/roles.
4. **Configuración documental:** campos fijos/adicionales, análisis y versionado de plantillas DOCX, paquetes por cliente/obra y resolución de valores para selecciones.
5. **Fuentes y presupuestos:** adjuntos persistentes y código de extracción/revisión humana; presupuestos con revisiones inmutables. La implementación de extracción está presente, pero su seguimiento/aceptación no está alineado completamente con el código y aún requiere validación.
6. **Salida documental:** la generación, previsualización renderizada e importación/exportación de documentos finales siguen fuera del alcance funcional disponible. Es una capacidad futura separada, no un paso que pueda completarse hoy desde la instalación.

Para el detalle vigente por funcionalidad, consulta [`specs/001/review.md`](specs/001/review.md) y [`specs/002/review.md`](specs/002/review.md). La aceptación manual de Windows/Word, la plantilla operativa conjunta y la preparación operativa de HTTPS, respaldos y retención siguen pendientes. No se debe inferir que una función está aceptada solo porque existan clases, endpoints o pruebas.
