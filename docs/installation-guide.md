# BRICA: instalación desde cero

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
Esta guía prepara una instalación **local de desarrollo o pruebas en Windows**:

- PostgreSQL y servidor dentro de Docker.
- Escritorio JavaFX ejecutado desde PowerShell.
- Datos y documentos conservados en volúmenes persistentes.

No es un despliegue público. HTTP se habilita únicamente para uso local. El servidor en Docker ya incluye Java y el entorno Python documental; no necesitas instalar PostgreSQL, Maven, JavaFX ni Python por separado para este recorrido. El escritorio sí requiere JDK 25.

## 1. Instalar los requisitos

Instala **Git**, **JDK 25** (no solamente JRE) y **Docker Desktop** con el motor WSL 2 habilitado. Abre Docker Desktop y espera a que esté listo.

Abre PowerShell y comprueba:

```powershell
git --version
java -version
javac -version
docker version
docker compose version
```

`java` y `javac` deben indicar 25. Docker debe mostrar información del cliente y del servidor. Si acabas de instalar las herramientas, vuelve a abrir la terminal. Revisa `JAVA_HOME` si Maven utiliza otro JDK.

## 2. Descargar el proyecto

Desde la carpeta donde quieras guardar el código:

```powershell
git clone https://github.com/eumirmanzanilla-beep/BRICA.git
cd BRICA
git switch feat/23-documents-secure-reference-catalog
git status --short --branch
```

Si el repositorio es privado, tu cuenta de GitHub necesita acceso. Esta guía corresponde a la rama `feat/23-documents-secure-reference-catalog`; después de integrarla, puede utilizarse `main` actualizado con esos cambios.

Ejecuta los comandos siguientes desde la raíz del proyecto, donde están `compose.yml` y `mvnw.cmd`. Si el proyecto ya existe en tu equipo, no sobrescribas cambios ni vuelvas a crear `.env` sin revisar primero su contenido.

## 3. Configurar PostgreSQL y el servidor

Para una copia nueva:

```powershell
Copy-Item .env.example .env
notepad .env
```

Configura:

```dotenv
POSTGRES_DB=brica
POSTGRES_USER=brica
POSTGRES_PASSWORD=REEMPLAZAR_POR_UNA_CLAVE_PRIVADA
POSTGRES_PORT=5433

BRICA_SERVER_PORT=8080
BRICA_ALLOW_HTTP=true
```

**Reemplaza la contraseña de ejemplo antes de continuar.** Usa una clave propia; no publiques `.env` ni lo incluyas en capturas.

| Variable | Valor de esta guía | Función |
|---|---|---|
| `POSTGRES_DB` | `brica` | Nombre de la base. |
| `POSTGRES_USER` | `brica` | Usuario técnico de PostgreSQL. |
| `POSTGRES_PASSWORD` | Tu contraseña privada | Acceso técnico a PostgreSQL. |
| `POSTGRES_PORT` | `5433` | Puerto de PostgreSQL en tu equipo. |
| `BRICA_SERVER_PORT` | `8080` | Puerto de la API en tu equipo. |
| `BRICA_ALLOW_HTTP` | `true` | Autoriza HTTP para esta instalación local. |

Compose carga `.env`; Maven **no lo carga automáticamente**. El usuario técnico `brica` no es una cuenta del escritorio. Cambiar `POSTGRES_PASSWORD` en `.env` no cambia la contraseña de una base ya inicializada: no borres su volumen para corregir una discrepancia.

## 4. Construir e iniciar

```powershell
docker compose up -d --build brica-server
```

La primera ejecución descarga imágenes y dependencias, construye el servidor, inicia PostgreSQL, aplica las migraciones Flyway y valida el esquema. Puede tardar varios minutos.

Comprueba los servicios y registros:

```powershell
docker compose ps
docker compose logs --tail=100 brica-server
```

Para seguir los registros en tiempo real:

```powershell
docker compose logs -f brica-server
```

`Ctrl+C` deja de mostrar los registros, pero no detiene los servicios.

Comprueba la API:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Debe responder con los valores:

```json
{"status":"UP","service":"brica-server"}
```

No ejecutes simultáneamente otro servidor con Maven en el puerto 8080. En esta guía el backend ya está dentro de Docker.

## 5. Consultar los roles iniciales

Las migraciones preparan roles, **no cuentas de acceso**. Consulta los identificadores reales:

```powershell
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id_role, name, hierarchy_level FROM roles ORDER BY hierarchy_level DESC;"'
```

Si tu versión de PowerShell altera las comillas del comando anterior, abre una consola del contenedor:

