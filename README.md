# BRICA

Sistema interno para una constructora que centraliza clientes, obras, contratos y documentación. La obra es el eje del negocio. El escritorio JavaFX consume una API REST; las reglas y la persistencia residen en el servidor.

## Estado actual

El repositorio ya contiene:

- API de salud: `GET /api/v1/health`.
- Clientes: registro, listado paginado, consulta, actualización y eliminación lógica.
- Obras: registro, listado paginado, consulta, corrección de información general y eliminación lógica. La corrección utiliza `ETag` / `If-Match` para detectar cambios concurrentes.
- Contratos: registro mediante `POST /api/v1/contracts`, asociado a una obra, con datos JSON almacenados en PostgreSQL. No incluye carga ni extracción de PDF.
- Persistencia JPA, validaciones del servidor, errores `ProblemDetail` y migraciones Flyway V1–V6.
- Autenticación por sesión de servidor, BCrypt coste 10, CSRF y autorización por permisos vigentes en los casos de uso de negocio. Inicio/cierre de sesión en JavaFX y administración local inicial de cuentas/roles.
- API de catálogo documental: atributos aprobados, registro, listado, renombrado y resolución de nombres de obra/cliente. Las pantallas documentales todavía no están implementadas.
- Escritorio con panel de inicio, pantallas de clientes y obras y comprobación de conexión. La sección Contratos todavía muestra un contenido provisional; no existe su formulario FXML.
- Pruebas unitarias, de API y de persistencia/HTTP con PostgreSQL en Testcontainers.

**Todavía no implementado:** pantallas del catálogo de campos de referencia, almacenamiento documental, plantillas versionadas, paquetes y motor de generación. La configuración documental completa aún no está aceptada. La validación visual del nuevo acceso JavaFX sigue pendiente en Windows.

## Stack y estructura

Java 25, Maven Wrapper, Spring Boot 4.1.1, JavaFX 25.0.2 y PostgreSQL 18.

| Ruta | Contenido |
|---|---|
| `brica-server/` | Servidor organizado por funcionalidad: API, aplicación, dominio e infraestructura. |
| `brica-client/` | Cliente JavaFX, recursos FXML y clientes HTTP asíncronos. |
| `brica-server/src/main/resources/db/migration/` | Migraciones SQL versionadas; no modificar las ya aplicadas. |
| `specs/001/` | Configuración documental: campos, plantillas, versiones y paquetes. |
| `specs/002/` | Motor documental: extracción de PDF y generación. |
| `docs/constitution.md` (previsto, aún ausente) | Principios de BRICA. |

Las versiones y la configuración efectiva están en los POM, `compose.yml` y `brica-server/src/main/resources/application.yml`.

## Desarrollo local

Requiere JDK 25 y Docker activo para PostgreSQL y las pruebas de integración. Usar WSL para desarrollo y pruebas; validar visualmente JavaFX en Windows. Ejecutar los comandos desde la raíz.

### 1. Preparar PostgreSQL

Crear `.env` a partir de `.env.example` y reemplazar la contraseña de ejemplo. No versionar credenciales. Compose utiliza estas variables; Maven **no carga `.env` automáticamente**.

```bash
docker compose up -d postgres
```

Por defecto, Compose publica PostgreSQL en el puerto `5433` del host y conserva los datos en un volumen. No eliminar el volumen para resolver errores de migración.

### 2. Iniciar el backend desde WSL

Definir `DB_URL`, `DB_USER` y `DB_PASSWORD` en el entorno del proceso, usando los valores configurados para PostgreSQL. Ejemplo sin credenciales reales:

```bash
export DB_URL='jdbc:postgresql://localhost:5433/brica'
export DB_USER='brica'
read -rsp 'Contraseña de PostgreSQL: ' DB_PASSWORD
export DB_PASSWORD
export BRICA_ALLOW_HTTP=true # Exclusivamente desarrollo local sin TLS.
./mvnw -pl brica-server spring-boot:run
```

Al arrancar, Flyway aplica las migraciones y Hibernate valida el esquema. Comprobar `http://localhost:8080/api/v1/health`; devuelve `{"status":"UP","service":"brica-server"}`.

Como alternativa al backend local, Compose dispone de un servicio de servidor con construcción multietapa:

```bash
docker compose up -d --build brica-server
```

No ejecutar ambos servidores simultáneamente en el mismo puerto. El servidor en Compose recibe las variables de conexión desde su configuración y se conecta al servicio `postgres`.

