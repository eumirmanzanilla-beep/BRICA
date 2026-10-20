# BRICA — Iteración 0

Primer esqueleto ejecutable del sistema BRICA.

## Objetivo de esta iteración

Comprobar la infraestructura mínima cliente-servidor antes de implementar reglas de negocio:

1. El backend Spring Boot arranca.
2. `GET /api/v1/health` responde `200 OK`.
3. El cliente JavaFX arranca.
4. El cliente puede comprobar la conexión con el servidor.
5. La prueba automática del endpoint de salud pasa correctamente.

No se incluye todavía PostgreSQL, JPA, Flyway, seguridad, Cliente ni Obra.

## Requisitos

- JDK 25
- Maven 3.10.0 o Maven Wrapper

## Crear Maven Wrapper

Desde la raíz, una sola vez:

```bash
mvn wrapper:wrapper -Dmaven=3.10.0
```

Después puede usarse `mvnw`/`mvnw.cmd`.

## Compilar todo

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS:

```bash
./mvnw clean verify
```

## Ejecutar servidor

```powershell
.\mvnw.cmd -pl brica-server spring-boot:run
```

Después abrir:

`http://localhost:8080/api/v1/health`

Respuesta esperada:

```json
{"status":"UP","service":"brica-server"}
```

## Ejecutar cliente

En otra terminal, dejando el servidor activo:

```powershell
.\mvnw.cmd -pl brica-client javafx:run
```

Presionar **Comprobar servidor**.

Resultado esperado: **Servidor BRICA disponible**.

## Configurar otra URL de servidor

Por variable de entorno:

```powershell
$env:BRICA_SERVER_URL="https://servidor-brica"
```

O como propiedad Java:

```text
-Dbrica.server.url=https://servidor-brica
```

## Siguiente iteración

Agregar PostgreSQL + Flyway y construir la primera vertical funcional: `Cliente` + `Obra`.
