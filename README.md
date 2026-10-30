
# BRICA

Sistema de gestión administrativa y documental para proyectos
de construcción.

## Estado del desarrollo

BRICA se encuentra en desarrollo. Actualmente dispone de una
arquitectura cliente-servidor y de las primeras funcionalidades
para administrar Clientes, Obras y Contratos.

### Backend

- API REST desarrollada con Java 25 y Spring Boot 4.1.1.
- Persistencia mediante PostgreSQL y JPA/Hibernate.
- Migraciones de base de datos con Flyway.
- Registro, consulta, actualización y eliminación lógica de Clientes.
- Registro, consulta, corrección y eliminación lógica de Obras.
- Registro de Contratos.
- Validación de información y respuestas HTTP estructuradas.
- Control de concurrencia en correcciones de Obras mediante
  ETag e If-Match.

### Cliente de escritorio

- Aplicación JavaFX 25.
- Navegación entre módulos.
- Panel de inicio.
- Gestión de Clientes.
- Gestión de Obras.
- Comunicación asíncrona con la API REST.

La interfaz de Contratos y las funcionalidades de generación
documental permanecen pendientes.

## Arquitectura

BRICA utiliza una arquitectura cliente-servidor:

JavaFX → REST/HTTP → Spring Boot → JPA → PostgreSQL

El cliente de escritorio no accede directamente a PostgreSQL.

## Estructura del repositorio

- `brica-client/`: aplicación JavaFX.
- `brica-server/`: backend Spring Boot.
- `docs/adr/`: decisiones de arquitectura.
- `compose.yml`: servicios Docker de desarrollo.
- `pom.xml`: configuración Maven multimódulo.

## Requisitos

- JDK 25.
- Maven Wrapper incluido en el proyecto.
- PostgreSQL 18 para el entorno Docker de desarrollo.
- Docker, opcional para el uso de PostgreSQL en desarrollo.

## Configuración del servidor

El backend utiliza las siguientes variables de entorno:

- `DB_URL`: URL JDBC de PostgreSQL.
- `DB_USER`: usuario de PostgreSQL.
- `DB_PASSWORD`: contraseña de PostgreSQL.

La conexión debe apuntar a una instancia disponible de PostgreSQL.

El archivo `.env.example` sirve como referencia para la
configuración de Docker. No deben incorporarse contraseñas reales
al repositorio.

## Compilación

Desde la raíz del proyecto en Windows:

    .\mvnw.cmd -pl brica-client compile

Para compilar todos los módulos:

    .\mvnw.cmd compile

## Ejecutar el servidor

Con PostgreSQL disponible y las variables de entorno configuradas:

    .\mvnw.cmd -pl brica-server spring-boot:run

Endpoint de comprobación:

    http://localhost:8080/api/v1/health

## Ejecutar el cliente

En otra terminal:

    .\mvnw.cmd -pl brica-client javafx:run

La URL del servidor puede configurarse mediante la variable
`BRICA_SERVER_URL` o la propiedad `brica.server.url`.

## Flujo de desarrollo

- `main`: rama principal de integración.
- `feat/*`: nuevas funcionalidades.
- `fix/*`: corrección de errores.
- `refactor/*`: mejoras internas sin modificar comportamiento.
- `chore/*`: mantenimiento y configuración.
- `docs/*`: documentación.

Los cambios destinados a `main` deben proponerse mediante
Pull Requests.

Se utilizan mensajes de commit siguiendo la convención
Conventional Commits.

## Documentación

Las decisiones arquitectónicas están registradas en `docs/adr/`.

El desarrollo debe conservar la separación entre presentación,
lógica de aplicación, dominio e infraestructura.

## Funcionalidades pendientes

- Interfaz de Contratos.
- Gestión documental.
- Plantillas y paquetes documentales.
- Generación de documentos.
- Funcionalidades administrativas adicionales.
- Preparación para distribución e instalación.
