# SPEC-002 — Motor documental de BRICA

**Estado:** Alineación de alcance iniciada; implementación pendiente de F0 y de la ficha técnica aprobada.

**Dependencia:** SPEC 001
**Resultado esperado vigente:** Un usuario autorizado incorpora o reutiliza documentos fuente PDF, XLS, XLSX o DOCX en una obra existente, extrae y revisa datos con evidencia, y confirma selectivamente los destinos autorizados. La revisión de extracción puede guardarse y retomarse. La generación documental queda fuera de esta SPEC.

> **Alineación F0 — 2026-11-17.** Las 33 decisiones del responsable y el alcance del [plan vigente](plan.md) prevalecen sobre el texto histórico posterior cuando difieran. En particular: no se requiere paquete, operación ni selección de versión para extraer; no se implementan renderizado, previsualización de salida, generación, exportación ni historial de generaciones. HU-004–HU-006 y CA-16–CA-30 se conservan como trazabilidad histórica de la futura generación, no como criterios de entrega de esta SPEC. La revisión de candidatos y el borrador de extracción son procesos distintos de una vista previa o borrador de generación. Los parámetros y dependencias indicados en §5 del plan siguen pendientes de aprobación; [Review.md](Review.md) registra avances y bloqueos.

## 1. Alcance de la primera entrega

La SPEC-002 implementará extracción y revisión de documentos fuente, reutilizando la configuración de campos definida en SPEC001 sin duplicar la administración de paquetes, plantillas, versiones ni campos. El alcance aprobado incluye PDF (con texto seleccionable o escaneados mediante OCR), Excel **XLS/XLSX** y **DOCX como fuente**. La importación de presupuestos comprende selección y agrupación de hojas, datos generales, partidas y conceptos con clave, descripción, unidad, cantidad, precio unitario e importe, así como subtotales, impuestos y totales cuando existan y sean identificables.

Los valores extraídos se presentan como propuestas revisables, con su procedencia cuando sea posible. El usuario corrige, completa y confirma antes de actualizar una **obra y un cliente ya registrados**; los conflictos con datos existentes no se resuelven sobrescribiendo automáticamente. Se conserva el archivo fuente y se reutilizan los datos confirmados, tanto campos simples como colecciones de conceptos presupuestales.

La salida de esta SPEC son datos y colecciones confirmados y consultables, sin producir documentos nuevos. La selección de plantillas, resolución para generación, previsualización de salida, producción DOCX/PDF, exportación e historial de generaciones se aplazan a una entrega posterior.

**Fuera de alcance:** alta automática de obras desde documentos; flujos especializados de estimaciones, convenios y cierre; interpretación universal de formatos arbitrarios; integraciones documentales externas. Estos procesos podrán consumir el motor en entregas posteriores.

## 2. Actores, precondiciones y límites

- **Actor:** usuario interno autorizado. La API del servidor comprueba acceso a la obra y a las operaciones solicitadas.
- La obra y su cliente existen. Puede iniciarse una generación con datos ya registrados, sin cargar archivos nuevos.
- La extracción no exige paquete documental, operación ni plantilla seleccionada. SPEC001 no utiliza operación/etapa como ámbito de paquete.
- El procesamiento se limita a formatos de entrada compatibles y a archivos que superen las validaciones de seguridad, tamaño y legibilidad configuradas. OCR y extracción pueden producir resultados parciales; nunca se consideran confirmados automáticamente.
- La SPEC anterior es propietaria de la configuración documental; esta SPEC consume sus definiciones y registra qué versiones exactas se usaron.

## 3. Historias y criterios de aceptación

### HU-001 · Incorporar y analizar documentos PDF

