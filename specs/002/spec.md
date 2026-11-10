# Spec 002 — Motor documental de BRICA

**Estado** · Propuesta para revisión  
**Resultado esperado** · Un usuario carga el PDF de un contrato, BRICA rellena los campos reconocidos en el formulario de una obra, el usuario confirma los datos y el motor genera un documento descargable a partir de una plantilla.

## 1. Alcance de la primera entrega

El **PDF de entrada es el documento fuente**. BRICA extrae información de ese archivo y la propone en el formulario. La **plantilla es un archivo independiente** que determina el formato de salida y los campos que se deben sustituir. Esta interpretación sigue la separación entre RF-04 y RF-06 del ERS.

Se propone un recorrido completo para **una obra y su cliente ya registrados**. La primera entrega procesa contratos PDF con texto seleccionable de al menos un formato acordado. El usuario puede corregir y completar la información, guardar los datos de la obra, previsualizar la salida y generar un **DOCX** mediante un paquete con una plantilla versionada disponible para el cliente. El paquete, la plantilla, los campos y su versión podrán quedar preparados mediante la configuración que adopte el plan técnico. En el flujo de generación el usuario debe poder identificar y elegir la opción disponible.

PDF es el formato de entrada solicitado. DOCX de salida, un formato inicial de contrato y una obra ya registrada son recortes propuestos para esta entrega. Quedan para entregas posteriores el OCR de documentos escaneados, la importación de presupuestos Excel, el registro de una obra nueva desde PDF, la administración completa de plantillas y paquetes, la reanudación de generaciones parciales, las estimaciones, los convenios, el cierre de obra y la exportación en lote.

## 2. Actores y precondiciones

- El usuario interno cuenta con acceso a la obra y con los permisos correspondientes para actualizarla y generar documentación.
- La obra está asociada con un cliente. El usuario selecciona la obra antes de cargar el contrato.
- Hay un paquete compatible con el cliente y con la operación, con una plantilla disponible y al menos una versión registrada.
- El catálogo identifica los campos reconocidos y el tipo de cada valor. Cada versión declara sus marcadores y qué campos exige para finalizar.

## 3. Historias y criterios de aceptación

### HU-001 · Cargar un contrato y rellenar el formulario

Como usuario interno, quiero cargar un contrato PDF en una obra para recuperar los datos que BRICA pueda reconocer y reducir la captura manual.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-01 | Una obra existente y un contrato válido del formato acordado con número, nombre de obra y fechas reconocibles | Cargo el archivo | BRICA propone los valores en los campos correspondientes, muestra su origen y mantiene sin cambios los datos guardados. |
| CA-02 | Un campo ausente, inválido o con dos valores incompatibles | Termina la extracción | El campo queda señalado para revisión y puedo capturarlo o corregirlo. El sistema conserva las demás propuestas válidas. |
| CA-03 | Un valor extraído que difiere del que ya está guardado | Reviso las propuestas | Puedo comparar los valores y decidir cuál conservar. La carga del PDF por sí sola no modifica la obra ni su cliente. |
| CA-04 | Un PDF escaneado sin texto, cifrado, dañado o un archivo que no es PDF | Intento cargarlo | BRICA explica la causa que pudo identificar y ofrece volver a seleccionar el archivo o continuar con captura manual. |
| CA-05 | Una revisión con valores válidos corregidos o aceptados | Confirmo el guardado | BRICA guarda los valores confirmados en la fuente común de la obra y vincula el contrato cuando se aceptó un PDF válido. Si cancelo, los datos vigentes permanecen intactos. |

### HU-002 · Generar documentación con los datos confirmados

Como usuario interno, quiero revisar y descargar el documento que BRICA completa con los datos de la obra.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-06 | Una obra con datos confirmados y una versión de plantilla compatible | Selecciono el paquete y la versión, y solicito la vista previa | Veo el resultado de sustituir los marcadores por los valores guardados. La vista previa conserva el formato acordado y no crea un documento definitivo. |
| CA-07 | Una versión con dos campos obligatorios sin valor válido | Solicito finalizar | BRICA identifica ambos campos y detiene la generación definitiva hasta que los corrija y vuelva a validar. |
| CA-08 | Una versión que contiene un marcador desconocido o cuyo archivo ya no está disponible | Solicito procesarla | BRICA identifica la versión y el problema, y permite regresar a la selección. El intento no crea un documento definitivo. |
| CA-09 | Una vista previa válida que ya revisé | Confirmo la generación | Puedo descargar un DOCX que abre correctamente y contiene los valores revisados. El historial muestra la obra, el paquete y la versión exacta de la plantilla. |
| CA-10 | Un documento definitivo ya generado | Corrijo un dato de la obra y genero de nuevo | Obtengo una salida con el dato actualizado. El archivo anterior y su vínculo con la versión original se conservan. |
| CA-11 | Una vista previa emitida y datos de la obra que cambiaron después | Intento confirmar la generación | BRICA solicita una vista previa actualizada antes de finalizar, de modo que la salida coincida con lo que revisé. |
| CA-12 | Un fallo durante el renderizado o el almacenamiento | Intento finalizar | BRICA informa qué operación falló y permite reintentar. El historial no presenta archivos incompletos como documentos definitivos. |

## 4. Requisitos de la Spec 002

