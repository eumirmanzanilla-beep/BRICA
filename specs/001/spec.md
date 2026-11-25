# Spec 001 — Plantillas, paquetes y campos de referencia de BRICA

> **Actualización funcional 2026-11-25:** los paquetes se crean vigentes, pueden estar vacíos y permiten cambio de nombre y eliminación lógica. La confirmación registra que el paquete está listo: requiere al menos una plantilla vigente y una versión vigente por cada plantilla vigente; las plantillas no vigentes pueden estar vacías. Si una modificación incumple la regla, el paquete deja de estar listo y debe confirmarse de nuevo. Las plantillas y sus versiones tienen vigencia independiente. Para nuevas selecciones se usa la versión vigente más reciente registrada correctamente; si se desactiva, se usa la anterior vigente. Las plantillas y versiones eliminadas se conservan como historial y se ocultan de las vistas principales. Esta actualización sustituye las referencias posteriores a borradores de paquetes y a la selección exclusiva de la última versión sin considerar su vigencia.

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Estado:** Implementado; aceptación manual pendiente
**Fecha / actualización:** 2026-11-20
**Issue:** pendiente de referencia
**Plan:** [plan.md](plan.md) · **Seguimiento:** [review.md](review.md)

**Resultado esperado:** usuarios autorizados administran campos, plantillas, versiones y paquetes, y consultan los valores de una obra. No hay etapas ni orden obligatorio de generación.

## 1. Contexto y fuentes

Esta entrega cubre la configuración documental y la consulta de valores desde los datos vigentes de Obra y Cliente. Deja los paquetes listos para un motor futuro; no genera documentos.

ERS, DAS y Diccionario aportan requisitos y modelo. La implementación vigente se confirma en el código y las migraciones, no solo en esos documentos iniciales.

Fuentes: `docs/informative/`, código documental de servidor y cliente, migraciones Flyway y [seguimiento](review.md). Los documentos iniciales contemplan etapas; esta entrega no las exige.

## 2. Alcance

Se admiten plantillas DOCX y adjuntos DOCX/PDF/XLSX. Extracción y generación definitiva pertenecen a otras entregas. La aceptación manual y la comprobación de permisos siguen pendientes.

**Incluye:** catálogo y valores, análisis DOCX, versiones, paquetes, borradores y captura asociada.

**No incluye:** extracción, generación/exportación, historial real de generaciones ni despliegue y retención definitivos.

### Reglas vigentes

- El catálogo incluye atributos fijos aprobados de Cliente/Obra y adicionales `TEXT`, `DATE` o `AMOUNT`. El servidor controla tipo, origen y ruta; nunca evalúa rutas libres. Cada dato se corrige en su entidad de origen.
- Los importes conservan precisión. La API devuelve por separado valor preciso y presentación; no calcula impuestos ni convierte monedas. Las fechas aceptan ISO o `dd/MM/aaaa` y se presentan en español. Estados de obra: En Proceso, En pausa, Terminada y Cancelada.
- Los paquetes son de cliente o de obra. Los de cliente se reutilizan en obras del mismo cliente; los de obra no se comparten. No hay operación/etapa ni secuencia obligatoria.
- Plantillas DOCX se analizan en servidor mediante un proceso Python local (`docxtpl`); Python no accede a PostgreSQL. Los adjuntos DOCX/PDF/XLSX no se extraen. El almacenamiento probado no define despliegue, respaldo ni retención definitivos.
- Nombres de cliente, obra, paquete, plantilla y versión: letras ASCII/Ñ, números y `_`; al menos una letra y sin `_` inicial. No se normalizan. Las fechas de terminación no pueden preceder al inicio.
- Las versiones tienen nombre propio, son históricas y no se renumeran. Eliminarlas es lógico. Los adjuntos de obra también se eliminan lógicamente; nunca se purgan archivos con estas acciones.
- Una plantilla debe estar vigente, no eliminada y tener una versión completada para nuevas selecciones. La composición de un paquete confirmado solo cambia con una versión elegible y control de concurrencia.
- La interfaz usa la API; el servidor aplica permisos. Guardados asíncronos conservan la captura ante fallos y no anuncian éxito antes de confirmación.

