# Spec 001 — Plantillas, paquetes y campos de referencia de BRICA

**Estado** · Alcance actualizado con decisiones del responsable; implementación y aceptación parciales

**Resultado esperado** · Un usuario interno con permisos puede registrar campos reutilizables, cargar y versionar una plantilla, consultar los valores que tomarían sus campos para una obra y configurar un paquete documental asociado a un cliente, sin operación/etapa ni secuencia obligatoria de generación.

## 1. Alcance

Esta entrega implementa **la configuración documental**. Empieza con datos de obra y cliente ya registrados y termina con un paquete disponible que el motor podrá consumir. La consulta de valores muestra el dato real que corresponde a cada campo para una obra seleccionada. La salida de esa consulta es una lista de valores, orígenes y estados de resolución.

El ERS sustenta el registro y versionado de plantillas en CU-08, y la creación y modificación de paquetes en CU-03. El DAS y el Diccionario de Datos sustentan la definición de campos y su resolución. La administración del catálogo desde la interfaz y la consulta de valores por obra son propuestas para hacer operativa esta entrega.

La primera entrega admite plantillas DOCX y permite registrar campos a partir de atributos aprobados de Obra y Cliente. La extracción de PDF, el renderizado de documentos y las reglas de estimaciones, convenios y cierre corresponden a entregas separadas. La Spec 002 cubre el motor documental y esta entrega se puede probar de forma independiente del motor. Su aceptación completa depende de la configuración de permisos en el servidor.

### Alcance vigente de las correcciones aprobadas

- Ofrecer todos los atributos fijos del Diccionario para Cliente y Obra: identificadores, nombres, representante legal, número de contrato, contratista, ubicación, descripción, fechas, estado e importes. La lista cerrada está en `ReferenceAttribute`; nunca evaluar rutas libres. `adicionales` se implementa mediante definiciones globales TEXT/DATE/AMOUNT y valores independientes por obra.
- Persistir importes precisos en PostgreSQL `NUMERIC` sin escala fija. La resolución conserva el valor preciso en `textValue` y entrega por separado `presentationValue`: el último decimal de la magnitud, si es 5 o mayor, se redondea hacia arriba eliminando una posición; si es menor, se conserva. Mantener el signo: `1.184 → 1.184`, `1.186 → 1.19`, `-1.186 → -1.19`. No se limita a dos decimales ni se introduce conversión de moneda o cálculos fiscales.
- Aceptar fechas ISO y `dd/MM/aaaa` para adicionales y capturarlas en el diálogo de obra; la resolución usa texto español, por ejemplo `16 de julio del 2005`. Estados: En Proceso, En pausa, Terminada y Cancelada. Las obras existentes se inicializan como En Proceso.
- Los paquetes siguen asociados a un cliente. Se elimina completamente operación/etapa como clasificación, filtro o condición de uso. Sus plantillas pueden utilizarse en cualquier orden y momento, siempre que el usuario tenga permiso y la selección sea elegible; esto no elimina validaciones de datos, estado o versión.
- D-04 aprobada: usar `docxtpl` mediante un proceso Python local invocado únicamente por el backend, con protocolo JSON y tiempo límite. Python no accede a PostgreSQL, y JavaFX continúa usando exclusivamente la API. No se incorpora otro servicio ni el motor de Spec 002.
- D-06 acotada: probar archivos construidos mediante almacenamiento en una ruta configurable del servidor, aislado detrás de un puerto. La raíz, el respaldo, la retención y el despliegue definitivos se tratarán por separado. Las pruebas locales no equivalen a aceptación del servidor de producción.
- Construir conjuntamente una plantilla distinta del acta demostrativa existente. El responsable dispone de un presupuesto destinado a una futura extracción de datos; no se ha identificado ni analizado su archivo en esta tarea y no constituye una plantilla de salida. El formato y la obligatoriedad de la nueva plantilla siguen pendientes.
- Los cambios contractuales por convenio quedan fuera del flujo. La captura documental de obra tiene su propio endpoint con `If-Match`, sin alterar el contrato de corrección de información general. Un adicional sin capturar resuelve a `MISSING` y permite comprobar CA-04/10, sin inventar nulos en nombres obligatorios.
- Crear/editar campos, plantillas y paquetes, capturar datos de obra y consultar valores mediante diálogos. El paquete puede iniciarse seleccionando cliente u obra por nombre; al usar obra, el servidor deriva su cliente. Exigir `CLIENTS_READ`/`WORKS_READ` para sus selectores, además del permiso documental correspondiente.
- Nombres de plantilla únicos globalmente por comparación exacta y sensible a mayúsculas, también entre eliminadas. Ocultar eliminadas en listas ordinarias y ofrecer consulta histórica explícita. Un campo solo se elimina físicamente si ninguna versión lo referencia; preservar valores adicionales históricos de obra si desaparece su definición.