```powershell
docker compose exec postgres sh
```

Dentro de ella ejecuta:

```sh
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

En PostgreSQL:

```sql
SELECT id_role, name, hierarchy_level FROM roles ORDER BY hierarchy_level DESC;
```

Escribe `\q` para salir de PostgreSQL y `exit` para salir del contenedor.

| Rol | Nivel | Permisos actuales |
|---|---:|---|
| Gerencial | 4 | Ocho permisos. |
| Administrativo | 3 | Los mismos ocho permisos. |
| Practicante | 2 | Cuatro permisos de lectura. |
| Invitado | 1 | Cuatro permisos de lectura. |

Anota el `id_role` de Gerencial y Administrativo. **El nivel no es el identificador**: no supongas sus IDs ni crees nuevamente los roles.

Los ocho permisos son `CLIENTS_READ`, `CLIENTS_WRITE`, `WORKS_READ`, `WORKS_WRITE`, `CONTRACTS_READ`, `CONTRACTS_WRITE`, `DOCUMENT_VALUES_READ` y `DOCUMENT_CONFIG_ADMIN`. Los roles de lectura reciben `CLIENTS_READ`, `WORKS_READ`, `CONTRACTS_READ` y `DOCUMENT_VALUES_READ`. La jerarquía no concede permisos implícitos; `CONTRACTS_READ` tampoco crea una pantalla o consulta todavía inexistente.

## 6. Crear las cuentas del escritorio

Abre la administración desde una terminal interactiva:

```powershell
docker compose exec brica-server java -jar /app/app.jar --spring.main.web-application-type=none --spring.profiles.active=security-admin
```

No añadas `-T`, tuberías ni redirecciones de entrada. Este proceso no abre otro servidor web y utiliza la conexión a la base del contenedor. Su acceso está protegido por los permisos del sistema operativo y de Docker: no lo expongas a usuarios no autorizados.

Cuando aparezca el menú, escribe `user` y completa:

| Pregunta | Cuenta gerencial | Cuenta administrativa |
|---|---|---|
| Usuario (exacto) | `UsuarioGerencial` | `UsuarioAdministrativo` |
| Nombre visible | `Usuario Gerencial` | `Usuario Administrativo` |
| ID del rol | ID real de Gerencial | ID real de Administrativo |
| Contraseña | Una contraseña propia | Otra contraseña propia |
| Confirmar contraseña | Repetir la primera | Repetir la segunda |

Crea una cuenta y repite `user` para la otra. Espera `Usuario creado.` y `Operación completada.`. Después escribe `exit`.

Las contraseñas no se muestran al escribirlas. Requieren al menos ocho caracteres, una mayúscula y un número; el máximo es 72 bytes UTF-8. Los usuarios se comparan exactamente, incluyendo mayúsculas.

**No hay una contraseña predeterminada.** Las cuentas y claves de otra instalación no se descargan con Git. No utilices `tools/ReplaceTestAccounts.java` para el alta inicial: sirve para una sustitución específica de cuentas antiguas, no para instalar desde cero. Tampoco necesitas el entorno temporal de `tools/review-environment.py`.

## 7. Abrir el escritorio

En otra terminal PowerShell, desde la raíz:

```powershell
$env:BRICA_ALLOW_HTTP="true"
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

Maven Wrapper descarga Maven y las dependencias del escritorio la primera vez. Accede con `UsuarioGerencial` o `UsuarioAdministrativo` y la contraseña que definiste para esa cuenta.

Estas variables se aplican a la terminal actual; repítelas en una terminal nueva. No uses el puerto temporal antiguo `49745`. La propiedad JVM `brica.server.url`, si estuviera configurada, tiene precedencia sobre `BRICA_SERVER_URL`.

## 8. Primer recorrido de uso

Una instalación nueva no trae clientes, obras ni plantillas demostrativas.

1. **Clientes → Nuevo cliente:** completa el formulario y guarda.
2. **Obras → Nueva obra:** selecciona cliente, escribe nombre y descripción y guarda. Ubicación y fechas se capturan después; la descripción conserva su carácter opcional.
3. Utiliza los cuatro iconos de la obra para editar información completa, seguir el asistente, consultar información o administrar paquetes/plantillas/versiones.
4. En el asistente puedes adjuntar contrato y presupuesto base, u omitirlos. Admite DOCX, PDF y XLSX de hasta 10 MiB, sin extracción automática.
5. En **Catálogo de campos** encontrarás 17 campos fijos predefinidos. Los nuevos adicionales usan clave y tipo `TEXT`, `DATE` o `AMOUNT`; sus valores se capturan por obra.
6. Desde obra o cliente crea un paquete borrador, crea una plantilla dentro de él, registra y analiza una versión DOCX, revisa los campos y confirma la versión. Después confirma el paquete cuando sus plantillas sean válidas.