Las decisiones puntuales sobre la instalación local y su limpieza se registran en [review.md](review.md); no son reglas de arranque ni autorizaciones generales. Las referencias heredadas a operación/etapa requieren alineación separada.

## 3. Actores, permisos y precondiciones

- El servidor autentica y autoriza cada operación; ocultar una acción en JavaFX no sustituye el permiso.
- La consulta de valores requiere permiso de lectura. Administrar campos, plantillas, versiones y paquetes requiere permiso documental. Los selectores requieren además `CLIENTS_READ` o `WORKS_READ`.
- El servidor obtiene el cliente desde la obra registrada. Para confirmar un paquete se requiere un cliente y una plantilla elegible con versión disponible; los paquetes incompletos pueden guardarse como borradores.
- Roles estándar y evidencia de permisos: ver [review.md](review.md). La aceptación de seguridad requiere probar usuarios con y sin permisos.

## 4. Reglas y datos: modelo funcional

| Concepto | Responsabilidad | Ejemplo |
|---|---|---|
| Campo de referencia | Define una clave canónica única, un nombre visible, un tipo, un origen lógico y la ruta de un atributo admitido. | `NOMBRE_OBRA` representa el nombre registrado de la obra. |
| Valor de referencia | Resultado de resolver el campo para una obra y su cliente asociado. Se obtiene de la fuente vigente. | Para la obra 17, `NOMBRE_OBRA` resuelve a «Rehabilitación de la cancha». |
| Plantilla documental | Identidad reutilizable de un formato. Tiene nombre, tipo de documento y estado [Vigente, borrador, No disponible]. | Acta de inicio. |
| Versión de plantilla | Archivo registrado y relación con los campos, marcadores y obligatoriedad usados en esa revisión. | Versión 2 del acta, con `{{NOMBRE_OBRA}}` obligatorio. |
| Paquete documental | Conjunto de plantillas asociado a un cliente, sin clasificación temporal ni secuencia obligatoria. | Paquete documental del cliente A. |

Los estados **Vigente**, **Borrador** y **No disponible** son válidos. Todos permiten consultar y administrar la plantilla, incluido agregar versiones. Solo **Vigente**, no eliminada y con última versión completada verificable, permite confirmar su incorporación a un paquete y seleccionarla para uso. Una plantilla que cambia de estado sigue visible en los paquetes existentes, pero queda fuera de nuevas selecciones. La eliminación lógica es independiente y deja todo el registro en solo lectura. En esta spec, «activa» significa «Vigente y no eliminada».

La **obligatoriedad se define por versión y campo**. Un campo puede ser obligatorio en un acta y opcional en otro documento. El paquete reúne **identidades de plantilla**. Para cada documento de una generación, el usuario interno elige una sola plantilla activa de entre las aplicables. La única versión seleccionable para una generación nueva es la revisión más reciente registrada correctamente y no eliminada de esa plantilla. El sistema muestra su número para que el usuario confirme la selección. Cambiar de plantilla antes de confirmar sustituye la selección anterior. La generación confirmada conserva la plantilla y la versión exactas que se utilizaron.

## 5. Requisitos funcionales y trazabilidad

| ID | Comportamiento requerido | Fuente |
|---|---|---|
| CD-001 | Catálogo de claves únicas y atributos aprobados; el servidor determina origen y tipo. | DAS; Diccionario `CAMPO_REFERENCIA` |
| CD-002 | Resolver datos vigentes de Obra/Cliente, distinguir presente, faltante e inválido, y corregir en la entidad de origen. | RF-03/05; DAS |
| CD-003 | Analizar DOCX, validar marcadores y obligatoriedad, y reanalizar antes de confirmar. Permitir estáticos con confirmación explícita. | CU-08; RF-13 |
| CD-004 | Guardar versiones independientes e historial; solo la última versión válida de una plantilla elegible se usa en selecciones nuevas. | RF-14; RES-03; CU-08 |
| CD-005 | Gestionar paquetes y borradores sin etapas; validar elegibilidad y concurrencia. El historial real de generaciones depende del motor futuro. | RF-11/12/15; CU-03; D-02/D-07 |
| CD-006 | Listar opciones por obra y resolver campos de una única selección vigente, sin imponer orden de uso. | RF-06/08/18; CU-04 |
| CD-007 | Autorizar en servidor, informar errores accionables y hacer recuperable el registro; evitar duplicados y excluir versiones incompletas. | RF-26; RNF-03; CU-08 |