### Correcciones aprobadas de navegación y permanencia (2026-11-13)

Estas decisiones sustituyen el flujo global de Documentación y el uso de una instancia temporal como aplicación habitual:

- Alta de obra con cliente, nombre y descripción; ubicación y fechas son opcionales y no se inventan para completar el alta. El resto de información se captura posteriormente y conserva validación por tipo y revisión.
- Obras muestra únicamente Cliente/Nombre/Descripción y cuatro acciones por fila: editar información completa, asistente de adjuntos → información → revisión, consultar información/adjuntos y configurar paquetes → plantillas → versiones. Barra superior: Nueva obra, Actualizar, Eliminar y Generar documentos (sin implementar generación en esta iteración).
- Crear desde obra fija el ámbito a esa obra y deriva su cliente en servidor. Crear desde cliente fija ese cliente sin asignar una obra arbitraria. No incorporar plantillas de otra obra ni otro cliente; los paquetes de cliente pueden aplicarse a obras del mismo cliente. Se preserva el legado sin reasignarlo automáticamente.
- La navegación global se llama Catálogo de campos y solo administra definiciones. Los campos fijos se predefinen internamente; el alta normal pide clave/tipo para adicionales, no un origen técnico. El nombre visible coincide con la clave. Compatibilidad interna de definiciones históricas no habilita editar rutas libres desde la interfaz.
- El asistente adjunta DOCX/PDF/XLSX de hasta 10 MiB, sin extracción. Se guardan metadatos, hash, bytes y relación con obra en almacenamiento persistente. La validación de plantillas DOCX sigue limitada a 1 MiB y no se confunde con adjuntar documentos base.
- Cerrar un formulario confirmado no espera una respuesta de red. Mostrar Guardando hasta confirmación real; ante fallo conservar y recuperar la captura, no anunciar éxito ni reintentar automáticamente. Medir el cierre local con objetivo inferior a 100 ms; no prometer latencia de red/BD inferior a ese tiempo.
- Anterior/Siguiente solo aparecen cuando se comprueba la existencia de la página correspondiente.

Las referencias heredadas a operación/etapa en ERS/DAS/Diccionario o Spec 002 deben alinearse mediante un cambio separado. La identidad que agrupa plantillas alternativas del mismo documento (D-12) es el **tipo de documento** ya existente; no se crea otra entidad. Las nuevas tablas del flujo se incorporan mediante migraciones posteriores a V6, sin modificar migraciones previas ni fuentes DOCX.

### Correcciones aprobadas de nombres y ciclo de vida (2026-11-15)

Estas decisiones prevalecen sobre las restricciones anteriores de esta spec cuando difieran:

- Los nombres de **clientes, obras, paquetes, plantillas y versiones** solo admiten `A–Z`, `a–z`, `Ñ`, `ñ`, números ASCII y `_`. Deben contener al menos una letra y no comenzar por `_`. Se rechazan espacios, acentos, puntos, guiones y nombres como `123_456`. Se permite comenzar por número si hay una letra después. Se mantienen los límites existentes: 255 para clientes/obras y 150 para paquetes/plantillas/versiones. No se recorta ni transforma el nombre automáticamente. La interfaz rechaza las ediciones/pegados con caracteres prohibidos; la validación completa también se ejecuta en el servidor. No aplicar esta regla al nombre original de archivos, representante legal, descripción, contratista ni a las claves canónicas de campos, que conservan sus reglas propias.
- Las fechas de obra siguen siendo opcionales. La captura incorpora calendarios y entrada estricta ISO o `dd/MM/aaaa`. La terminación contractual y la real, si existe inicio, **no pueden ser anteriores al inicio**; igualdad permitida. La petición inicial de permitir fechas invertidas quedó expresamente corregida por el responsable.
- Iconos de obra vectoriales (edición, asistente/documento, consulta/ojo y configuración/carpeta), independientes de fuentes emoji, con tamaño fijo, alineación, ayuda emergente, nombre accesible y estados por permisos.
- Un paquete **de cliente** es reutilizable en obras actuales y posteriores de ese mismo cliente, sin copiarlo ni cambiar su ámbito. Los paquetes exclusivos de obra confirmados también son visibles desde el cliente, indicando su obra, pero no se habilitan para otras obras. Editar desde otro contexto nunca reasigna el ámbito.
- Se pueden crear/agregar varias plantillas tanto antes como después de confirmar el paquete, sin reemplazar las ya vinculadas. En un confirmado, la plantilla nueva se prepara en su ámbito y se incorpora únicamente después de tener una versión elegible. Si se cancela esa preparación, la composición anterior permanece válida y la plantilla puede retomarse con Agregar plantilla existente. Las modificaciones de composición mantienen `If-Match`.
- Cada versión tiene un **nombre obligatorio independiente de su número y archivo**. Se captura al registrar y puede renombrarse sin modificar archivo, hash, campos o número. El nombre forma parte de la identidad de la solicitud de registro para idempotencia. V14 asigna `Version_N` a versiones existentes y conserva los registros pendientes para recuperación.
- La eliminación de versiones es lógica y requiere confirmación en escritorio. Se conservan archivos, campos y referencias históricas; estas referencias siguen protegiendo el catálogo. Para nuevas selecciones se usa la versión no eliminada de mayor número, o se excluye la plantilla si no queda ninguna. Nunca reutilizar ni renumerar números; una nueva versión se numera sobre el máximo histórico, incluidas las eliminadas. La consulta histórica permanece disponible y una versión eliminada no se renombra.
- La eliminación de documentos de este bloque se refiere **solo a adjuntos de obra**: confirmación, permiso `WORKS_WRITE`, validación de pertenencia y eliminación lógica. Desaparecen de consulta/descarga ordinaria, pero se conserva el archivo y sus metadatos. No se implementa purga física ni eliminación de documentos generados. Los archivos de versiones se gestionan mediante su versión, no como archivos independientes.
- El responsable autorizó limpiar los registros incompatibles de prueba de `brica`. Tras conocer sus dependencias eligió limpieza lógica de clientes/obras/plantillas, retirando solamente los cinco paquetes incompatibles y sus vínculos. Conservar cuentas, catálogo, versiones, adjuntos y documentos locales; los paquetes con nombre válido permanecen aunque su cliente quede eliminado. No convertir esta limpieza puntual en una migración o un borrado automático de arranque.

## 2. Actores y precondiciones