Como usuario interno, quiero adjuntar PDF digitales o escaneados a una obra para recuperar datos reconocibles y reducir la captura manual.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-01 | Una obra existente y un PDF con texto reconocible | Solicito analizarlo | BRICA propone los campos identificados, muestra procedencia cuando esté disponible y no modifica datos vigentes. |
| CA-02 | Un PDF escaneado o con páginas mixtas | Solicito analizarlo | BRICA utiliza OCR donde se requiera y presenta los resultados como propuestas sujetas a revisión. |
| CA-03 | Un dato ausente, ambiguo o inconsistente | Finaliza el análisis | Se identifica el dato que requiere revisión, conservando las propuestas válidas restantes. |
| CA-04 | Un PDF cifrado no accesible, dañado, ilegible o no admitido | Intento procesarlo | BRICA explica el impedimento, no registra valores no confirmados y permite reemplazar el archivo o continuar manualmente. |
| CA-05 | Un PDF válido aceptado por el usuario | Confirmo su incorporación | Se conserva el archivo original y su vínculo con la obra y el tipo de documento fuente correspondiente. |

### HU-002 · Importar presupuestos Excel

Como usuario interno, quiero importar presupuestos Excel con sus conceptos y montos para reutilizar información económica y técnica de la obra.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-06 | Un XLS/XLSX compatible con una o varias hojas | Lo cargo | BRICA muestra las hojas detectadas y permite elegir cuáles importar y cómo identificar cada presupuesto. |
| CA-07 | Una hoja con conceptos reconocibles | Solicito la extracción | Se proponen clave, descripción, unidad, cantidad, precio unitario e importe por concepto, conservando agrupaciones identificables. |
| CA-08 | Una hoja con subtotales, impuestos, totales o fórmulas | Reviso la importación | BRICA distingue conceptos de filas de cálculo o presentación y señala discrepancias de importes sin corregirlas silenciosamente. |
| CA-09 | Dos hojas que representan alternativas o presupuestos distintos | Confirmo la importación | Se conservan como presupuestos diferenciados y no se fusionan ni sustituyen automáticamente. |
| CA-10 | Una estructura no reconocible o valores ambiguos | Finaliza el análisis | Se indican hojas, filas o datos pendientes; puedo corregir, excluir o cancelar la importación sin perder información vigente. |

### HU-003 · Revisar y confirmar información extraída

Como usuario interno, quiero contrastar las propuestas con los datos existentes y decidir qué información guardar.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-11 | Datos extraídos de PDF o Excel | Abro la revisión | Veo valores propuestos, valores existentes y origen identificable, con posibilidad de corregir o descartar. |
| CA-12 | Un valor propuesto distinto al registrado | Confirmo la revisión | Solo se aplican los cambios aprobados y válidos; no se sobrescriben otros datos. |
| CA-13 | Valores incompletos o inválidos | Intento guardarlos | BRICA señala las validaciones incumplidas y conserva las propuestas válidas para corregirlas. |
| CA-14 | Una revisión en curso | Cancelo sin confirmar | Los datos vigentes de la obra y del cliente permanecen intactos. |
| CA-15 | Un presupuesto confirmado | Consulto sus conceptos para generar documentos | Los conceptos importados están disponibles como colección asociada al presupuesto seleccionado, sin convertirse en campos individuales fijos. |

### HU-004 · Seleccionar y validar la configuración de generación

Como usuario interno, quiero seleccionar un paquete y las versiones disponibles de sus plantillas para generar la documentación correspondiente.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-16 | Una obra y paquetes compatibles | Inicio una generación | BRICA muestra paquetes compatibles y las plantillas que los integran. |
| CA-17 | Un paquete con varias plantillas | Configuro la generación | Puedo elegir una o varias plantillas y una versión disponible por cada una. |
| CA-18 | Una versión con campos obligatorios faltantes o inválidos | Solicito previsualizar o finalizar | BRICA identifica los campos y bloquea la finalización de los documentos afectados hasta resolverlos. |
| CA-19 | Una versión inactiva, no disponible o cuyo archivo falta | Intento utilizarla | BRICA impide usarla en una nueva generación y permite elegir una alternativa válida. |
| CA-20 | Una plantilla con campos simples y una colección de conceptos | Solicito resolverla | BRICA sustituye los valores simples y reproduce los elementos de la colección según la estructura definida en la plantilla, sin perder el orden seleccionado. |

### HU-005 · Previsualizar y generar documentos

