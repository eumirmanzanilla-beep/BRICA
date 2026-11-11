# Prueba manual — configuración documental hasta F6

**Alcance:** casos construidos, únicamente nombres de obra/cliente. No genera documentos definitivos, no extrae PDF y no acepta un despliegue de producción. Esta guía no acredita que el recorrido ya se haya ejecutado en Windows/Word.

## 1. Preparación en WSL

Trabajar desde la raíz con Java 25 y Docker Desktop disponibles. No borrar bases ni volúmenes para preparar la prueba.

### Python y pruebas automatizadas

El servidor necesita un intérprete con las dependencias fijadas; no las instala durante una solicitud.

```bash
python3 -m venv brica-server/.venv-documents
brica-server/.venv-documents/bin/python -m pip install -r brica-server/src/main/python/requirements.txt
export BRICA_DOCUMENT_PYTHON="$PWD/brica-server/.venv-documents/bin/python"
export BRICA_DOCUMENT_SCRIPT="$PWD/brica-server/src/main/python/brica_docx.py"
export BRICA_DOCUMENT_STORAGE="$PWD/brica-server/var/documents"
./mvnw test --no-transfer-progress
./mvnw clean verify --no-transfer-progress
```

En Debian/Ubuntu, `venv` puede requerir el paquete del sistema correspondiente a la versión de Python. Si ya dispones de `uv`, la alternativa sin cambiar paquetes del sistema es:

```bash
uv venv brica-server/.venv-documents
uv pip install --python brica-server/.venv-documents/bin/python -r brica-server/src/main/python/requirements.txt
```

Después exporta las mismas variables anteriores. Python no se necesita en Windows: solo en el servidor y en el entorno de pruebas. Maven incorpora la suite Python a través de `PythonDocxAnalyzerTest`; si faltan dependencias, falla en vez de omitirla. Su detalle queda en `brica-server/target/python-tests.log`.

### Backend

Usar las instrucciones de [README](../../README.md) para configurar `DB_*` sin imprimir secretos. Maven no carga `.env`. Verificar el puerto efectivo: `5433` es el predeterminado, no necesariamente el de tu contenedor.

```bash
docker compose up -d postgres
export BRICA_ALLOW_HTTP=true # Solo desarrollo local sin TLS.
./mvnw -pl brica-server spring-boot:run
```

Flyway aplicará V7 al arrancar contra la base elegida. Durante la implementación V7 se probó en bases desechables; no se aplicó manualmente a la base de desarrollo con DBHub.

Alternativa: `docker compose up -d --build brica-server`. La imagen incluye Python y sus dependencias. Compose usa un volumen **de prueba** separado para archivos; no ejecutar simultáneamente ambos backends en el mismo puerto. El detalle y los límites de almacenamiento se prueban aparte en [storage-test.md](storage-test.md).

### Cuentas y datos

La consola administrativa local del README permite crear roles con `role` y usuarios con `user`, o actualizar permisos con `permissions`. No existen credenciales predeterminadas; no restablecer cuentas existentes para realizar esta prueba.

Preparar cuentas independientes:

| Cuenta de prueba | Permisos documentales | Resultado esperado |
|---|---|---|
| Sin permisos | Ninguno | No puede consultar ni administrar documentación. |
| Consulta | `DOCUMENT_VALUES_READ` | Solo opciones/valores por obra; no administra ni descarga plantillas. |
| Administración | `DOCUMENT_CONFIG_ADMIN` | Configura campos/plantillas/paquetes, sin leer valores de obras. |
| Ambas funciones | Los dos anteriores | Recorrido documental completo. |

Usar además una cuenta con los permisos de Clientes/Obras necesarios para preparar y corregir datos. Los permisos documentales **no** conceden permisos de negocio implícitos. Crear dos clientes y dos obras asociadas a clientes distintos; anotar sus IDs y nombres. Reservar estos registros únicamente para pruebas.

Conservar capturas o respuestas de consulta de Obra/Cliente **antes** de registrar versiones y **antes** de editar composición: son las dos comprobaciones separadas de CA-35.

## 2. Abrir el escritorio en Windows

La copia Windows debe contener los mismos archivos que la de WSL.

```powershell
$env:BRICA_ALLOW_HTTP="true" # Solo desarrollo local.
$env:BRICA_SERVER_URL="http://localhost:8080"
.\mvnw.cmd -pl brica-client javafx:run
```

Verificar acceso incorrecto/correcto, Enter y doble clic sin solicitudes duplicadas, cerrar sesión y acceder con otra cuenta. Con permisos retirados o cuenta/rol desactivados, las llamadas posteriores deben dejar de autorizarse. No compartir contraseñas, cookies ni capturas con secretos.

## 3. Catálogo — F1/F2