- El acceso se concede globalmente por rol. El rol que tenga permiso de consulta de valores puede resolver campos de cualquier obra registrada. Para administrar campos, plantillas, versiones y paquetes se requiere el permiso de administración documental.
- Roles estándar: Gerencial (4) y Administrativo (3) tienen los mismos ocho permisos actuales; Practicante (2) e Invitado (1) solo los cuatro permisos de lectura. La jerarquía no concede permisos implícitos. Las propuestas y su aprobación quedan fuera de este sprint; en el futuro podrán aprobar Administrativo y Gerencial. No se permite escritura directa a Practicante mientras ese flujo no exista.
- El 2026-11-13 se autorizó aplicar migraciones a la base persistente `brica` y sustituir las tres cuentas antiguas por `UsuarioGerencial`/`UsuarioAdministrativo`, con respaldo y sin borrar negocio. Esta operación local es explícita, no un seed automático ni un restablecimiento al arrancar. Las pruebas de integración siguen usando bases desechables; la aplicación habitual usa Compose y volúmenes persistentes.
- El servidor comprueba la autenticación y el permiso correspondiente tanto desde la interfaz como ante llamadas directas a la API. La resolución requiere una obra existente y toma el cliente de su relación registrada. El registro de una plantilla puede realizarse sin seleccionar una obra.
- Para confirmar un paquete y ofrecerlo en generación, debe existir el cliente seleccionado y al menos una plantilla elegible con una versión disponible. Un paquete incompleto se puede guardar como borrador.
- La autenticación y los permisos de consulta, administración documental y roles estándar se comprueban con sesiones HTTP reales y PostgreSQL desechable. La carga JavaFX se verifica por separado; la aceptación del recorrido Windows/Word no se sustituye por esas pruebas. La evidencia vigente y las comprobaciones manuales pendientes están en [review.md](review.md).

## 3. Modelo funcional

| Concepto | Responsabilidad | Ejemplo |
|---|---|---|
| Campo de referencia | Define una clave canónica única, un nombre visible, un tipo, un origen lógico y la ruta de un atributo admitido. | `NOMBRE_OBRA` representa el nombre registrado de la obra. |
| Valor de referencia | Resultado de resolver el campo para una obra y su cliente asociado. Se obtiene de la fuente vigente. | Para la obra 17, `NOMBRE_OBRA` resuelve a «Rehabilitación de la cancha». |
| Plantilla documental | Identidad reutilizable de un formato. Tiene nombre, tipo de documento y estado [Vigente, borrador, No disponible]. | Acta de inicio. |
| Versión de plantilla | Archivo registrado y relación con los campos, marcadores y obligatoriedad usados en esa revisión. | Versión 2 del acta, con `{{NOMBRE_OBRA}}` obligatorio. |
| Paquete documental | Conjunto de plantillas asociado a un cliente, sin clasificación temporal ni secuencia obligatoria. | Paquete documental del cliente A. |

Los estados **Vigente**, **Borrador** y **No disponible** son válidos. Todos permiten consultar y administrar la plantilla, incluido agregar versiones. Solo **Vigente**, no eliminada y con última versión completada verificable, permite confirmar su incorporación a un paquete y seleccionarla para uso. Una plantilla que cambia de estado sigue visible en los paquetes existentes, pero queda fuera de nuevas selecciones. La eliminación lógica es independiente y deja todo el registro en solo lectura. En esta spec, «activa» significa «Vigente y no eliminada».

La **obligatoriedad se define por versión y campo**. Un campo puede ser obligatorio en un acta y opcional en otro documento. El paquete reúne **identidades de plantilla**. Para cada documento de una generación, el usuario interno elige una sola plantilla activa de entre las aplicables. La única versión seleccionable para una generación nueva es la revisión más reciente registrada correctamente y no eliminada de esa plantilla. El sistema muestra su número para que el usuario confirme la selección. Cambiar de plantilla antes de confirmar sustituye la selección anterior. La generación confirmada conserva la plantilla y la versión exactas que se utilizaron.

## 4. Historias y criterios de aceptación

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

## 5. Requisitos y trazabilidad