Como usuario interno, quiero revisar el resultado y producir documentos DOCX/PDF a partir de los datos confirmados.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-21 | Una selección válida con datos suficientes | Solicito vista previa | Veo el contenido y formato resultantes sin registrar documentos definitivos. |
| CA-22 | Una vista previa revisada | Confirmo la generación | BRICA produce los formatos DOCX y PDF solicitados para cada plantilla seleccionada y registra sus archivos. |
| CA-23 | Una vista previa cuyos datos, presupuesto o selección cambiaron | Intento confirmar | BRICA exige actualizar la vista previa antes de finalizar. |
| CA-24 | Un error de sustitución, conversión o almacenamiento | Intento finalizar | BRICA identifica los documentos afectados y no presenta archivos incompletos como definitivos. |
| CA-25 | Una generación anterior | Actualizo datos y genero de nuevo | Se produce una nueva generación sin modificar los archivos ni la configuración histórica de la anterior. |

### HU-006 · Guardar, reanudar, consultar y exportar

Como usuario interno, quiero conservar generaciones pendientes y recuperar documentos producidos sin repetir el trabajo.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-26 | Una generación iniciada con selección y datos parciales | Solicito guardar | BRICA conserva el estado pendiente y la información necesaria para continuar. |
| CA-27 | Una generación pendiente | La reabro | Recupero la selección y los datos guardados; BRICA vuelve a validar disponibilidad y vigencia antes de finalizar. |
| CA-28 | Una generación finalizada | Consulto su historial | Veo obra, paquete, plantillas, versiones exactas, documentos, formatos y datos utilizados o su instantánea inmutable. |
| CA-29 | Uno o varios documentos almacenados | Solicito exportarlos | Descargo los archivos seleccionados sin modificar los originales. |
| CA-30 | Un archivo histórico faltante o inaccesible | Intento exportarlo | BRICA informa qué archivo no pudo recuperarse y permite exportar los demás disponibles. |

## 4. Requisitos de la SPEC-002

| ID | Comportamiento requerido | Trazabilidad |
|---|---|---|
| MD-001 | Recibir y validar archivos PDF y XLS/XLSX asociados a una obra, con límites configurables de procesamiento. | RF-04, RNF-03, RES-01 |
| MD-002 | Extraer texto de PDF digitales y aplicar OCR a contenido escaneado, conservando indicios de procedencia y señalando incertidumbre. | RF-04, RF-05, RNF-04 |
| MD-003 | Interpretar hojas presupuestales seleccionadas y proponer datos generales, partidas, conceptos, cantidades, precios e importes, distinguiendo presupuestos alternativos. | RF-04, RF-03, RES-01 |
| MD-004 | Permitir revisión, corrección, descarte y confirmación explícita de propuestas, sin sobrescritura automática de datos vigentes. | RF-03, RF-05, RF-09, CU-02 |
| MD-005 | Registrar fuentes PDF/Excel y presupuestos importados vinculados a la obra; conservar binarios y metadatos de archivo con integridad verificable. | DAS Gestión de Archivos, Diccionario de Datos: ARCHIVO, CONTRATO, PRESUPUESTO |
| MD-006 | Consultar paquetes compatibles y versiones disponibles definidos por la SPEC anterior; permitir seleccionar una o varias plantillas y su versión exacta. | RF-08, RF-11, RF-15, CU-04 |
| MD-007 | Resolver campos del catálogo y colecciones de conceptos, con reglas de representación acordadas para la plantilla. | RF-06, RF-03, RES-02 |
| MD-008 | Validar campos obligatorios, datos inválidos, marcadores no resueltos y disponibilidad de versiones, informando cómo corregirlos. | RF-18, RNF-03, RNF-04 |
| MD-009 | Previsualizar el resultado y requerir una nueva revisión si cambian datos, selección o presupuesto antes de confirmar. | RF-16, RNF-01, CU-04 |
| MD-010 | Generar DOCX y PDF, conservar el formato de la plantilla dentro de las capacidades soportadas y evitar registrar como definitivos resultados incompletos. | RF-06, RES-02, CU-04 |
| MD-011 | Guardar y recuperar generaciones parciales con estado, selección y datos capturados. | RF-23 |
| MD-012 | Registrar generaciones, versiones exactas, instantánea de valores y documentos producidos; mantener inmutables los resultados históricos. | RF-09, RF-17, RNF-01 |
| MD-013 | Consultar y exportar individualmente o en conjunto los archivos generados, sin alterar los almacenados. | RF-24, CU-07 |
| MD-014 | Aplicar autorización en servidor, manejo seguro de archivos y mensajes de error accionables en carga, análisis, generación y exportación. | RF-26, RNF-03, DAS Seguridad |