1. Con administración, abrir **Documentación → Campos**.
2. Registrar `NOMBRE_OBRA` → nombre de obra y `NOMBRE_CLIENTE` → nombre del cliente asociado. Si ya existen, reutilizarlos; no duplicarlos.
3. Probar clave duplicada, minúsculas y espacios: debe rechazarse sin modificar el catálogo. El máximo es 50 caracteres y el marcador completo puede medir 54.
4. Renombrar el nombre visible de un campo. La clave, el origen y el tipo no deben cambiar.
5. Con la cuenta de consulta, **Campos**, **Plantillas** y **Paquetes** deben estar deshabilitados; **Valores por obra** debe estar disponible. Con administración sin consulta ocurre lo contrario.

La API interna de F2 sigue siendo `POST /api/v1/reference-values/resolve`, cuerpo `{"workId":17,"fieldIds":[1,2]}` (sustituir por IDs reales). El permiso es consulta, incluso sin administración. Debe resolver los nombres de cada obra y de su propio cliente, sin escribir en esas entidades.

**Límite:** los dos nombres son obligatorios en el modelo de negocio. No introducir nulos por SQL para inventar un escenario manual de faltantes. La clasificación de texto ausente/inválido/error está probada aisladamente; CA-04/10 no se aceptan completos por esa evidencia.

## 4. Plantilla y primera versión — F3/F4

Usar copias de `brica-server/src/test/resources/documents/`; nunca documentos operativos. El acta demostrativa es un fixture técnico, no la plantilla de negocio acordada.

1. En **Plantillas**, indicar nombre, tipo documental (por ejemplo `ACTA_PRUEBA`) y estado **Borrador**. Pulsar **Crear plantilla** y seleccionarla en la lista.
2. Pulsar **Agregar versión → Seleccionar DOCX y analizar** y elegir `acta-inicio-prototipo.docx`.
3. Esperar sin bloqueo de ventana. Debe mostrar dos campos y cuatro apariciones, incluyendo cuerpo/runs divididos, celda, encabezado y pie.
4. En la primera versión no hay clasificación automática: elegir Obligatorio/Opcional para **todos** los campos. Omitir uno e intentar confirmar: debe solicitar su clasificación.
5. Confirmar. El diálogo se cierra al completar; al volver a seleccionar la plantilla, aparece la versión 1, archivo, hash y asociaciones.
6. Abrirla en el listado de versiones: todo es de solo lectura. Renombrar después un campo no cambia clave, origen ni asociaciones.
7. Comparar Obra/Cliente con la captura inicial: no deben cambiar (**primera evidencia CA-35**).
8. Cambiar estado a **Vigente** y pulsar **Guardar seleccionada** antes de confirmar un paquete.

### Rechazos y documento estático

- `marcador-desconocido.docx`: diagnóstico de clave desconocida y confirmación bloqueada.
- `marcador-entre-parrafos.docx`: diagnóstico con ubicación; no se acepta como estático.
- Copia con `{{nombre_obra}}`, `{{ NOMBRE_OBRA }}` o una expresión Jinja: debe bloquearse. Solo se admite `{{CLAVE}}` exacto.
- `documento-estatico.docx`: informa ausencia de campos. Solo se registra al marcar expresamente **Confirmo que es un documento estático**.
- ZIP inválido, relaciones externas, macros, XML inseguro o regiones sin soporte: deben rechazarse, sin crear una versión utilizable. Límite inicial: 1 MiB comprimido, 20 MiB descomprimido y 512 entradas.

### Segunda versión y revisión cambiada

1. Hacer una copia del fixture en Word. Conservar un campo, retirar otro y, tras registrar otra clave del catálogo sobre uno de los dos atributos admitidos, agregar ese marcador nuevo.
2. Analizar como revisión de la misma plantilla: el campo conservado propone su obligatoriedad previa; el nuevo exige clasificación; el retirado no se incorpora.
3. Ajustar la propuesta y confirmar. Debe existir versión 2; versión 1 conserva archivo y configuración.
4. En una prueba separada, analizar y **modificar el archivo en disco antes de confirmar**, aunque mantenga el nombre. Debe devolver un análisis actualizado y pedir nueva revisión, sin registrar automáticamente.
5. Repetir cambiando el nombre visible de un campo desde otra sesión después del análisis: también exige revisar de nuevo.

El ID de confirmación se muestra antes del envío. Si se pierde la respuesta, usar **Consultar estado**, no crear inmediatamente otra confirmación. Si está pendiente, usar **Recuperar**. También puede pegarse el ID en **Plantillas → Recuperar registro** tras reiniciar el escritorio. `COMPLETED` devuelve el resultado original; `FAILED` requiere corregir la causa y una confirmación intencional nueva. Los fallos entre escrituras y las carreras se inyectan en pruebas automatizadas, no deteniendo la base de desarrollo.

## 5. Paquetes — F5/F6