## 6. Historias y criterios de aceptación

### HU-01 · Administrar el catálogo y comprobar valores

Como usuario autorizado, quiero registrar los campos que las plantillas pueden utilizar y verificar qué valor aportaría cada uno en una obra.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-01 | Los campos fijos están predefinidos con origen interno | Registro un campo adicional con clave y tipo | BRICA usa la clave como nombre visible, valida el tipo y permite capturar su valor por obra, sin pedir elegir una ruta de origen. |
| CA-02 | Una clave ya registrada o una solicitud que envía una ruta libre, un origen no admitido o un tipo incompatible | Intento confirmar la definición | El servidor rechaza la solicitud, identifica el problema y conserva la configuración previa. |
| CA-03 | Una obra con nombre y cliente registrados | Consulto los valores de dos campos que apuntan a esos datos | BRICA muestra el nombre de la obra y el del cliente asociado, con su origen. Al consultar otra obra, resuelve sus propios valores. |
| CA-04 | Un campo que todavía no se ha capturado | Consulto el campo correspondiente | BRICA lo muestra como faltante. Si corrijo el dato desde el flujo de obra y vuelvo a consultar, aparece el valor actualizado. |
| CA-05 | Una clave ya vinculada con una versión registrada | Intento cambiar su clave, tipo, origen o ruta | BRICA conserva la definición vinculada. Puedo registrar otra clave para configuraciones futuras. |

### HU-02 · Registrar una plantilla y agregar versiones

Como usuario autorizado, quiero cargar una plantilla y comprobar que sus marcadores corresponden al catálogo antes de habilitarla.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-06 | Un DOCX legible con marcadores conocidos, incluido uno repetido | Cargo el archivo y reviso el análisis | BRICA muestra los campos reconocidos, su origen y las apariciones del marcador. Puedo indicar qué campos son obligatorios para esa versión. |
| CA-07 | Un DOCX con uno o más marcadores desconocidos o mal formados | Intento confirmar | BRICA señala los marcadores pendientes y bloquea el registro hasta que corrija y vuelva a validar el archivo. |
| CA-08 | Una versión registrada | Cargo y confirmo una nueva revisión válida | BRICA registra una versión distinta y la ofrece para nuevas generaciones. La versión anterior conserva su archivo y sus campos para consulta histórica. |
| CA-09 | Un archivo inválido o un fallo al guardar su archivo o metadatos | Intento registrar la versión | BRICA informa el problema y la versión incompleta no queda disponible para un paquete. Las versiones previas siguen accesibles. |
| CA-10 | Una versión válida con un campo obligatorio y una obra que todavía carece de su valor | Consulto los valores para esa obra | BRICA marca el valor como faltante y obligatorio y permite completar el dato mediante el flujo de su entidad de origen, con sus permisos y validaciones; no crea un valor documental independiente. La versión sigue siendo válida para otras obras que sí tienen ese dato. |
| CA-11 | Un DOCX con marcadores conocidos en el cuerpo, una celda, un encabezado y un pie de página, incluido un marcador dividido en fragmentos dentro de un mismo párrafo | Analizo y confirmo el guardado de la versión | BRICA reconoce y vincula todas las apariciones de los marcadores y permite registrar la versión. |
| CA-12 | Un marcador en una región no admitida o dividido entre párrafos distintos | Analizo el archivo | BRICA indica dónde está el marcador y bloquea el registro hasta que se corrija. |

### HU-03 · Configurar y consultar paquetes