| ID | Comportamiento requerido | Fuente |
|---|---|---|
| CD-001 | Listar y registrar campos mediante una clave única y un nombre visible. El usuario elige un atributo de una lista aprobada y el servidor determina su origen y tipo. Permitir cambios descriptivos sin alterar el significado de una clave vinculada. | DAS Gestión de campos de referencia, Diccionario CAMPO_REFERENCIA, propuesta de administración |
| CD-002 | Resolver los valores desde los datos vigentes de la obra y el cliente asociado. Mostrar el valor como texto y distinguir un dato faltante de un valor inválido. Aplicar una conversión común para la consulta y la sustitución documental. La corrección se guarda en su entidad de origen mediante el flujo correspondiente. | RF-03, RF-05, RNF-02, RNF-04, DAS Gestión de campos de referencia |
| CD-003 | Analizar cada DOCX antes de confirmar, incluidos cuerpo, tablas, encabezados y pies. Relacionar sus marcadores con el catálogo, bloquear los desconocidos, mal formados o fuera de las regiones admitidas, y guardar la obligatoriedad por versión con revisión de los campos nuevos y de la configuración heredada. Si no hay marcadores, informar al usuario y solicitar confirmación explícita para registrar el DOCX como documento estático. Al confirmar, volver a analizar y comprobar que el archivo y sus asociaciones coincidan con lo revisado. | CU-08, RF-13, Diccionario VERSION_PLANTILLA_CAMPO |
| CD-004 | Registrar revisiones como versiones independientes, conservar su archivo y configuración y permitir consultar su detalle. Solo la última revisión registrada correctamente de una plantilla activa puede seleccionarse para nuevas generaciones. Las revisiones anteriores y las plantillas eliminadas se conservan en modo de consulta histórica. Almacenar el archivo en el servidor y sus metadatos en ARCHIVO según el DAS. | RF-14, RES-03, CU-08, DAS Gestión de plantillas y Despliegue |
| CD-005 | Crear, consultar y modificar paquetes asociados a un cliente, sin operación/etapa ni secuencia de generación. Permitir varios paquetes con nombres distintos mediante comparación exacta dentro de cada cliente. Los paquetes incompletos se pueden guardar como borradores persistentes. Después de confirmar el paquete se pueden cambiar nombre y composición. Listar las plantillas por nombre. Validar la revisión editada para preservar cambios concurrentes. El historial de generación conserva la referencia al paquete y las plantillas y versiones utilizadas. | RF-11, RF-12, RF-15, CU-03, Diccionario PAQUETE_DOCUMENTAL y PAQUETE_PLANTILLA; decisión vigente D-02/D-07, pendiente alinear fuentes heredadas |
| CD-006 | Listar las opciones aplicables por obra, según su cliente asociado. Resolver los campos por obra, plantilla y versión seleccionadas, con sus valores como texto y sus estados. Entregar los documentos estáticos al flujo documental sin campos para sustituir. Validar el paquete aplicable y una única selección vigente de plantilla y versión por documento al preparar una nueva generación, sin imponer orden ni momento de uso. | RF-06, RF-08, RF-18 y CU-04 como contrato para la generación posterior; decisión vigente D-02 |
| CD-007 | Comprobar en el servidor la autenticación, el permiso global de consulta de valores y el permiso de administración, según la operación. Presentar errores que identifiquen el dato, archivo o selección a corregir. Registrar la operación de guardado con un estado recuperable. Mantener fuera de uso las versiones pendientes o fallidas y evitar duplicados al repetir una confirmación. | RF-26, RNF-03, CU-08 FE02 y DAS Protección de información |

## 6. Reglas de alcance para la primera implementación