Las operaciones de negocio requieren HTTPS por defecto; HTTP se rechaza salvo `BRICA_ALLOW_HTTP=true`, reservado para desarrollo local. Para Compose, configurar ese valor en su entorno o `.env` únicamente en desarrollo. No confiar en cabeceras reenviadas de clientes para fingir HTTPS; el despliegue TLS/reverse proxy debe configurarse expresamente antes de uso operativo.

### Crear roles y cuentas iniciales

No existen usuarios ni contraseñas de acceso por defecto. Tras compilar, desde una terminal interactiva del servidor, con `DB_*` configuradas:

```bash
java -jar brica-server/target/brica-server-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --spring.profiles.active=security-admin
```

Este perfil **no abre un servidor web**. El acceso administrativo depende de los permisos del sistema operativo sobre el servidor y sus credenciales de base. Usar `role` para crear un rol y anotar su ID; después `user` para asignarlo a una cuenta. `permissions`, `user-state`, `role-state` y `password` permiten gestionar permisos, estados y restablecer contraseñas; `exit` termina. Las contraseñas se solicitan sin eco, nunca como argumentos ni en migraciones.

Claves disponibles: `CLIENTS_READ`, `CLIENTS_WRITE`, `WORKS_READ`, `WORKS_WRITE`, `CONTRACTS_READ`, `CONTRACTS_WRITE`, `DOCUMENT_VALUES_READ`, `DOCUMENT_CONFIG_ADMIN`. Se asignan por separado: administración documental no concede consulta de valores, ni un nivel jerárquico alto concede permisos implícitos. `CONTRACTS_READ` no crea una consulta de contratos aún inexistente.

Los identificadores de usuario se comparan exactamente, sin recortar ni convertir mayúsculas. Contraseñas: al menos 8 caracteres, una mayúscula y un número, máximo técnico 72 bytes UTF-8, sin truncamiento. Usuarios/roles desactivados y permisos retirados dejan de autorizar operaciones aunque exista una sesión previa.

### 3. Iniciar el escritorio desde Windows

Desde PowerShell, con JDK 25 disponible:

```powershell
$env:BRICA_ALLOW_HTTP="true" # Solo desarrollo local sin TLS.
.\mvnw.cmd -pl brica-client javafx:run
```

El cliente utiliza `http://localhost:8080`. Para otro destino:

```powershell
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

También admite la propiedad de JVM `brica.server.url`, que tiene precedencia sobre la variable de entorno. Si el servidor corre en otro host, ajustar la URL al destino accesible desde Windows.

La sesión no caduca por tiempo, según la política aprobada. Cookie y CSRF permanecen únicamente en memoria del escritorio; cerrar sesión los descarta e invalida la sesión del servidor. Cerrar la aplicación intenta logout sin bloquear JavaFX. Un cierre abrupto puede dejar una sesión remota huérfana hasta reiniciar el servidor; no se implementa «recordar acceso» ni se persisten cookies/contraseñas.

## Verificación

Desde WSL, con Docker activo:

```bash
./mvnw test
./mvnw clean verify
git diff --check
```

Las pruebas de integración usan PostgreSQL 18 en Testcontainers; no necesitan apuntar a la base de desarrollo. La ejecución manual del backend sí requiere sus variables de conexión. Compilar no sustituye abrir y verificar las pantallas en Windows. Registrar por separado las comprobaciones ejecutadas y las pendientes.

## Próxima entrega: configuración documental

La [Spec 001](specs/001/spec.md) define el catálogo de campos, la consulta de valores por obra, el análisis de DOCX, las versiones de plantilla y los paquetes documentales. Su [plan de trabajo](specs/001/plan.md) detalla fases, archivos, funciones, validaciones y criterios de salida. La aceptación completa requiere permisos reales comprobados en el servidor.

El [registro de avance](specs/001/review.md) enumera las tareas abiertas/cerradas, las decisiones aprobadas y la evidencia de pruebas. F0/F1 tienen prioridad antes de F3. Hay [fixtures técnicos DOCX](specs/001/fixtures/README.md), no formatos operativos aprobados; la nueva plantilla se diseñará conjuntamente sobre otro documento. Fechas, importes y el flujo completo siguen pendientes.

La [Spec 002](specs/002/spec.md) corresponde al motor documental posterior. No confundir la consulta de valores de la configuración documental con una previsualización renderizada o una generación definitiva.

## Reglas de contribución

Consultar [AGENTS.md](AGENTS.md). La constitución está prevista en `docs/constitution.md`, todavía ausente. Presentar y aprobar el plan antes de cambios no triviales, conservar el trabajo local y consultar antes de incorporar dependencias o cambiar la arquitectura. No hacer commits, push ni abrir PR sin solicitud expresa.