Como usuario autorizado, quiero agrupar las plantillas que necesita un cliente y modificar esa composición cuando cambien sus formatos, sin imponer cuándo ni en qué orden pueden generarse los documentos.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-13 | Un cliente registrado y dos plantillas elegibles | Creo un paquete con ambas plantillas | BRICA guarda la identificación y la composición, y lo muestra al consultar paquetes para ese cliente, sin requerir operación/etapa ni orden de generación. |
| CA-14 | Un paquete existente | Agrego una plantilla y retiro otra, y confirmo | La nueva composición queda disponible para operaciones futuras. Las generaciones anteriores conservan la referencia al paquete y muestran las plantillas y versiones que utilizaron. |
| CA-15 | Un paquete vacío, un cliente inexistente o una plantilla duplicada o inelegible | Intento confirmar | BRICA identifica qué debe corregirse y conserva la configuración anterior. |
| CA-16 | Un paquete de otro cliente y un paquete aplicable con una plantilla activa y otra desactivada | Consulto la configuración para mi obra | BRICA excluye el paquete ajeno. Para la nueva selección solo ofrece la plantilla activa y su revisión más reciente, sin filtro por operación/etapa ni requisito de haber generado otro documento antes. |
| CA-17 | Un usuario sin permiso de administración | Intenta registrar una versión, modificar un campo o guardar un paquete | El servidor rechaza la operación y conserva los datos previos. |
| CA-18 | Varias plantillas activas aplicables a un documento | Selecciono una plantilla y confirmo la versión ofrecida, y después cambio de opción | El flujo conserva una única selección de plantilla y versión. El servidor rechaza una plantilla desactivada o eliminada, una revisión anterior, una versión que pertenece a otra plantilla o más de una selección para el mismo documento. |
| CA-19 | Un usuario autenticado con permiso global de consulta y sin permiso de administración | Solicito valores para dos obras registradas | BRICA devuelve en cada caso los datos de la obra y de su cliente asociado. El permiso de consulta no habilita cambios de configuración. |
| CA-20 | Una solicitud sin autenticación o de un usuario sin permiso de consulta | Solicito valores por identificador de obra directamente a la API | El servidor rechaza la solicitud. |

### HU-04 · Verificar la resolución y el historial

Como usuario autorizado, quiero que las opciones vigentes y el historial muestren la configuración que corresponde a cada selección y generación documental.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-21 | Una plantilla activa cuya versión 1 ya se usó | Se confirma el registro de la versión 2 | Las nuevas generaciones solo ofrecen la versión 2. El historial conserva el vínculo con la versión 1. Un intento fallido de registrar otra versión conserva la última revisión válida como seleccionable. |
| CA-22 | Una plantilla eliminada de forma lógica que ya tiene referencias históricas | Consulto su historial o intento seleccionarla, editarla o registrar una versión nueva | BRICA permite consultar su registro y versiones en modo de solo lectura. La excluye de nuevas selecciones y rechaza las operaciones de modificación o uso. |
| CA-23 | Una generación que utilizó una parte de las plantillas de un paquete | Cambio la composición del paquete y consulto esa generación | El historial muestra el paquete de origen y solo las plantillas y versiones efectivamente utilizadas. La composición actual se consulta por separado. |
| CA-24 | Una obra y una versión de plantilla aplicable que requiere algunos campos del catálogo | Solicito resolver la selección de la plantilla para generar un documento | BRICA devuelve únicamente los campos de esa versión. Rechaza una versión o plantilla que ya no sea elegible para el paquete. |
| CA-25 | Un importe cero permitido, un texto compuesto solo por espacios y un valor que incumple una regla aplicable | Resuelvo los campos | BRICA distingue y devuelve respectivamente presente, faltante e inválido. Si falla la lectura, informa un error de consulta en lugar de clasificar los datos como faltantes. |
| CA-26 | Una clave canónica de 50 caracteres y una clave con minúsculas, espacios o caracteres fuera del conjunto permitido | Registro los campos y valido sus marcadores | BRICA admite la clave canónica y su marcador completo de 54 caracteres. Rechaza las claves inválidas y exige coincidencia exacta con la sintaxis canónica dentro del DOCX. |