Las plantillas DOCX tienen un límite de análisis de **1 MiB**, distinto del límite de adjuntos. Los paquetes creados desde una obra son exclusivos de ella; los creados desde un cliente quedan asociados a ese cliente. Conserva la ventana mientras indique Guardando; el cierre local del formulario no significa que el servidor haya confirmado la persistencia.

**Generar documentos está deshabilitado:** extracción y generación/exportación definitiva siguen fuera de esta versión. La sección Contratos del escritorio todavía es provisional.

## 9. Detener y volver a iniciar

Cierra el escritorio. Para detener sin borrar los datos:

```powershell
docker compose stop
```

Para volver a iniciar:

```powershell
docker compose up -d brica-server
$env:BRICA_ALLOW_HTTP="true"
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

Las cuentas y datos permanecen en el volumen PostgreSQL. Los archivos permanecen en el volumen documental, que conserva el nombre histórico `brica_document_test_files`; no es almacenamiento temporal. Ambos servicios tienen política `unless-stopped`.

**No ejecutes `docker compose down -v` ni borres volúmenes:** puedes perder datos y archivos. La persistencia no sustituye los respaldos. Tras reiniciar el servidor tendrás que iniciar sesión otra vez.

## 10. Restablecer una contraseña

Abre la consola del paso 6. Escribe `password`, indica el usuario exacto e introduce y confirma su nueva contraseña. Termina con `exit`. No necesitas guardar claves en SQL ni modificar hashes manualmente.

## 11. Actualizar una instalación

Antes de actualizar una instalación con información importante, respalda PostgreSQL y el volumen de documentos en almacenamiento privado durable. Esta guía no automatiza el respaldo ni su restauración.

```powershell
git status
```

Si tienes cambios locales, revísalos y consérvalos antes de continuar. Cuando la copia esté preparada para actualizar:

```powershell
git pull --ff-only
docker compose up -d --build brica-server
```

Cierra y vuelve a abrir el escritorio con el paso 7. Flyway aplica las nuevas migraciones al iniciar; si alguna falla, revisa los registros. No edites migraciones aplicadas, no borres volúmenes y no fuerces la actualización del historial para resolverlo.

## 12. Problemas frecuentes

| Problema | Comprobación |
|---|---|
| GitHub indica repositorio inexistente | URL y acceso de tu cuenta al repositorio privado. |
| Docker no conecta | Docker Desktop abierto y motor disponible. |
| JavaFX o Maven no inicia | `java` y `javac` deben usar JDK 25; revisar `JAVA_HOME`. |
| Puerto 8080 ocupado | Detener el otro servidor o cambiar `BRICA_SERVER_PORT` y la URL del cliente. |
| Puerto 5433 ocupado | Cambiar `POSTGRES_PORT`; el servidor Docker sigue conectando internamente a `postgres:5432`. |
| Salud responde, pero no permite acceder | `BRICA_ALLOW_HTTP=true` en `.env` y en el entorno del escritorio. |
| Usuario incorrecto | Crear la cuenta con `security-admin` y revisar mayúsculas. |
| La administración exige terminal interactiva | Ejecutar `docker compose exec` sin `-T`, redirecciones ni tuberías. |
| Se cambió `.env`, pero no el servidor | Reaplicar `docker compose up -d brica-server`; editar la contraseña no cambia una base existente. |
| El cliente conecta a otro puerto | Configurar `BRICA_SERVER_URL` y revisar la propiedad JVM `brica.server.url`. |

## Referencia rápida

| Acceso | Valor |
|---|---|
| PostgreSQL | Usuario técnico `brica`, contraseña de `.env`. |
| Escritorio gerencial | `UsuarioGerencial`, contraseña creada en la consola. |
| Escritorio administrativo | `UsuarioAdministrativo`, su propia contraseña. |
| API local | `http://localhost:8080` |
| Salud | `http://localhost:8080/api/v1/health` |
| PostgreSQL desde el equipo | `localhost:5433`, base `brica`. |

Para desarrollo del backend fuera de Docker y pruebas automatizadas, consulta [README.md](../README.md). Para la aceptación funcional, consulta el [condición de aceptación](../specs/001/spec.md#9-condición-de-aceptación). La aceptación visual Windows/Word y el despliegue público con HTTPS se verifican por separado; esta guía no afirma que estén completados.
