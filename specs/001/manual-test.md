# Prueba manual — navegación por obra y persistencia

## 1. Instalación habitual

El responsable autorizó aplicar V11–V13 a `brica` y sustituir `arqAdiran`, `UserTest` y `CarlosYonson` por `UsuarioGerencial` y `UsuarioAdministrativo`, conservando obras, clientes, contratos y archivos. No hay semillas automáticas de cuentas ni contraseñas en el repositorio. Los roles conservan los permisos aprobados: Gerencial 4 y Administrativo 3 con los ocho actuales; Practicante 2 e Invitado 1 solo lectura. Propuestas/aprobación fuera de esta iteración.

**No usar el servidor temporal del puerto 49745 como aplicación habitual.** Compose publica normalmente el backend persistente en `http://localhost:8080`. PostgreSQL y documentos están en volúmenes permanentes; el de archivos conserva su nombre histórico para no perder datos. No ejecutar `down -v`, borrar volúmenes ni restablecer cuentas para probar.

Desde Linux/WSL, con `.env` configurado y después de respaldar/verificar las migraciones:

```bash
# Solo instalación local sin TLS, si esa es la configuración acordada.
BRICA_ALLOW_HTTP=true docker compose up -d --build brica-server
BRICA_ALLOW_HTTP=true BRICA_SERVER_URL=http://localhost:8080 ./mvnw -pl brica-client javafx:run
```

En Windows, usar el mismo código:

```powershell
$env:BRICA_ALLOW_HTTP="true"
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

Acceder con una de las dos cuentas propuestas y la contraseña de pruebas acordada. No incluir secretos/cookies en capturas. El despliegue accesible por red requiere configurar HTTPS; HTTP es únicamente la opción explícita local.

## 2. Obras: alta y navegación

1. Pulsar **Nueva obra**. Debe pedir únicamente cliente, nombre y descripción, sin obligar ubicación o fechas. Con cliente y nombre válidos se puede crear la obra; descripción conserva la validación de texto existente.
2. La tabla muestra Cliente, Nombre, Descripción y cuatro iconos pequeños al final. La barra superior contiene Nueva obra, Actualizar, Eliminar y Generar documentos.
3. **Generar documentos** está visible pero deshabilitado con explicación de próxima iteración; no simula una generación exitosa.
4. **✏ Editar** permite cambiar nombre, descripción, ubicación y demás campos de obra. El cliente contextual no se reasigna silenciosamente. Fechas `dd/MM/aaaa`/ISO opcionales, con validación estricta y comparación solo cuando ambas están capturadas. Importes precisos, sin cálculo fiscal automático.
5. Guardar debe cerrar localmente sin esperar la red y mostrar Guardando. Solo después de confirmar el servidor cambia a Guardado. Si falla o hay `412`, se reabre la captura con el error y sin perder datos; no se reintenta automáticamente ni se anuncia éxito prematuro.
6. **👁 Ver** presenta toda la información en consulta, incluidos adicionales y adjuntos con opción de guardar una copia. No ofrece Guardar cambios. Usuarios de lectura no pueden utilizar las acciones de escritura.
7. **Eliminar** hace borrado lógico: conserva el historial y no borra archivos asociados.

## 3. 📄 Asistente de información

1. **Contrato y presupuesto base:** adjuntar uno o ambos, o pulsar Omitir documentos. Se admiten DOCX/PDF/XLSX hasta 10 MiB. Se conservan bytes, hash, nombre original y relación con obra; no se extraen ni rellenan datos automáticamente. Los archivos anteriores permanecen, no se sustituyen sin aviso.
2. Probar extensión/contenido incompatible y macro OOXML: deben rechazarse. Descargar el adjunto y contrastar su hash/bytes con el original. Una obra distinta no puede descargarlo por cambiar el identificador en la URL.
3. **Información:** completar campos fijos y adicionales. Textos pueden tener varias líneas; fechas inválidas como `31/02/2026` se rechazan; importes conservan todos los decimales.
4. **Revisión:** muestra los campos antes de guardar y permite volver a corregir. Las fechas se muestran en español. No es una previsualización renderizada de un documento Word.
5. Guardar conserva la información con `If-Match` y cierra el diálogo localmente. Dos sesiones sobre la misma revisión: la segunda debe recibir conflicto sin sobrescribir; su captura se mantiene para revisarla.

## 4. 🖨 Paquetes → plantillas → versiones

1. Desde una obra, pulsar el icono de paquetes. La ventana indica el ámbito fijo de esa obra, sin pedir IDs ni volver a seleccionar cliente.
2. **Nuevo paquete:** elegir primero un nombre. Se guarda como borrador vinculado automáticamente a la obra y a su cliente real obtenido por el servidor. Todavía no requiere plantillas.
3. Dentro del paquete, **Crear plantilla:** indicar nombre y tipo documental. El tipo agrupa alternativas del mismo documento; no es el formato del archivo. El contexto de obra/cliente se hereda del paquete.
4. Seleccionar esa plantilla y **Registrar versión**. Elegir un DOCX de hasta 1 MiB, analizar, revisar marcadores y clasificar cada campo. Se rechazan desconocidos, Jinja libre o regiones sin soporte. DOCX sin marcadores requiere aceptación explícita como estático.
5. Confirmar debe ocultar inmediatamente el registro mientras el servidor procesa. Un error o análisis cambiado vuelve a mostrar la captura/revisión. El ID permite consultar o recuperar una operación sin duplicarla.
6. Tras una versión completa, **Confirmar paquete**. Plantilla sin versión válida o no vigente: rechazo. La confirmación controla la revisión del paquete.
7. Registrar versión 2 y consultar ambas: la anterior permanece inmutable. Doble clic en una versión muestra su configuración. Confirmaciones simultáneas reciben números distintos.
8. Paquetes confirmados pueden cambiar nombre o composición válida; el ámbito confirmado queda fijo. Para crear plantillas todavía sin versión, utilizar un paquete borrador. Retirar una plantilla del paquete no borra la plantilla ni sus versiones.
9. Desde **Clientes → Paquetes documentales**, repetir: el nuevo paquete se enlaza automáticamente al cliente seleccionado y queda en ámbito cliente, no asignado a una obra por inferencia.
10. Un paquete/plantilla de obra A no se ofrece ni admite en obra B, aunque tengan el mismo cliente. Los de ámbito cliente solo se aplican a obras de ese cliente. Los registros antiguos sin ámbito se preservan como legado, no se reasignan automáticamente.
11. Los nombres de plantilla conservan la unicidad exacta global anterior. Los paquetes comparan nombres exactamente dentro de su ámbito; no normalizar espacios o mayúsculas.

## 5. Catálogo de campos

1. La navegación ya no ofrece el módulo global Documentación; muestra **Catálogo de campos**, dedicado a definiciones. Paquetes/plantillas/versiones se administran desde obra o cliente.
2. Los 17 campos fijos se predefinen con su origen interno; no hay selector de rutas ni lista técnica de atributos en Crear campo.
3. Crear un adicional indicando clave y tipo TEXT/DATE/AMOUNT. La clave es también su nombre visible y sus valores se capturan por obra, no en el catálogo.
4. Rechazar claves con minúsculas, espacios, duplicadas o de más de 50 caracteres. Los marcadores siguen siendo `{{CLAVE}}` exactos, de hasta 54 caracteres.
5. Si se elimina un campo fijo sin vínculos y se vuelve a crear con su clave reservada, el servidor restablece su tipo/origen internos, sin adivinarlos por el nombre escrito.
6. Eliminar un campo sin versiones: confirmación Eliminar, `204`, desaparición y mensaje de éxito. Un campo vinculado a cualquier versión permanece protegido, aunque la plantilla ya no esté vigente o esté eliminada. No sembrar plantillas demostrativas sobre todos los campos para esta prueba.
7. Un adicional sin capturar resuelve a `MISSING`; valores de una obra no se comparten con otra. La eliminación de una definición sin vínculos no borra silenciosamente valores históricos de obra.

## 6. Paginación, respuesta y permanencia

1. Con menos de una página, no deben verse Anterior/Siguiente. Solo aparece Anterior si existe página previa y Siguiente cuando se comprueba que hay datos posteriores; no únicamente porque la página actual esté llena.
2. Confirmar formularios con red demorada: el cierre local no debe esperar al servidor, y el estado debe distinguir Guardando de Guardado. La latencia de red/BD no se presenta como tiempo de cierre local. La prueba gráfica mide un caso concreto, no garantiza todos los equipos o redes.
3. Reiniciar únicamente el backend y acceder otra vez: cuentas, obras, campos, paquetes, versiones y adjuntos permanecen. No crear cuentas ni restaurar datos al arrancar. No detener PostgreSQL durante una operación para simular errores en la instalación habitual.
4. Comparar clientes/obras/contratos/archivos anteriores con el respaldo. Las migraciones no deben borrar sus filas ni inventar fechas para hacerlas obligatorias.

## 7. Verificación automatizada y aceptación

Con Docker activo y `BRICA_DOCUMENT_PYTHON` apuntando a un entorno con las dependencias fijadas:

```bash
./mvnw verify --no-transfer-progress
git diff --check
./mvnw -pl brica-client test dependency:build-classpath -Dmdep.outputFile=/tmp/opencode/brica-fx-classpath --no-transfer-progress
BRICA_ALLOW_HTTP=true java --enable-native-access=ALL-UNNAMED \
  -cp "brica-client/target/test-classes:brica-client/target/classes:$(cat /tmp/opencode/brica-fx-classpath)" \
  com.brica.client.presentation.NavigationFxSmoke
```

Las pruebas JUnit usan PostgreSQL desechable sin tocar datos de negocio de la instalación persistente. La prueba gráfica comprueba FXML, cierre local y recuperación ante fallo. El registro ejecutado, respaldo y estado de la instalación están en `review.md`.

| Caso | Sistema / fecha | Esperado | Observado / evidencia | Resultado |
|---|---|---|---|---|
| Alta mínima e iconos | Pendiente Windows | Tres datos iniciales, cuatro acciones | | |
| Asistente y consulta | Pendiente Windows | Adjuntos, información, revisión | | |
| Paquete/plantilla/versión | Pendiente Windows/Word | Orden y ámbito correctos | | |
| Respuesta local | Pendiente equipo del usuario | Cierre sin esperar a red, sin falso éxito | | |
| Persistencia | Registrar reinicio realizado | Sin pérdida de datos ni cuentas | | |

Fuera de alcance: extracción PDF/Excel, generación/exportación definitiva, aprobación del Practicante, plantilla operativa conjunta y afirmación de aceptación visual Windows/Word sin ejecutarla.