### HU-05 · Confirmar archivos y editar paquetes sin perder cambios
Como administrador de BRICA, quiero confirmar archivos previamente analizados, registrar sus versiones de forma segura y editar paquetes existentes

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-27 | Un DOCX analizado y revisado | Confirmo con otro archivo del mismo nombre, o con asociaciones del catálogo que cambiaron desde la revisión | El servidor vuelve a analizar el archivo y muestra un aviso breve con el resultado actualizado. El usuario revisa y confirma de nuevo antes de registrar la versión. |
| CA-28 | Un registro de versión pendiente que se interrumpe tras guardar solo parte de la información | Se recupera la operación o el usuario consulta su estado | BRICA reanuda el registro si puede verificar lo conservado, o lo marca como fallido e indica cómo reintentar. La última versión válida sigue disponible hasta que se complete el nuevo registro. |
| CA-29 | Una confirmación de carga que ya fue procesada, pero cuya respuesta no recibió el usuario | Se reenvía la misma confirmación | BRICA devuelve el resultado de la operación original y evita registrar una segunda versión. Una carga intencional distinta se tramita por separado. |
| CA-30 | Dos administradores editan el mismo paquete a partir de la misma revisión | Uno guarda y luego el otro envía su edición anterior | BRICA conserva el primer cambio y pide al segundo administrador recargar el paquete y revisar sus cambios. |
| CA-31 | Un cliente registrado | Registro dos paquetes con nombres distintos para ese cliente | Ambos aparecen como alternativas aplicables y el usuario elige uno. BRICA rechaza un nombre exactamente duplicado para el mismo cliente; no recorta espacios ni convierte mayúsculas para comparar y no exige operación/etapa. |
| CA-32 | Un paquete ya registrado | Cambio su nombre o composición, o intento reasignarlo a otro cliente | BRICA acepta los cambios válidos de nombre o composición y rechaza la reasignación. Para otro cliente se registra un paquete nuevo. |
| CA-33 | Dos confirmaciones distintas y válidas para la misma plantilla | Se procesan de forma simultánea | Cada registro exitoso obtiene una versión distinta. Solo la última versión registrada correctamente queda seleccionable. |

### HU-06 · Revisar y retomar la configuración
Como administrador de BRICA, quiero revisar la configuración de plantillas y paquetes, conservar la información entre versiones y recuperar configuraciones incompletas, para garantizar la integridad de los datos.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-34 | Un paquete con plantillas agregadas en distinto orden del alfabético | Consulto su composición | BRICA muestra las plantillas en orden ascendente por nombre. |
| CA-35 | Una obra y su cliente con datos conocidos | Registro una versión válida y, en una verificación separada, edito la composición de un paquete | Compruebo manualmente que cada cambio documental se guardó y que los datos de negocio de la obra y del cliente conservan sus valores. |
| CA-36 | Una primera versión con campos detectados, o una revisión con campos conservados, nuevos y retirados | Reviso la obligatoriedad | En la primera versión clasifico todos los campos. En una revisión, BRICA propone la obligatoriedad anterior para los campos conservados, pide clasificar los nuevos y excluye los retirados. Puedo revisar y ajustar la propuesta antes de confirmar. La versión anterior conserva su configuración. |
| CA-37 | Un DOCX válido sin marcadores | Intento registrarlo | BRICA informa el hallazgo y permite confirmar expresamente su registro como documento estático o volver para corregir el archivo. Si confirmo, queda disponible como documento sin sustituciones. |
| CA-38 | Un paquete con captura incompleta | Lo guardo como borrador, cierro la aplicación y lo vuelvo a abrir | Recupero la captura para completarla o descartarla. El borrador permanece fuera de la lista de generación hasta que lo confirme y pase las validaciones. |
| CA-39 | Una plantilla con dos versiones, o una plantilla eliminada de forma lógica | Abro una versión histórica | BRICA muestra su número, estado, campos, marcadores y obligatoriedad en modo de solo lectura. En un documento estático indica que no tiene campos. |
| CA-40 | Un campo referenciado por una versión | Consulto su identificación o intento renombrarlo en el flujo nuevo | BRICA muestra la clave como nombre visible y conserva clave, tipo, origen y asociaciones; el catálogo nuevo no ofrece renombrado independiente. |
| CA-41 | Una fecha o un importe válido | Consulto el valor y solicito resolución de una selección | BRICA valida el tipo y devuelve fecha española o importe preciso, además de la presentación separada del importe. No modifica el origen. El motor futuro deberá consumir explícitamente ese contrato. |
| CA-42 | Los permisos de consulta y administración configurados en el servidor | Verifico las consultas y cambios con usuarios con y sin los permisos correspondientes | El servidor permite las acciones autorizadas y rechaza las demás. La aceptación completa queda pendiente mientras esta verificación no pueda ejecutarse con permisos reales. |