- Usar marcadores con la forma `{{CLAVE}}`, por ejemplo `{{NOMBRE_OBRA}}`. Cada aparición de una clave se vincula con el mismo campo. Se admiten párrafos del cuerpo, celdas de tablas, encabezados y pies de página, incluidas las variantes de encabezado o pie que use la plantilla. Un marcador puede abarcar varios fragmentos de formato dentro de un mismo párrafo. El analizador y el motor deberán compartir las mismas reglas de reconocimiento y soporte. El diagnóstico distingue marcadores desconocidos, mal formados o ubicados fuera de las regiones admitidas.
- Ofrecer la lista cerrada completa de `ReferenceAttribute` y alta de adicionales tipados. El servidor determina tipo/origen/ruta de los atributos fijos y valida los valores por obra de cada definición adicional. No evalúa expresiones ni rutas suministradas por el usuario.
- Para nuevas generaciones, ofrecer únicamente plantillas activas del paquete aplicable y la última revisión registrada correctamente de cada una. Si la configuración cambia entre la selección y la solicitud de resolución, rechazar la selección anterior y pedir al usuario que revise las opciones vigentes.
- La eliminación de una plantilla es lógica. Su registro, versiones, archivos y vínculos permanecen para consulta histórica en modo de solo lectura. Una plantilla eliminada queda fuera de la configuración y de nuevas generaciones, y sus versiones quedan cerradas a modificación. Un campo referenciado debe seguir siendo resoluble para las versiones que lo utilizan.
- Conservar para cada generación el paquete de origen y las plantillas y versiones realmente utilizadas. Consultar aparte la composición vigente del paquete. Esta entrega no requiere guardar una copia histórica de todas las opciones del paquete.
- Separar dos consultas funcionales. La primera lista opciones por obra a partir de su cliente asociado. La segunda recibe obra, plantilla y versión, y devuelve por campo marcador, clave, tipo, valor como texto, origen, obligatoriedad y estado. En el flujo de generación, la segunda consulta también comprueba que el paquete seleccionado sea aplicable y que la plantilla y la revisión sigan siendo elegibles. La consulta administrativa de valores de una versión se puede realizar sin seleccionar un paquete.
- Clasificar como **presente** un valor disponible que cumple las reglas de su tipo y origen, como **faltante** un dato sin capturar o un texto vacío o con solo espacios, y como **inválido** un dato existente que incumple el tipo o una regla aplicable. El cero y el valor booleano falso pueden estar presentes cuando sean válidos. Un error al leer el origen se comunica como fallo de consulta.
- Validar que la clave tenga de 1 a 50 caracteres del conjunto A–Z, 0–9 y guion bajo, sin espacios. El marcador debe coincidir exactamente con `{{CLAVE}}`, incluyendo mayúsculas y llaves, y puede ocupar hasta 54 caracteres. Alinear el límite del marcador en el esquema y en la definición de VERSION_PLANTILLA_CAMPO del Diccionario de Datos antes de implementar. Si la columna ya existe, ajustar su longitud mediante Flyway.
- La consulta de valores es global para los roles con ese permiso. En esta entrega se aplica el mismo alcance de consulta a todas las obras registradas. Los permisos de consulta y administración se comprueban por separado.
- Mantener una sola fuente para cada dato de negocio. La consulta en configuración documental lee el valor. Los formularios de obra y cliente son los puntos para corregirlo en esta entrega.
- Los paquetes se asocian a un cliente concreto, según confirmación del responsable. Los paquetes generales sin cliente quedan fuera del alcance.
- Si falta una plantilla para componer el paquete, se puede abrir su registro y volver a la selección. El usuario autorizado puede guardar el paquete incompleto como **borrador persistente**, reabrirlo tras cerrar la aplicación y continuar o descartarlo. El borrador conserva la captura y las selecciones realizadas. Solo un paquete confirmado y válido se ofrece para generación.
- Mantener los módulos del cliente y del servidor, el acceso mediante API y las migraciones con Flyway. El proceso local Python aprobado se limita al adaptador documental; no introduce acceso directo desde JavaFX, un servicio independiente ni acceso a PostgreSQL desde Python.

### Confirmación, recuperación y edición