1. En **Paquetes**, crear nombre/cliente y seleccionar una plantilla; guardar sin confirmar. Debe aparecer como `DRAFT`.
2. Cambiar captura y cerrar la aplicación: elegir **Guardar cambios**. Abrir de nuevo, seleccionar el paquete y pulsar **Abrir seleccionado**. Debe recuperar captura y selección.
3. Mientras se edita, pulsar **Registrar una plantilla**: cambiará de pestaña. Registrar una plantilla/versión y volver a **Paquetes**. La captura debe conservarse. Si hay muchas plantillas, usar **Cargar más plantillas**.
4. Confirmar con plantilla Vigente y versión completa: queda `CONFIRMED`. Vacío, cliente inexistente, duplicados o selección inelegible deben rechazarse sin reemplazar la configuración previa.
5. Registrar otros paquetes del mismo cliente: el nombre exactamente repetido se rechaza; `Paquete`, `paquete` y ` Paquete ` son distintos. No se exige operación/etapa ni orden de generación.
6. Editar nombre/composición de uno confirmado. Su cliente permanece fijo. Las plantillas se muestran por nombre e ID como desempate. Comparar Obra/Cliente (**segunda evidencia CA-35**).
7. Abrir el mismo paquete en dos procesos del escritorio. Guardar en el primero; guardar la edición antigua en el segundo: debe pedir recargar. No debe sobrescribir el primer cambio ni aplicar un reintento automático.
8. Probar **Descartar borrador guardado**: solo elimina ese borrador y su composición, no plantillas/versiones. No se admite para un paquete confirmado.

Cerrar o salir con cambios sin guardar debe ofrecer guardar, salir sin guardar o cancelar. Un cierre abrupto del proceso solo recupera lo ya guardado; no se promete conservar cambios que nunca se enviaron.

## 6. Estados, alternativas y valores

1. Crear dos plantillas del **mismo tipo documental**, cada una con versión completa, e incorporarlas a un paquete confirmado del cliente A.
2. En **Valores por obra**, indicar el ID de su obra, consultar opciones y elegir paquete. Cada tipo ofrece un selector: solo una alternativa por tipo; no hay orden obligatorio entre documentos.
3. Elegir plantilla/versión y consultar valores. Mostrar solo los campos de esa versión, marcador, origen, texto, estado y obligatoriedad. Para estático: sin campos.
4. Cambiar una plantilla a **Borrador** y luego **No disponible**. Sigue visible, consultable y administrable (incluso admite versiones); no se ofrece para nueva selección y no se acepta al confirmar una composición que la incluya. Su composición histórica no desaparece.
5. Si ninguna plantilla del paquete es elegible, no se ofrece ese paquete para nueva selección. Al restituir Vigente y disponer de un archivo íntegro, vuelve a ofrecerse.
6. Registrar otra versión después de consultar opciones y antes de resolver: la selección anterior debe rechazarse y pedir consulta actualizada. Repetir tras editar composición del paquete.
7. Consultar obra del cliente B: no debe recibir paquetes del A. Los borradores no aparecen como aplicables.
8. Corregir un nombre desde **Obras** o **Clientes**, usando la cuenta con su permiso de escritura, y volver a consultar: debe mostrarse el nuevo texto. No hay captura de valores documentales independientes.
9. Eliminar lógicamente una plantilla: permanece en listados/históricos de solo lectura; ya no puede editarse, agregar versiones ni seleccionarse.

El motor de generaciones reales, sus archivos producidos y su historial permanecen fuera de esta entrega. No confundir la lista de valores con una previsualización renderizada.

## 7. Registro de resultados

Completar por escenario; no sustituir la evidencia manual por el resultado de Maven.

| Fecha / sistema | Caso y CA | Datos/IDs y versión | Esperado | Observado | Evidencia | Resultado |
|---|---|---|---|---|---|---|
| Pendiente | Acceso y permisos, CA-17/19/20/42 | Sin credenciales en la evidencia | Acciones independientes | | | |
| Pendiente | Análisis/estático, CA-06/07/11/12/26/36/37 | Fixture y hash | Diagnóstico/clasificación | | | |
| Pendiente | Versiones, CA-08/09/21/22/27/28/29/33/39/40 | Confirmación y revisión | Conservación/recuperación | | | |
| Pendiente | Paquetes, CA-13/15/16/18/24/30/31/32/34/38 | Paquete, revisión, cliente | Validación/aplicabilidad | | | |
| Pendiente | CA-35, registro de versión | Obra/cliente antes/después | Sin cambios de negocio | | | |
| Pendiente | CA-35, edición de composición | Obra/cliente antes/después | Sin cambios de negocio | | | |

Pendientes explícitos: CA-04/10 con un origen opcional real, ampliación de fechas/importes (CA-25/41), historial de generaciones reales y plantilla operativa conjunta. No iniciar F7 ni declarar aceptación completa al finalizar este recorrido técnico.