### HU-07 · Corregir captura y administrar versiones/adjuntos

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-43 | Un formulario de cliente, obra, paquete, plantilla o versión | Escribo/pego caracteres no permitidos o confirmo un nombre sin letras | La interfaz bloquea los caracteres prohibidos y la validación rechaza el nombre completo inválido, también por API directa. Ñ/ñ sí se admiten; acentos y espacios no. |
| CA-44 | Una obra con fechas opcionales | Selecciono fechas por calendario o las escribo | Se conservan las fechas exactas; ambas terminaciones se rechazan si son anteriores al inicio, sin persistencia parcial. |
| CA-45 | Un paquete del cliente y uno exclusivo de una obra confirmada | Abro la documentación del cliente o de otra obra del mismo cliente | El cliente ve ambos ámbitos identificados; las otras obras pueden reutilizar solo el paquete de cliente, sin reasignarlo. |
| CA-46 | Un paquete borrador o confirmado con plantillas existentes | Creo y agrego otras plantillas | No se reemplazan las anteriores. La composición confirmada solo incorpora plantillas con versión elegible y conserva control de concurrencia. |
| CA-47 | Una versión registrada | Renombro o elimino lógicamente su registro | Renombrar conserva bytes/hash/campos/número. Eliminar conserva el historial y excluye su uso nuevo; se elige la anterior disponible y no se reutilizan números. |
| CA-48 | Un adjunto de obra y usuarios con/sin escritura | Confirmo eliminarlo o intento hacerlo usando otra obra | Solo se permite con escritura y pertenencia correcta. Se oculta de consultas/descargas ordinarias sin borrar bytes ni afectar otros adjuntos. |

## 7. Requisitos no funcionales y restricciones

- Los marcadores tienen la forma exacta `{{CLAVE}}`; clave máxima de 50 caracteres y marcador máximo de 54. Se admiten cuerpo, tablas, encabezados y pies; se rechazan marcadores desconocidos o en regiones no soportadas.
- Los valores se clasifican como presentes, faltantes o inválidos. Un error de lectura se informa como error, no como dato faltante. Cero y `false` pueden ser valores válidos.
- Un paquete borrador es persistente y no se ofrece para uso. Los cambios en paquetes confirmados respetan `If-Match`; el cliente queda fijo.
- El registro de versión es idempotente y recuperable. Las operaciones pendientes o fallidas no habilitan versiones incompletas.
- Las generaciones futuras conservarán el paquete y las versiones realmente usadas. Esta spec define el contrato; no implementa ese historial.

## 8. Decisiones abiertas

- Despliegue, respaldo y retención de archivos: pendientes de decisión; no forman parte de la aceptación funcional de esta entrega.
- Alineación de las referencias históricas a operación/etapa: pendiente de revisión separada.

## 9. Condición de aceptación

La aceptación requiere un recorrido en Windows/Word con datos de prueba: configurar campos, registrar dos versiones, confirmar un paquete, consultar valores, comprobar permisos y verificar que los cambios documentales no alteren Cliente/Obra (CA-35). Revisar también captura, adjuntos, ámbito de paquetes y eliminación lógica (CA-43–CA-48). Registrar actor, entorno, resultado y evidencia en [review.md](review.md). La aceptación sigue pendiente hasta completar este recorrido. Generación e historial real dependen de Spec 002.

**Fuentes del proyecto** · ERS_BRICA.docx, DAS_BRICA.docx, DiccionarioDatos_BRICA.docx y constitución de BRICA. Los identificadores CD y CA son locales a esta spec.