- Al confirmar una plantilla, el servidor analiza de nuevo el DOCX y compara su contenido y sus asociaciones de campos con los que revisó el usuario. Si cambió alguno, presenta el análisis actualizado con un aviso breve y vuelve a pedir confirmación. La obligatoriedad se toma de las opciones que el usuario confirmó para esos campos.
- El registro documental distingue **pendiente**, **completado** y **fallido**. El estado pendiente permite recuperar una operación interrumpida. Para completar el registro deben existir el archivo verificable, sus metadatos y todas las asociaciones. Si la recuperación no puede continuar con la información conservada, la operación queda fallida y el sistema explica cómo reintentar. Las versiones pendientes o fallidas permanecen fuera de la selección.
- Identificar cada confirmación de registro para reconocer su reenvío. Si la operación sigue pendiente, devolver su estado. Si terminó, devolver su resultado. Una nueva solicitud intencional usa otra identificación. Al confirmar cargas distintas para una misma plantilla, preservar números de versión únicos.
- Para cada cliente se permiten varios paquetes, con nombres distintos por comparación exacta. No convertir mayúsculas ni recortar espacios automáticamente; las demás validaciones de presencia y longitud siguen siendo necesarias. El usuario elige entre las alternativas aplicables. No existe catálogo de operaciones/etapas ni condiciones de secuencia o momento de generación.
- El nombre y la composición de un paquete son editables. Una vez confirmado, el cliente queda fijo. Para otro cliente se crea otro paquete. La edición compara la revisión que abrió el usuario con la vigente al guardar. Si difieren, BRICA rechaza el cambio y solicita recargar antes de editar otra vez.

### Orden, clasificación y consulta

- Mostrar las plantillas de un paquete en orden ascendente por nombre. Usar el identificador para desempatar nombres iguales. Ese orden es únicamente de presentación, no una secuencia obligatoria para generar documentos.
- Clasificar explícitamente todos los campos al registrar la primera versión. En una revisión, proponer la obligatoriedad de la versión anterior para los campos conservados, exigir una clasificación para los nuevos y excluir los retirados. El administrador puede ajustar la propuesta antes de confirmar.
- Si un DOCX válido no contiene marcadores, informar el hallazgo. El usuario puede volver para corregirlo o confirmar explícitamente su registro como **documento estático**. Un marcador inválido, desconocido o en una región sin soporte sigue bloqueando el registro. El documento estático se puede incluir en un paquete y el motor utiliza su contenido sin sustituciones.
- Permitir consultar el detalle de una versión con su número, estado, campos, marcadores y obligatoriedad. Las versiones históricas y las plantillas eliminadas se consultan en modo de solo lectura. El administrador puede cambiar el nombre visible de un campo conservando su clave, tipo y origen cuando ya esté vinculado.
- Usar la misma resolución tipada en consultas y selección de versiones. Mostrar la presentación del importe y su valor preciso separados. El motor y la generación definitiva no forman parte de esta entrega; no asumir que el renderizado ya consume esos valores.

## 7. Condición de entrega

Esta sección conserva la condición de aceptación del flujo completo. La implementación con casos construidos no sustituye el recorrido manual, la plantilla operativa conjunta ni el despliegue definitivo de D-06. Fechas/importes ya pertenecen al contrato de resolución; el historial de generaciones reales sigue diferido. Guía ejecutable: [manual-test.md](manual-test.md). El registro histórico de almacenamiento está en [review.md](review.md).

Con una obra y su cliente de prueba, registrar un campo de referencia y comprobar su valor, cargar un DOCX que lo utilice, registrar una segunda versión y crear un paquete que contenga la plantilla. Al consultar la configuración aplicable, deben verse la composición, la última revisión seleccionable de cada plantilla activa y los valores de los campos de la versión consultada. La aceptación también debe demostrar el rechazo de un marcador desconocido, la detección de un valor obligatorio faltante, la conservación de una versión anterior, la detección de marcadores en encabezados y pies, la selección única de plantilla y versión y el rechazo de solicitudes sin el permiso correspondiente.

Verificar manualmente la conservación de los datos de Obra y Cliente según CA-35. La aceptación completa requiere configurar y comprobar en el servidor los permisos de consulta y administración documental. El contrato con la Spec 002 deberá reflejar la entrega de valores como texto y el tratamiento de documentos estáticos.

**Fuentes del proyecto** · ERS_BRICA.docx, DAS_BRICA.docx, DiccionarioDatos_BRICA.docx y constitución de BRICA. Los identificadores CD y CA son locales a esta spec.