## 5. Datos y formatos de referencia

Los documentos reales de la empresa sirven como **casos de validación**, no como garantía de compatibilidad con cualquier archivo similar.

- **PDF:** documentos contractuales y administrativos, incluidos escaneos como el acta de entrega proporcionada. Datos candidatos: cliente, contratista, nombre de obra, número de contrato, fechas, ubicación, descripción de trabajos, montos, anticipos y saldos. Su correspondencia con la fuente común debe revisarse antes de guardar; no todos son atributos existentes de OBRA.
- **Excel:** presupuestos como `PPTO. SANTA FE.xlsx`, con varias hojas y conceptos. Datos candidatos: nombre de presupuesto, hoja de origen, partida, clave, descripción, unidad, cantidad, precio unitario, importe, subtotal, impuestos y total.
- **Plantillas de salida:** archivos DOCX versionados administrados por la SPEC anterior. Las colecciones de conceptos requieren una convención explícita de bloques repetibles; su definición debe coordinarse con esa SPEC antes de implementar la sustitución.

La obligatoriedad para generar depende de la configuración de cada versión de plantilla; las reglas para persistir datos de obra y presupuestos son independientes. Se deben preservar identificadores, ceros iniciales, precisión decimal y formatos de fecha sin introducir conversiones silenciosas.

## 6. Límites de diseño y decisiones por concretar

- Mantener separadas **extracción**, **revisión/confirmación**, **persistencia de información de negocio** y **generación**. La orquestación reside en aplicación; lectores, OCR y renderizadores son adaptadores de infraestructura. JavaFX consume la API del servidor.
- No reutilizar el catálogo de campos simples como sustituto de un modelo de conceptos presupuestales. El Diccionario de Datos actual contiene PRESUPUESTO y ARCHIVO, pero no detalla partidas ni conceptos: **se requiere definir la persistencia estructurada** y sus migraciones Flyway.
- Definir los formatos Excel compatibles y cómo se reconocen encabezados, filas, fórmulas, totales, hojas alternativas y agrupaciones; los archivos reales son fixtures de referencia.
- Acordar con la SPEC anterior la sintaxis y el comportamiento de bloques repetibles para conceptos, así como las reglas de campos opcionales y la selección de versiones disponibles.
- Definir límites de OCR, páginas, tamaño, tiempo y recursos; procesar archivos no confiables de forma aislada y sin ejecución de macros ni contenido activo.
- Definir el mecanismo de vista previa y conversión DOCX→PDF, junto con las garantías de fidelidad del formato y la política de errores por documento en generaciones múltiples.
- Definir el tratamiento de borradores, estados de generación, consistencia entre base de datos y almacenamiento de archivos y recuperación ante fallos.
- El CU-07 del ERS cita RF-28, no presente en el listado de RF disponible; esta SPEC vincula la exportación a RF-24 hasta corregir la referencia.

## 7. Condición de entrega

La entrega se aceptará cuando se demuestren, con casos anotados y muestras reales autorizadas: **(1)** analizar PDF digital, escaneado y mixto con OCR donde se requiera; **(2)** leer XLS/XLSX y DOCX fuente, revisar mapeos y conservar presupuestos/conceptos estructurados; **(3)** revisar evidencia, confirmar selectivamente y consultar datos, con permisos y concurrencia; y **(4)** guardar/reanudar una revisión de extracción y recuperarse ante fallos sin aplicar datos automáticamente. Los criterios EX-01–EX-14 del plan concretan la trazabilidad. Los parámetros de calidad, límites y corpus específicos se acuerdan antes de declarar aceptación. No se exige ni implementa generación o exportación de salida.

**Fuentes del proyecto:** ERS_BRICA.docx, DAS_BRICA.docx, DiccionarioDatos_BRICA.docx, SPEC anterior de configuración documental y ejemplos PDF/Excel proporcionados. Los identificadores HU, CA y MD de este documento pertenecen a SPEC-002.