| ID | Comportamiento requerido | Trazabilidad |
|---|---|---|
| MD-001 | Recibir el PDF para una obra concreta, comprobar que puede procesarse e indicar el resultado de la carga. | RF-04, RNF-03, CU-01 como referencia de extracción |
| MD-002 | Extraer los valores reconocibles como propuestas editables. Mostrar por campo el valor detectado y, cuando pueda identificarse, la página y el fragmento del que provino. Señalar valores ausentes, inválidos o ambiguos. | RF-04, RF-05, RNF-04 |
| MD-003 | Permitir corregir, completar, aceptar o descartar propuestas. Guardar los valores aprobados usando las validaciones del servidor y la fuente común de la obra. Conservar los controles de actualización existentes. | RF-03, RF-05, RF-09, CU-02, RNF-02 |
| MD-004 | Asociar el PDF aceptado con la obra mediante su registro de contrato y archivo. Mantener los binarios en almacenamiento administrado y los metadatos en ARCHIVO, con ruta relativa y hash SHA-256 según el DAS. | RF-04, DAS Documentación Fuente y Gestión de Archivos |
| MD-005 | Identificar un paquete compatible y seleccionar la plantilla y la versión disponible. Resolver los marcadores por su correspondencia con el catálogo de campos y los datos confirmados de la obra. | RF-06, RF-08, CU-04, RES-02 |
| MD-006 | Validar disponibilidad de la versión, marcadores reconocidos y todos los campos obligatorios. La plantilla podrá tener campos opcionales cuyo tratamiento esté definido en su configuración. El sistema debe informar qué campo requiere atención y cómo continuar. | RF-18, RNF-03, RNF-04, CU-04, CU-08 como referencia de marcadores |
| MD-007 | Mostrar una vista previa con el formato y los valores que se emitirán. Tras confirmar, generar el DOCX y registrar los valores usados o una referencia inmutable a ellos, la obra, el paquete, la versión y el archivo resultante. Si cambian los datos o la selección, se requiere volver a revisar. | RF-06, RF-16, RF-17, RNF-01, CU-04, RES-02 |
| MD-008 | Permitir descargar el documento definitivo. Una nueva generación conserva la anterior y la descarga entrega el archivo almacenado. | RF-09, RF-17, RF-24 en el alcance de un documento definitivo |
| MD-009 | Aplicar las comprobaciones de acceso en el servidor para cargar, confirmar, previsualizar, generar y descargar. Si una operación falla, ofrecer un mensaje y una acción siguiente sin registrar una salida parcial como definitiva. | RF-26, RNF-03, DAS Seguridad y Gestión de Archivos |

## 5. Datos del primer formato

Estos campos son candidatos para el primer contrato. Su obligatoriedad para finalizar proviene de la versión de la plantilla elegida. Las validaciones para guardar la obra provienen de sus reglas vigentes.

| Dato de la obra | Regla de extracción y validación |
|---|---|
| Número de contrato | Conservar letras, ceros iniciales y separadores. Identificarlo como número contractual en la fuente. |
| Nombre de la obra | Capturar el nombre o la denominación de la obra según el formato acordado. Si difiere del valor registrado, mostrar la comparación. |
| Contratista | Usar el valor identificado como contratista. Mantener separada la identidad del cliente seleccionado en la obra. |
| Fecha de inicio y fecha de terminación | Interpretar ambas en sus roles contractuales. Señalar fechas ambiguas y validar la coherencia antes de guardar. |
| Monto antes de IVA, IVA y monto contratado | Registrar solo los importes cuyo concepto se pueda identificar. Cuando existan los tres, señalar cualquier inconsistencia. La ausencia de un importe queda pendiente de confirmación. |

Los nombres anteriores corresponden a atributos de OBRA en el Diccionario de Datos. El plan técnico deberá verificar cómo se representan en el código y evitar duplicar fuentes vigentes. La plantilla deberá usar los identificadores y marcadores de su catálogo real.

## 6. Límites de diseño y decisiones pendientes

- Mantener el análisis del PDF y el renderizado en infraestructura, con orquestación en la capa de aplicación. El cliente JavaFX opera mediante la API y el dominio recibe valores de negocio.
- Tratar el PDF como contenido de entrada. Validar tamaño, legibilidad y formato con límites configurados antes de procesarlo. Mantener el documento dentro del entorno de BRICA para esta primera entrega.
- Confirmar el primer formato con un contrato real anonimizado y una plantilla representativa. Definir qué etiquetas y variantes se reconocen, qué campos son obligatorios y cómo se muestran los importes y las fechas en esa plantilla.
- Resolver en el plan técnico las bibliotecas de extracción y renderizado, el mecanismo de vista previa y la configuración inicial del paquete. Los cambios de esquema, si se requieren, deberán seguir Flyway.
- El CU-07 del ERS referencia RF-28, que no está definido en el listado de requisitos entregado. Esta spec vincula la descarga a RF-24 y deja la referencia del CU pendiente de corrección.

## 7. Condición de entrega

La primera entrega se acepta cuando un usuario completa el recorrido con el contrato y la plantilla acordados. Debe poder cargar el PDF, revisar el formulario rellenado, corregir un dato, confirmarlo, revisar la vista previa y descargar un documento que conserva el formato y contiene los valores confirmados. La evidencia incluirá al menos el recorrido exitoso, un campo obligatorio faltante, un PDF que no se pueda extraer y una nueva generación que preserve la anterior.

**Fuentes del proyecto** · ERS_BRICA.docx, DAS_BRICA.docx, DiccionarioDatos_BRICA.docx y constitución de BRICA. Los identificadores MD y CA pertenecen a esta spec.
