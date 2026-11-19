# SPEC002 — Decisiones y consideraciones pendientes para la extracción documental

**Fecha:** 2026-11-17.

**Estado:** inventario para discusión; las alternativas y recomendaciones **no son decisiones aprobadas**.

**Alcance de este documento:** lectura de archivos base, extracción e identificación de datos y decisiones necesarias para su revisión e incorporación. **La generación documental no pertenece a esta SPEC**, según la última indicación del responsable.

Se conserva el nombre de archivo solicitado, `pendients.md`. No se crea todavía `plan.md`: primero debe cerrarse este inventario para no convertir supuestos en tareas aprobadas.

## 1. Fuentes, precedencia y límites de la revisión

Se contrastaron:

- [SPEC002 actual](spec.md), HU-001–HU-006, CA-01–CA-30 y MD-001–MD-014. Es una propuesta de requisitos, no una implementación del motor.
- [SPEC001](../001/spec.md), especialmente las correcciones aprobadas del 2026-11-15, y su [seguimiento](../001/review.md).
- [README](../../README.md) y [MEMORY](../../MEMORY.md).
- Código local del servidor y del cliente: catálogo/resolución, análisis DOCX, adjuntos, contratos, información de obra, clientes, selección documental, seguridad y almacenamiento. Esta revisión incluye cambios locales aún no publicados; no presupone que todo esté integrado en `main`.
- POM del servidor, requisitos Python, `application.yml`, migraciones existentes y configuración de Compose.
- Secciones pertinentes de `docs/ERS_BRICA.docx`, `docs/DAS_BRICA.docx` y `docs/DiccionarioDatos_BRICA.docx`, leídas sin modificar sus archivos.

`context.md` está vacío. La muestra de presupuesto que SPEC002 llama `PPTO. SANTA FE.xlsx` no aparece con ese nombre en `docs/`; sí están `PPTO 01 041126.xlsx` y su PDF. La referencia a un acta escaneada tampoco identifica inequívocamente un archivo local. No se analizó aquí el contenido de los PDF/Excel de negocio ni se declara su compatibilidad.

**Precedencia para redactar la propuesta:** la última instrucción del responsable excluye generación; las decisiones vigentes de SPEC001 prevalecen sobre sus notas históricas. Una contradicción con ERS/DAS/Diccionario se señala, no se corrige silenciosamente. Modificar esas fuentes o elegir bibliotecas requiere aprobación separada.

### 1.1. Base confirmada: no reabrirla por omisión

1. La obra y el cliente ya existen. No se autorizó crearlos ni reasignarlos automáticamente desde un archivo.
2. Analizar una plantilla y extraer valores de un documento fuente son operaciones distintas. `TemplateAnalysisService` y `brica_docx.py` reconocen `{{CLAVE}}`, comprueban catálogo y configuración; no implementan extracción PDF/Excel ni OCR. Tener `docxtpl` instalado no resuelve esos lectores.
3. El catálogo tiene atributos internos y adicionales `TEXT`, `DATE`, `AMOUNT`; no evalúa rutas libres. Un campo utilizado por una plantilla no implica que su valor exista en un documento fuente.
4. La información no se sobrescribe automáticamente. SPEC002 escrita exige propuestas y confirmación explícita; cualquier recorte a identificación sin persistencia debe quedar acordado en P01.
5. Nombres de cliente/obra/paquete/plantilla/versión: letras ASCII y ñ/Ñ, números y `_`, al menos una letra y sin `_` inicial. No confundirlos con claves canónicas de campos ni nombres originales de archivos.
6. Las fechas siguen siendo opcionales y ninguna terminación contractual o real puede ser anterior al inicio. Los importes precisos se conservan separados de su presentación; no se aprobaron cálculos fiscales automáticos.
7. Paquetes de cliente reutilizables en obras del mismo cliente; los exclusivos de obra no se vuelven reutilizables por aparecer en la consulta del cliente. No existe clasificación por operación/etapa.
8. Versiones y adjuntos tienen eliminación lógica y archivos históricos conservados. Las nuevas selecciones usan la última versión no eliminada; no se reutilizan números de versión.
9. JavaFX consume la API. Extracción técnica en infraestructura y coordinación en aplicación, sin acceso del escritorio o del proceso Python a PostgreSQL.
10. El permiso es explícito; la jerarquía no lo concede por sí sola. Practicante/Invitado son de lectura y el flujo de propuestas interpersonales/aprobación sigue diferido. Una propuesta producida por un extractor no equivale a ese flujo.

### 1.2. Diferencias documentales que deben quedar visibles

| Fuente | Diferencia comprobada | Tratamiento necesario |
|---|---|---|
| SPEC002, resultado esperado, §1, HU-004–HU-006 y condición de entrega | Incluye previsualización, generación, exportación e historial de generaciones. | Reescribir el alcance en una tarea aprobada; no implementarlos por estar todavía en el texto. Ver P01/P32. |
| SPEC002 §2 | Exige paquete compatible con cliente y **operación**. | Eliminar esa condición al alinear documentos: contradice la decisión vigente. Un extractor no debe exigir paquete para poder analizar fuentes. |
| SPEC002 CA-17/MD-006 | Sugiere elegir una versión disponible por plantilla. | No ampliar ahora la selección vigente de SPEC001; es una decisión de la futura generación. |
| SPEC002 §1/CA-06 | Incluye XLS/XLSX y OCR. | XLS no está admitido por los adjuntos actuales; OCR no existe. No presentarlos como capacidades disponibles ni descartarlos sin aprobación. |
| MEMORY, estado y D-03/D-09 | Describe V5–V7, solo nombres y fechas/importes diferidos. | Es histórico: el código y seguimiento llegan a V14 y contemplan campos tipados/fechas/importes. No volver a implementar lo existente. |
| MEMORY, D-06 y estado de Git | Remite a `storage-test.md` eliminado localmente y describe una situación anterior de commits/push. | Usar el estado local y el seguimiento como evidencia; no restaurar archivos eliminados ni tratar notas históricas como autorizaciones. |
| ERS CU-02 | Cambios contractuales de monto/plazo/presupuesto deben tramitarse como convenio. | Separar completar/corregir captura de modificar condiciones contractuales; decidir P21 antes de confirmar importaciones. |
| ERS CU-07 y SPEC002 | Exportación cita RF-28 no localizado en el listado; SPEC002 propone RF-24. | Mantener la discrepancia documentada para la futura generación/exportación, no crear un requisito de extracción ficticio. |
| Diccionario | Define PRESUPUESTO/ARCHIVO, pero no partidas/conceptos ni revisión de extracciones; conserva operación/etapa. | Diseñar el modelo que falta y alinear fuentes después de aprobarlo. No copiar literalmente el esquema conceptual. |

### 1.3. Lectura de prioridades

- **B — Bloqueante:** cerrar antes de implementar el componente afectado.
- **D — Diseño/operación:** documentar y aprobar antes de su entrega; se puede investigar con casos privados sin modificar negocio.
- **F — Futuro:** registrar, pero no convertir en dependencia para terminar la extracción.

No es necesario responder todas las cuestiones en una sola conversación. El orden sugerido está en §9.

## 2. Alcance, formatos y datos que se intentarán reconocer

### P01 — Qué significa terminar la extracción y qué requisitos se mantienen

**Prioridad:** B. **Referencia:** SPEC002 §1, HU-001–HU-003 y nueva instrucción del responsable.

**Problema y relevancia:** la generación está excluida, pero falta precisar si esta entrega termina mostrando candidatos o también permite confirmar su incorporación. Son alcances diferentes: el segundo requiere persistencia de propuestas, conflictos, permisos y transacciones. La condición de entrega actual exige documentos DOCX/PDF finales y ya no sirve para aceptar esta SPEC.

- **A. Identificación solamente:** cargar, analizar y mostrar candidatos/procedencia; ninguna escritura en datos de negocio. **Pros:** entrega menor, menor riesgo de sobrescritura, permite medir lectores antes de diseñar toda la importación. **Contras:** obliga a recapturar, no satisface CA-11–CA-15 completos y no demuestra reutilización de datos confirmados.
- **B. Extracción revisable con confirmación selectiva:** conservar fuente y propuestas, comparar con valores actuales y aplicar solo lo aprobado. **Pros:** cumple la intención de HU-003, reduce captura y deja información utilizable por el motor futuro. **Contras:** exige modelo de revisión, autorización por destino, concurrencia y recuperación; cuesta más que una pantalla de resultados.

**Orientación, pendiente de aprobación:** B conserva mejor lo pedido en la SPEC escrita. La revisión sería de datos/fuentes, no una vista previa de documentos generados. Precisar también si la captura manual sin archivo forma parte de este flujo o utiliza los formularios existentes.

Se acepta la propuesta B, remarcando que la captura manual de datos SÍ se incluye en esta iteración. 

### P02 — Formatos de entrada y clase de documento fuente

**Prioridad:** B. **Referencia:** MD-001/MD-005, `AttachmentFormat`, `WorkAttachment.Kind`.

**Problema y relevancia:** adjuntar ya admite DOCX/PDF/XLSX, pero solo clasifica CONTRATO/PRESUPUESTO_BASE. SPEC002 pide PDF y XLS/XLSX, y menciona otros documentos administrativos. Adjuntar DOCX no significa que se haya aprobado extraerlo; XLS requiere otro lector y validación de contenedor.

- **A. Primera entrega PDF y XLSX:** mantener DOCX como adjunto sin extracción y diferir XLS expresamente. **Pros:** reduce variantes y evita introducir el contenedor binario antiguo; aprovecha entradas actuales. **Contras:** recorta CA-06 y puede excluir archivos reales; requiere aceptación del recorte, no llamarlo implementación completa de XLS/XLSX.
- **B. PDF, XLS y XLSX:** agregar soporte explícito de XLS y mantener DOCX fuera de extracción. **Pros:** coincide con el alcance escrito y evita conversiones manuales del usuario. **Contras:** más dependencias, validaciones y fixtures; no se pueden tratar XLS y XLSX como el mismo contenedor.
- **C. Incluir también DOCX como fuente:** usar un lector de contenido separado del analizador de plantillas. **Pros:** aprovecha documentos base ya adjuntos. **Contras:** amplía el encargo, necesita reglas propias para tablas/texto y no se resuelve invocando el analizador de marcadores.

**Decidir:** matriz formato × clase de fuente × operación permitida. Si se admiten actas/órdenes de servicio, elegir clasificación adicional sin confundirla con el tipo documental de una plantilla.

Se acepta la propuesta C. Se deben poder extraer información del docx, pdf xls

### P03 — Corpus representativo y promesa de compatibilidad

**Prioridad:** B para definir aceptación; D para ampliar cobertura. **Referencia:** SPEC002 §5 y archivos locales.

**Problema y relevancia:** no se identifican inequívocamente el presupuesto y el escaneo citados. Un ejemplo de presupuesto no representa todos los contratos, diseños y escaneos. Sin corpus acordado no puede medirse calidad ni afirmarse que un formato es compatible.

- **A. Perfiles de formatos conocidos con muestras autorizadas:** inventariar PDF digital, escaneo, mixto, XLS si se mantiene, XLSX de varias hojas, archivo dañado y conflictos. **Pros:** resultados reproducibles, fallos explicables y límites de soporte claros. **Contras:** requiere reunir muestras, anonimizar y mantener perfiles cuando cambia el formato.
- **B. Lector genérico con mapeo manual:** detectar estructura y pedir al usuario columnas/regiones que no se reconozcan. **Pros:** acepta mayor variedad sin desarrollar un perfil por cliente. **Contras:** más interacción y menor automatización; reconocer estructura no garantiza interpretar el dato correcto.
- **C. Prometer reconocimiento de cualquier formato:** **Pros:** experiencia ideal si funcionara. **Contras:** no es verificable con las muestras disponibles, contradice la exclusión de formatos arbitrarios y genera expectativas falsas; no es una base aceptable de entrega.

**Decidir:** qué archivos son referencia autorizada, cuáles pueden convertirse en fixtures anonimizados y qué contenido esperado se anotará manualmente. No versionar documentos reales ni modificarlos durante la investigación.

Se acepta la opción B. 
### P04 — Universo de campos: catálogo completo o guiado por plantillas

**Prioridad:** B. **Referencia:** `ReferenceAttribute`, `TemplateAnalysisService`, `ResolveReferenceValuesService`.

**Problema y relevancia:** el catálogo define el significado y origen del destino, pero no dónde aparece su valor en un PDF/Excel. Además, algunas claves representan IDs internos que nunca deberían sustituirse con números impresos. Usar solo campos de una plantilla puede dejar de reconocer datos útiles presentes en la fuente.

- **A. Extraer todos los campos de negocio admitidos:** usar una lista explícita de destinos escribibles independiente del paquete. **Pros:** sirve para completar obra/cliente aunque no haya plantillas; evita acoplar fuentes con una salida. **Contras:** más candidatos y revisión; debe limitarse el conjunto para evitar falsos positivos.
- **B. Elegir campos objetivo antes de analizar:** el usuario selecciona un subconjunto, opcionalmente sugerido por una plantilla. **Pros:** reduce ruido y esfuerzo de revisión. **Contras:** agrega un paso y puede omitir información; leer configuración de plantillas necesitaría una política de permiso sin conceder administración completa.
- **C. Exigir siempre paquete y versión:** **Pros:** objetivos concretos. **Contras:** impide preparar datos antes de configurar plantillas y traslada decisiones de generación a esta SPEC; no se recomienda como precondición.

**Decidir:** campos objetivo iniciales y tratamiento de campos no encontrados. IDs de obra/cliente y relaciones se mantienen internos; una referencia impresa puede servir de indicio, no de autorización para reasignar entidades.

Se acepta la opcion A
### P05 — Correspondencia entre etiquetas, valores y claves canónicas

**Prioridad:** B. **Referencia:** CA-01/CA-03/CA-07, catálogo cerrado y adicionales por obra.

**Problema y relevancia:** una fuente puede decir «Contratante», «Dependencia» o «Cliente» para conceptos parecidos; también contener varios montos. La coincidencia de palabras no basta para mapear un dato. No existe un catálogo de sinónimos/perfiles de extracción ni configuración de reglas de lectura.

- **A. Reglas y perfiles versionados mantenidos en código:** **Pros:** determinismo, pruebas claras y menos configuración peligrosa. **Contras:** cada variante necesita mantenimiento y despliegue; no cubre documentos desconocidos sin intervención.
- **B. Mapeo asistido por usuario:** reconocer candidatos y permitir asociarlos a destinos admitidos, guardando opcionalmente un perfil. **Pros:** más flexible y permite corregir formatos nuevos. **Contras:** requiere permisos para editar/compartir perfiles y reglas para validar su alcance; un perfil mal reutilizado puede equivocarse sistemáticamente.
- **C. Extracción semántica mediante un modelo:** **Pros:** puede reconocer expresiones y estructuras menos uniformes. **Contras:** respuestas no deterministas, coste, privacidad, validación adicional y riesgo de inventar valores. El texto de una fuente nunca puede convertirse en instrucciones ejecutables ni autorizar herramientas.

**Decidir:** estrategia inicial, responsable de las reglas y manejo de datos desconocidos. No crear automáticamente campos globales ni rutas de origen a partir de etiquetas libres; proponerlos como no mapeados o permitir asociarlos a un adicional ya registrado.

Se acepta la opción B

### P06 — Varios candidatos, varios documentos y precedencia

**Prioridad:** B. **Referencia:** CA-03/CA-11/CA-12.

**Problema y relevancia:** contrato y presupuesto pueden discrepar; un mismo PDF puede incluir importe contratado, anticipo, subtotal y saldo, o fechas distintas. Elegir el primer resultado o el último archivo produciría cambios silenciosos aunque el texto se haya leído correctamente.

- **A. Conservar alternativas y resolver cada conflicto manualmente:** **Pros:** transparencia y ninguna precedencia implícita; permite ver todas las evidencias. **Contras:** más trabajo y necesidad de agrupar duplicados para no saturar la revisión.
- **B. Precedencia explícita por campo y clase de fuente:** mantener todas las alternativas y destacar una recomendada. **Pros:** agiliza casos frecuentes sin perder evidencia. **Contras:** exige acordar reglas distintas por campo, explicar excepciones y versionar la política; no autoriza confirmación automática.

**Decidir:** fuentes prioritarias por dato, qué constituye igualdad y si la aprobación puede mezclar candidatos de fuentes distintas. No igualar por presentación redondeada ni usar una puntuación de confianza como única precedencia.

## 3. PDF, OCR y presupuestos Excel

### P07 — PDF digital, lectura de tablas y selección de regiones

**Prioridad:** B. **Referencia:** CA-01/CA-03/CA-04.

**Problema y relevancia:** extraer texto no garantiza orden de lectura: columnas, encabezados repetidos y tablas pueden separar una etiqueta de su valor. La comprobación actual de PDF verifica la cabecera `%PDF-`, no que el documento completo sea procesable.

- **A. Texto y reglas de proximidad para formatos conocidos:** **Pros:** menor coste, rápido en PDF digitales simples y fácil de probar. **Contras:** pierde contexto de tablas/columnas y puede interpretar mal el orden si el extractor solo devuelve texto plano.
- **B. Texto con coordenadas, bloques y tablas cuando estén disponibles:** **Pros:** mejora correspondencia y facilita señalar la región de origen. **Contras:** normalizar coordenadas, rotación, páginas y tablas cuesta más; la estructura inferida sigue siendo una hipótesis.
- **C. Regiones seleccionadas manualmente:** **Pros:** salida útil ante formatos desconocidos y evidencia muy concreta. **Contras:** necesita visor/interacción y reduce la extracción automática.

**Decidir:** combinaciones soportadas y respuesta ante PDF cifrados, truncados o parcialmente legibles. Rechazar o permitir sustituir el archivo sin guardar valores no revisados; no añadir recuperación de contraseñas ni descifrado como supuesto.

### P08 — OCR: alcance, idiomas y resultados inciertos

**Prioridad:** B si se conserva CA-02. **Referencia:** PDF escaneados/mixtos en SPEC002.

**Problema y relevancia:** no existe OCR en el código. Un PDF con algo de texto puede tener páginas o regiones escaneadas; tener una capa textual no implica que sea buena. La confusión `O/0`, `I/1`, separadores y firmas puede alterar identificadores o importes.

- **A. OCR solo donde falle la lectura digital:** **Pros:** reduce tiempo y transformaciones innecesarias; permite mantener el texto original cuando es fiable. **Contras:** hay que definir cómo detectar insuficiencia/calidad y cómo combinar resultados en páginas mixtas sin duplicar datos.
- **B. OCR a todas las páginas:** **Pros:** flujo técnico más uniforme ante documentos heterogéneos. **Contras:** consume más recursos y puede empeorar texto digital correcto; introduce errores evitables y requiere conservar la evidencia original.
- **C. Diferir OCR y permitir revisión/captura manual:** **Pros:** simplifica la primera entrega. **Contras:** no cumple CA-02 ni la condición original; requiere un recorte explícito de alcance, no considerarlo completado.

**Decidir:** idiomas, impresos/manuscritos, rotación, resolución y calidad mínima. El OCR se presenta como propuesta, con advertencias; no prometer que una puntuación representa probabilidad real de acierto ni usarla para confirmar automáticamente.

### P09 — Tecnología de extracción y dependencias autorizadas

**Prioridad:** B antes de agregar dependencias. **Referencia:** POM, requisitos Python y D-04 de SPEC001.

**Problema y relevancia:** el proceso Python aprobado es un analizador DOCX restringido, no una aprobación general de librerías PDF/Excel/OCR. Faltan lectores y posiblemente binarios externos. Deben evaluarse licencia, versiones, vulnerabilidades, instalación offline y soporte Windows/Linux del servidor.

- **A. Lectores Java y OCR externo cuando sea necesario:** **Pros:** integración directa con tipos, errores y despliegue Java; menos intercambio de datos para PDF/Excel. **Contras:** introduce dependencias Maven y el OCR puede seguir necesitando procesos nativos; debe limitarse el uso de memoria del backend.
- **B. Adaptadores en procesos Python locales separados:** **Pros:** aprovecha el patrón existente JSON/timeout y permite terminar un proceso problemático. **Contras:** requiere bibliotecas/binarios nuevos, protocolo acotado y despliegue reproducible; no heredar sin más los límites de DOCX ni su intercambio base64.
- **C. Servicio de extracción separado o externo:** **Pros:** aislamiento y escalado independientes; un proveedor puede aportar OCR avanzado. **Contras:** cambia arquitectura y operación, añade comunicaciones y, si es externo, expone documentos/costes a un tercero. No está autorizado actualmente.

**Decidir:** mediante una prueba acotada con el corpus de P03. Mantener puertos/adaptadores, sin base de datos accesible al lector ni cambios de arquitectura implícitos. Aprobar nombres/versiones/licencias concretos antes de incorporarlos; este documento no elige una biblioteca.

### P10 — Reconocimiento de hojas, encabezados y conceptos Excel

**Prioridad:** B. **Referencia:** CA-06/CA-07/CA-10, MD-003.

**Problema y relevancia:** pueden existir títulos fusionados, filas vacías, encabezados repetidos, partidas, subtotales y columnas sin nombre. No está definido si se soportan hojas ocultas, filtros, filas ocultas o varias tablas en una misma hoja. Importar cada fila como concepto duplicaría totales y texto de presentación.

- **A. Perfiles por diseño de presupuesto:** **Pros:** alta precisión para ejemplos conocidos y reglas verificables para filas de resumen/partida. **Contras:** cambios de diseño requieren actualizar el perfil; hay que identificar cuándo un archivo dejó de corresponder a él.
- **B. Detección inicial y mapeo editable de columnas/rangos:** **Pros:** adapta hojas nuevas y permite excluir regiones. **Contras:** interfaz y validación más complejas; el usuario debe distinguir correctamente las columnas.
- **C. Plantilla Excel única de intercambio:** **Pros:** estructura predecible y menor mantenimiento del lector. **Contras:** exige transformar presupuestos antes de importarlos y no cumple por sí sola la importación de archivos originales proporcionados.

**Decidir:** columnas mínimas, filas de encabezado, celdas fusionadas, orden, jerarquía de partidas y exclusión de filas ocultas. Mostrar lo que se excluye; no descartarlo sin informar.

### P11 — Fórmulas, valores almacenados y contenido activo de Excel

**Prioridad:** B. **Referencia:** CA-08 y seguridad de archivos.

**Problema y relevancia:** fórmula, valor calculado almacenado y texto mostrado no son equivalentes. Puede faltar el valor almacenado, estar desactualizado o depender de vínculos externos. Abrir el archivo con un lector no garantiza recalcular correctamente todas las funciones.

- **A. Leer valores almacenados y conservar fórmulas como evidencia:** **Pros:** evita ejecutar contenido activo y es relativamente reproducible. **Contras:** no asegura actualidad; valores sin cálculo almacenado requieren revisión y no pueden sustituirse por cero.
- **B. Evaluar un subconjunto de fórmulas con reglas explícitas:** **Pros:** resuelve operaciones simples conocidas y permite contrastar cantidad × precio. **Contras:** requiere definir compatibilidad y distinguir verificación de importe de recalcular toda la hoja; funciones no admitidas deben quedar pendientes.
- **C. Recalcular mediante una aplicación externa controlada:** **Pros:** puede soportar más funciones. **Contras:** añade infraestructura, aislamiento, tiempos y posibles diferencias de resultados; nunca ejecutar macros, vínculos externos ni comandos contenidos en la hoja.

**Decidir:** política para fórmula sin valor utilizable, funciones admitidas y vínculos externos. Conservar el original inmutable y no afirmar que un cálculo de BRICA es el que traía el archivo.

### P12 — Varias hojas, alternativas y relaciones entre presupuestos

**Prioridad:** B. **Referencia:** CA-06/CA-09.

**Problema y relevancia:** una hoja puede ser un presupuesto alternativo, una continuación o un resumen de otras hojas. La SPEC pide no fusionar alternativas, pero no explica cómo reconocer continuaciones y resúmenes. Elegir todas las hojas puede duplicar conceptos e importes.

- **A. Cada hoja seleccionada crea un presupuesto distinto:** **Pros:** trazabilidad simple y evita fusiones automáticas. **Contras:** fragmenta presupuestos repartidos en varias hojas y puede registrar resúmenes como presupuestos vacíos.
- **B. Agrupación explícita por el usuario:** seleccionar hojas y clasificarlas como partes, resumen o alternativas. **Pros:** conserva estructura real y evita inferir decisiones de negocio. **Contras:** requiere un paso adicional y validaciones de duplicados/orden.
- **C. Agrupación sugerida por reglas:** **Pros:** reduce trabajo con archivos estables. **Contras:** nombres de hojas no prueban relaciones; la sugerencia debe confirmarse y mostrar qué se incorpora.

**Orientación:** B, con sugerencias opcionales. Decidir orden de hojas/filas y si un resumen aporta totales sin convertirse en fuente adicional de conceptos.

## 4. Modelo de datos y significado de la información

### P13 — Modelo estructurado de presupuestos, partidas y conceptos

**Prioridad:** B antes de persistir conceptos. **Referencia:** CA-15/MD-003/MD-005 y Diccionario PRESUPUESTO.

**Problema y relevancia:** no hay entidades de presupuesto/partidas/conceptos implementadas. `contracts.data` es JSON de contratos, no un presupuesto estructurado. El catálogo escalar no permite almacenar una colección ordenada de conceptos sin perder su significado.

- **A. Tablas normalizadas:** presupuesto, partidas y conceptos con orden, tipos e identidad. **Pros:** restricciones claras, consultas/paginación eficientes y edición por concepto. **Contras:** más tablas/migraciones y diseño explícito de jerarquías antes de importar.
- **B. Documento JSONB versionado por presupuesto:** **Pros:** permite estructuras variables y conserva una captura completa con rapidez. **Contras:** las restricciones y ediciones parciales son más complejas; hay que diseñar esquema/versionado y evitar colecciones enormes en una sola respuesta.
- **C. Modelo híbrido:** cabecera/identidad/revisiones explícitas y contenido flexible acotado. **Pros:** combina relaciones fiables con adaptación de metadatos. **Contras:** dos niveles de validación y necesidad de definir qué nunca queda como JSON libre.

**Decidir:** campos obligatorios, orden, profundidad de partidas, conceptos sin clave y claves repetidas. Cantidades y precios no son campos globales individuales ni deben registrarse uno por cada fila.

### P14 — Identidad del presupuesto, revisiones y reimportación

**Prioridad:** B. **Referencia:** CA-09/CA-12/CA-15.

**Problema y relevancia:** confirmar otra hoja o un archivo corregido puede significar presupuesto nuevo, revisión o duplicado. No existe regla para presupuesto vigente/principal ni para reconciliar conceptos sin claves únicas. Reemplazar silenciosamente elimina historia útil para el motor posterior.

- **A. Cada importación confirmada crea un presupuesto independiente:** **Pros:** no destruye anteriores y la implementación es más simple. **Contras:** proliferan duplicados y el usuario necesita distinguir cuál utilizar.
- **B. Presupuesto con revisiones inmutables y elección explícita de revisión principal:** **Pros:** trazabilidad, comparación y futura selección documental precisa. **Contras:** requiere reglas de vigencia y edición; no puede decidirse solo por fecha del archivo o número de hoja.
- **C. Actualización en sitio:** **Pros:** lista más sencilla y menos objetos visibles. **Contras:** puede borrar conceptos/aprobaciones anteriores y complica historial/concurrencia; solo aceptable si se conserva una revisión previa verificable y se aprueba esa política.

**Decidir:** identidad, duplicados, revisión principal, qué se compara y si se reemplaza/agrega/excluye algún concepto. Que exista el mismo código de concepto no prueba que sea la misma partida ni la misma revisión.

### P15 — Procedencia, confianza y estados de cada dato

**Prioridad:** B para el contrato de propuestas. **Referencia:** CA-01/CA-03/CA-11.

**Problema y relevancia:** se piden orígenes «cuando sea posible», sin definir cuáles son imprescindibles. Un valor puede haberse leído con exactitud y aun así estar asociado al campo equivocado. Los estados de resolución `PRESENT/MISSING/INVALID` describen datos de negocio, no candidatos OCR ambiguos ni decisiones del revisor.

- **A. Evidencia mínima:** archivo/hash, página o hoja/celda, texto original y destino sugerido. **Pros:** trazabilidad útil con coste moderado y fácil comparación. **Contras:** localizar visualmente una coincidencia entre varias puede resultar difícil; OCR necesita al menos referencia a la página.
- **B. Evidencia enriquecida:** regiones/coordenadas, método, versión de lector/perfil, candidatos alternativos y correcciones. **Pros:** permite auditoría y revisión visual precisa. **Contras:** más almacenamiento e interfaz; coordenadas y extractos también contienen información sensible.

**Decidir:** evidencia obligatoria por formato y estados separados para lectura, validación y revisión. Conservar texto original y valor normalizado; si se muestra confianza, explicar su origen y no inventar porcentajes comparables entre motores.

### P16 — Nombres extraídos frente a las restricciones de SPEC001

**Prioridad:** B. **Referencia:** `InputRules.validName`, CA-43 de SPEC001.

**Problema y relevancia:** nombres reales como una razón social o nombre de obra suelen contener espacios, acentos y puntuación. La extracción puede ser correcta y el servidor rechazar el destino. Cambiar «Construcción del Niño» por otro nombre automáticamente contradiría la prohibición de normalizaciones silenciosas.

- **A. Conservar propuesta literal y pedir corrección explícita antes de guardar:** **Pros:** respeta reglas vigentes y permite explicar exactamente el rechazo. **Contras:** genera trabajo manual y el nombre técnico guardado puede no ser adecuado para futuros documentos oficiales.
- **B. Separar identificador/nombre técnico y denominación documental literal:** **Pros:** conserva el texto legal y permite nombres internos restringidos. **Contras:** añade un atributo/fuente y exige decidir qué campo consumirá cada plantilla; requiere migración y aprobación del modelo, no solo un filtro del extractor.
- **C. Revisar las restricciones generales de nombres:** **Pros:** admite directamente denominaciones reales. **Contras:** reabre una decisión reciente de SPEC001 y afecta formularios, pruebas y validaciones; no puede introducirse como arreglo oculto del importador.

**Decidir expresamente:** A mantiene compatibilidad inmediata; B o C requieren decisión de negocio adicional. El nombre original del archivo no está sujeto a esas restricciones y no debe alterarse para resolver este problema.

### P17 — Fechas reconocidas y significado contractual/real

**Prioridad:** B. **Referencia:** fechas de SPEC001, `ReferenceValue` y `Work`.

**Problema y relevancia:** `03/04/2026` puede ser ambiguo; una fecha de firma no es la fecha de inicio ni la de terminación real. OCR puede intercambiar dígitos y Excel utiliza sistemas de fechas distintos. Una propuesta aislada válida puede volver inconsistente el conjunto de fechas.

- **A. Formatos regionales explícitos por fuente/perfil:** **Pros:** conversiones reproducibles, diagnóstico de formatos no admitidos y validación cronológica conocida. **Contras:** requiere configurar convenciones y reconocer fechas textuales o seriales; formatos ambiguos deben seguir pendientes.
- **B. Proponer interpretaciones y exigir selección:** **Pros:** evita adivinar y muestra el texto original. **Contras:** más intervención en fuentes heterogéneas y necesidad de validar nuevamente el conjunto al confirmar.

**Decidir:** fechas iniciales admitidas, interpretación de fechas Excel, tratamiento de fechas parciales y mapeo de firma/entrega/inicio/terminación. No relajar la cronología aprobada ni inventar día/mes faltantes; almacenar una fecha tipada, no el texto español de presentación como dato base.

### P18 — Decimales, unidades, claves y conciliación económica

**Prioridad:** B. **Referencia:** CA-07/CA-08, `AmountPresentation`, importes `NUMERIC`.

**Problema y relevancia:** separadores, monedas y texto formateado pueden cambiar la interpretación de `1,234`. Una clave `0012` no es la cantidad 12. El valor numérico subyacente de Excel puede no reflejar su formato visible; OCR puede perder signos. La regla de presentación de SPEC001 no es una regla de cálculo presupuestal.

- **A. Lectura literal y decimal preciso con revisión de discrepancias:** **Pros:** no modifica lo declarado por el documento; conserva signos, ceros iniciales y evidencia. **Contras:** importes inconsistentes pueden necesitar una advertencia o quedar pendientes; hay que decidir qué impide confirmar.
- **B. Lectura más cálculo de comprobación separado:** **Pros:** detecta diferencias entre cantidad × precio, subtotales e impuestos sin perder el original. **Contras:** requiere acordar moneda, precisión, tolerancia, descuentos y redondeo; no puede sobrescribir el importe fuente con el calculado.

**Decidir:** convenciones por archivo, tratamiento de porcentajes/moneda, unidades y discrepancias admisibles. Diferenciar importe fuente, valor preciso confirmado, cálculo de comprobación y presentación; nunca redondear la captura usando `AmountPresentation` ni realizar operaciones económicas con `double` como fuente de verdad.

### P19 — Campos sin destino, adicionales y cambios del catálogo

**Prioridad:** B. **Referencia:** anticipos/saldos de SPEC002 §5 y `DocumentInformationService`.

**Problema y relevancia:** anticipo y saldo no figuran como atributos fijos de obra. Los adicionales son definiciones globales con valor por obra, y sus claves/tipos pueden cambiar de disponibilidad mientras una propuesta está abierta. Un extractor no debe crear atributos de dominio ni recuperar definiciones eliminadas por sí mismo.

- **A. Mantener candidatos no mapeados:** **Pros:** no amplía el modelo sin decisión y permite continuar con el resto. **Contras:** esos datos no serán reutilizables como valores confirmados hasta definir su destino.
- **B. Asociar explícitamente a un adicional existente:** **Pros:** aprovecha el catálogo tipado y la captura por obra. **Contras:** debe confirmar equivalencia semántica y tipo; un saldo dependiente del periodo puede no corresponder a un adicional permanente de obra.
- **C. Incorporar un atributo/modelo específico:** **Pros:** reglas y significado de negocio más fuertes. **Contras:** migraciones, cambios de resolución y requisitos de vigencia; requiere aprobación separada.

**Decidir:** qué hacer con desconocidos y qué autoridad puede crear definiciones. Revalidar el catálogo al confirmar; conservar la evidencia si un campo desapareció, sin reasignarlo a otra clave por similitud.

### P20 — Contrato fuente, `contracts.data` y fuente común de obra

**Prioridad:** B. **Referencia:** `RegisterContractService`, `WorkReferenceData`, Diccionario CONTRATO/ARCHIVO.

**Problema y relevancia:** hay adjuntos marcados CONTRACT y registros de contrato JSON, pero no una vinculación estructurada entre ambos ni una correspondencia aprobada entre JSON contractual y atributos de obra. El resolver documental lee la obra/cliente, no cualquier clave de `contracts.data`. Guardar el mismo monto en ambos sin autoridad definida crea dos fuentes de verdad.

- **A. Confirmar solo en los atributos aprobados de obra/cliente y conservar evidencia de la fuente:** **Pros:** reutilización inmediata mediante el resolver actual y menor duplicidad. **Contras:** no constituye un registro completo de contrato ni resuelve su historial especializado.
- **B. Registrar contrato estructurado como autoridad y adaptar resolución:** **Pros:** más cercano al modelo contractual del Diccionario y mejor separación documental. **Contras:** amplía el modelo actual, la API y la resolución; necesita decidir relaciones, contratos múltiples y vigencia.
- **C. Conservar JSON de extracción adicional a la fuente común:** **Pros:** preserva datos sin perder estructura variable. **Contras:** si ese JSON es editable y también contiene valores vigentes, puede divergir; debe distinguir evidencia inmutable de dato de negocio confirmado.

**Decidir:** destino y autoridad por campo. No afirmar que subir un PDF contractual ya crea un contrato, ni que registrar JSON lo vuelve fuente del catálogo automáticamente.

### P21 — Completar captura frente a registrar una modificación contractual

**Prioridad:** B. **Referencia:** ERS CU-02 FE02 y exclusión de convenios de SPEC002.

**Problema y relevancia:** la API actual permite capturar montos y fechas de obra, pero el ERS exige convenio para cambios contractuales. Una importación posterior con otro monto puede ser corrección de captura o una modificación real. La fuente nueva, por sí sola, no permite distinguirlo.

- **A. Permitir únicamente completar valores ausentes o corregir con justificación explícita:** **Pros:** limita sobrescrituras y permite documentar errores de captura. **Contras:** todavía requiere quién puede calificar una corrección y qué historial se conserva; una justificación no sustituye un convenio real.
- **B. Permitir propuestas conflictivas, pero bloquear su aplicación contractual:** **Pros:** no pierde la información extraída y respeta la separación de convenios. **Contras:** el usuario no podrá completar esos cambios mediante esta entrega y necesita explicación de cómo proceder fuera de ella.
- **C. Implementar vigencia/convenios ahora:** **Pros:** resuelve de forma completa el cambio contractual. **Contras:** contradice la exclusión vigente, introduce otro flujo de negocio y retrasaría la extracción; solo sería posible con ampliación explícita.

**Decidir:** campos protegidos, cuándo se consideran establecidos y procedimiento de corrección. No inferir convenio de una fecha de archivo ni permitir que reimportar eluda las reglas contractuales.

## 5. Revisión, confirmación y continuidad

### P22 — Confirmación en obra, cliente y presupuesto

**Prioridad:** B si se elige P01-B. **Referencia:** CA-11–CA-14 y casos de uso actuales.

**Problema y relevancia:** una revisión puede tocar tres destinos. Cambiar un cliente compartido afecta otras obras, y un permiso de escritura de obra no autoriza editar cliente. Los servicios actuales no constituyen una confirmación atómica de toda una importación; un DTO completo de obra puede vaciar datos omitidos si se usa como actualización parcial.

- **A. Confirmación atómica de los cambios seleccionados:** preparar un conjunto explícito por destino, aplicar servicios/reglas dentro de una transacción y dejar evidencia. **Pros:** no queda la obra actualizada con un presupuesto fallido; permite una decisión coherente. **Contras:** coordinación entre módulos, permisos compuestos, orden de bloqueos y respuesta clara si falla un destino.
- **B. Confirmaciones independientes:** aprobar obra, cliente y presupuestos por separado. **Pros:** cada módulo conserva un límite sencillo y pueden aplicarse partes válidas. **Contras:** la interfaz debe explicar que la importación no está completa y qué ya se guardó; reintentar no debe aplicar otra vez los cambios previos.

**Decidir:** unidad de confirmación y semántica de omitir/descartar/vaciar un valor. No enviar valores ausentes como nulos destructivos ni modificar el cliente sin advertir que es compartido. No reasignar la obra a otro cliente automáticamente.

### P23 — Concurrencia entre extracción, revisión y datos vigentes

**Prioridad:** B. **Referencia:** `WorkRevision`/`If-Match`, `UpdateClientService` y catálogo.

**Problema y relevancia:** la revisión puede durar mientras otra persona cambia la obra, el cliente o un presupuesto. La obra ya dispone de ETag; la actualización actual de cliente no tiene un contrato equivalente de revisión. Leer un dato al analizar no garantiza que siga vigente al confirmar.

- **A. Revisión optimista por cada destino:** guardar revisiones/instantáneas base y comprobarlas al confirmar. **Pros:** detecta cambios sin mantener bloqueos durante el trabajo humano. **Contras:** exige ampliar el control del cliente y de modelos nuevos; el revisor debe resolver conflictos y no repetir ciegamente la petición.
- **B. Comparación de valores de cada campo modificado:** **Pros:** permite cambios no solapados y reduce rechazos por ediciones independientes. **Contras:** lógica de fusión más compleja, especialmente para colecciones y validaciones entre fechas/montos; debe realizarse de forma atómica.
- **C. Bloqueo durante toda la revisión:** **Pros:** aparente simplicidad de exclusión. **Contras:** bloquea a otros usuarios durante minutos o sesiones y es frágil ante cierres; no es adecuado sostener una transacción de base mientras el usuario revisa.

**Decidir:** granularidad y UX de conflicto. Conservar la revisión/captura ante un 412, releer valores y requerir aprobación nueva; no reintentar escrituras automáticamente ni omitir la comprobación por ejecutar desde una tarea interna.

### P24 — Duplicados, idempotencia y volver a analizar

**Prioridad:** B. **Referencia:** recuperabilidad existente de registro de versiones, no de extracción.

**Problema y relevancia:** el mismo archivo puede subirse dos veces, asociarse a dos obras o analizarse con otro perfil. Mismo nombre no implica mismos bytes; mismo hash no implica misma decisión de importación. Un reintento tras perder la respuesta no debe crear otro presupuesto ni aplicar cambios nuevamente.

- **A. Idempotencia explícita por solicitud y contexto:** identificar archivo/hash, obra, opciones, lector/perfil y selección revisada; repetir devuelve el resultado original. **Pros:** separa reintento de nueva intención y conserva trazabilidad. **Contras:** requiere persistencia de operaciones, fingerprint y tratamiento de parámetros distintos con la misma clave.
- **B. Deducción por hash de archivo:** **Pros:** detecta cargas repetidas y puede ahorrar almacenamiento/procesamiento. **Contras:** no basta para idempotencia de negocio y puede mezclar obras o revisiones si se usa como único identificador.

**Orientación:** A; B solo como optimización con referencias y permisos correctos. Decidir si reanalizar crea otro resultado inmutable o reemplaza un análisis no confirmado, y qué ocurre con correcciones manuales previas.

### P25 — Ejecución, límites de tiempo y recuperación del servidor

**Prioridad:** B antes de OCR o lotes. **Referencia:** timeout documental actual de 20 s y llamadas del cliente con timeout acotado.

**Problema y relevancia:** OCR y hojas grandes pueden exceder una petición HTTP. El escritorio asíncrono no convierte una operación de servidor en un trabajo durable. Cerrar el cliente o reiniciar el backend puede interrumpir procesos y dejar resultados pendientes.

- **A. Petición síncrona con límites bajos:** **Pros:** menos infraestructura y respuesta sencilla para archivos pequeños. **Contras:** timeouts y consumo prolongado de hilos; una desconexión no indica si el análisis terminó. Necesita idempotencia igualmente.
- **B. Trabajo persistido con ejecución local limitada y consulta de estado:** **Pros:** soporta cierre del cliente, progreso, cancelación y reanudación de revisión. **Contras:** necesita cola/estado durable, número acotado de procesos, detección de trabajos abandonados y política de reinicio; un `Future` en memoria no basta.
- **C. Cola/worker separado:** **Pros:** aislamiento y capacidad independiente. **Contras:** añade servicios y operación fuera de la arquitectura actual; no aprobado por defecto.

**Decidir:** concurrencia máxima, cancelación de procesos hijos, tiempos por fase y qué se reintenta al reiniciar. No volver a aplicar cambios de negocio por recuperar un análisis.

### P26 — Borradores y ciclo de vida de la extracción

**Prioridad:** B si hay revisión persistente; D si P01-A se entrega sin continuidad. **Referencia:** cancelación CA-14; MD-011 habla de generaciones, no de importaciones.

**Problema y relevancia:** no hay estados persistentes de extracción. Es necesario distinguir archivo cargado, análisis parcial, revisión, confirmado, cancelado y fallo; un error de OCR no equivale a un presupuesto confirmado. Guardar generaciones parciales quedó fuera del alcance y no autoriza por sí solo el modelo de borrador de extracción.

- **A. Revisión solo en memoria del cliente:** **Pros:** menor persistencia y gestión de retención. **Contras:** cerrar o perder conexión destruye trabajo; no encaja bien con tareas largas y hace difícil recuperar fallos.
- **B. Borrador de importación en servidor:** conservar decisiones por campo/fila, evidencia y revisiones base. **Pros:** continuidad, recuperación y diagnóstico fiable. **Contras:** hay que decidir propiedad/visibilidad, caducidad, limpieza y cambios de versión del analizador.

**Decidir:** estados y transiciones exactas, diferencia entre cancelar y eliminar, reanudación tras cambio de datos y si pueden aceptarse bloques independientes. No reutilizar `document_registrations` como entidad genérica de extracción: hoy registra versiones de plantillas y tiene otras referencias/invariantes.

### P27 — Revisión visual y ubicación del nuevo flujo

**Prioridad:** D, condiciona aceptación de P15/P22. **Referencia:** `WorkCaptureDialog`, `WorkInformationForm`, CA-11.

**Problema y relevancia:** el asistente actual adjunta archivos y captura manualmente; no muestra propuestas comparadas ni tablas presupuestales. El botón Generar documentos está deshabilitado y no debe habilitarse para disfrazar una extracción. Tablas grandes pueden bloquear JavaFX si se cargan completas.

- **A. Extender el asistente con un paso de análisis/revisión:** **Pros:** recorrido continuo desde la obra y menor navegación. **Contras:** sobrecarga el asistente si integra documentos, OCR, presupuestos y conflictos; debe separarse en componentes pequeños y guardar estado.
- **B. Ventana de extracción independiente abierta desde la obra/adjunto:** **Pros:** espacio para original, candidatos, comparaciones y conceptos paginados; mantiene responsabilidades. **Contras:** más navegación y necesidad de identificar claramente la obra y el trabajo activo.

**Decidir:** visor PDF o solo texto/evidencia localizada, selección de hojas/rangos y edición masiva. Un visor requiere evaluar su integración/dependencias y no debe ejecutar contenido activo. Mostrar «Analizando»/«Revisión pendiente»/«Confirmado» con significado real; el objetivo de cierre local rápido no implica procesamiento en menos de 100 ms.

## 6. Seguridad, archivos y operación

### P28 — Permisos de analizar, revisar, confirmar y consultar

**Prioridad:** B. **Referencia:** MD-014, V10 y política global vigente.

**Problema y relevancia:** hoy existen ocho permisos, sin uno de extracción/presupuestos. Adjuntar usa `WORKS_WRITE`, leer adjuntos `WORKS_READ`; editar cliente requiere `CLIENTS_WRITE`. Conceder administración de plantillas para poder analizar una fuente sería excesivo. La política actual tampoco implementa asignación de usuarios a obras.

- **A. Reutilizar permisos existentes por destino:** lectura de obra para ver, escritura de obra para incorporar y permisos adicionales para cliente/contrato si se escriben. **Pros:** menos cambios de roles y coherencia con casos de uso actuales. **Contras:** extracción costosa y alta de presupuestos quedarían agrupadas en permisos amplios; falta aclarar quién puede analizar sin confirmar.
- **B. Permisos específicos de extracción/revisión/confirmación:** **Pros:** permite separar capacidades y uso de recursos. **Contras:** migración y matriz de roles nuevas que deben aprobarse; no implica habilitar propuestas del Practicante ni introducir jerarquías implícitas.

**Decidir:** matriz de acciones × roles, consulta de resultados de otro usuario y permiso para reglas de mapeo. Verificar pertenencia de fuente/presupuesto a obra en cada endpoint. Añadir control por obra/cliente sería una ampliación distinta de la autorización global actual, no algo ya implementado.

### P29 — Validación de contenido, aislamiento y límites efectivos

**Prioridad:** B. **Referencia:** `AttachmentFormat`, `application.yml`, MD-001/MD-014.

**Problema y relevancia:** 10 MiB de subida y firma/formato básico no limitan páginas OCR, píxeles, hojas, celdas, fórmulas, relaciones externas o memoria de un lector. El verificador de adjuntos OOXML no es equivalente al analizador DOCX más restrictivo. Archivos aceptables para almacenar pueden ser peligrosos o demasiado costosos para procesar.

- **A. Límites y aislamiento por proceso dentro del despliegue actual:** **Pros:** menor cambio operativo y posibilidad de detener lectores. **Contras:** un proceso separado no es por sí solo un sandbox; necesita usuario restringido, temporales privados, sin red y límites reales de memoria/CPU/archivos/procesos.
- **B. Entorno de ejecución aislado dedicado:** **Pros:** mejores fronteras de recursos y seguridad. **Contras:** más complejidad, despliegue y operación; requiere aprobación arquitectónica.

**Decidir valores concretos:** tamaño por formato, páginas, resolución, hojas/filas/celdas, expansión ZIP, tiempo total/por página, concurrencia y salida máxima del lector. Definir rechazo de macros, vínculos externos, fórmulas activas, PDFs cifrados y documentos parciales; no ejecutar contenido ni aceptar rutas/comandos desde el archivo. No aumentar solo `max-file-size`: alinear proxy si existe, Spring, almacenamiento, lector y descarga.

### P30 — Guardar la fuente, conservar referencias y eliminar análisis

**Prioridad:** B para modelo/confirmación; D para retención operativa. **Referencia:** CA-05/MD-005 y borrado lógico de SPEC001.

**Problema y relevancia:** `work_attachments` ya conserva fuentes y hashes, mientras `document_files` pertenece al flujo de versiones. `ServerDocumentFileStore.relativePath` utiliza `registrations/UUID.docx` incluso para bytes PDF/XLSX. Falta decidir si reutilizar un adjunto, duplicarlo para extracción o generalizar metadatos; también qué pasa al eliminar un adjunto que respalda una importación confirmada.

- **A. Referenciar el adjunto existente desde el análisis/importación:** **Pros:** evita duplicación y aprovecha permisos, hash y vínculo con obra. **Contras:** necesita una forma interna segura de acceder a la evidencia cuando la descarga ordinaria del adjunto queda oculta; no debe eludir permisos ni borrar referencias.
- **B. Generalizar un registro común de archivo con referencias:** **Pros:** uniformidad de metadatos, formatos y conservación. **Contras:** exige migrar consumidores sin romper rutas históricas, aclarar propiedad y contar referencias antes de purgar; no es una refactorización gratuita.
- **C. Copiar el binario por extracción:** **Pros:** independencia aparente de cada operación. **Contras:** duplica espacio y crea dudas de cuál copia es original; complica limpieza/backup y no evita reglas de integridad.

**Decidir:** conservación desde la carga o solo al confirmar, archivos temporales al cancelar y retención de texto OCR/propuestas. La eliminación lógica no revoca datos ya confirmados; borrar una fuente no debe borrar presupuestos/valores por cascada. No renombrar/mover históricos por su extensión interna sin migración aprobada, ni confiar en esa extensión para escoger lector. Mantener el hash del original.

### P31 — Consistencia, auditoría, migraciones y puesta en servicio

**Prioridad:** B para persistencia; D para operación. **Referencia:** Flyway V1–V14 y unidad DB/archivos del DAS.

**Problema y relevancia:** publicar un archivo y confirmar una transacción PostgreSQL no es una sola operación atómica. Pueden quedar archivos huérfanos, propuestas sin fuente o confirmaciones de resultado incierto. Los registros actuales no ofrecen toda la auditoría de «quién aceptó qué valor y con qué evidencia». La numeración siguiente no debe fijarse sin revisar nuevamente las migraciones.

- **A. Operación durable en fases, publicación verificable y finalización idempotente:** **Pros:** permite recuperar fallos y distinguir recibido/analizado/confirmado; compatible con el patrón ya usado para versiones. **Contras:** exige reconciliación y una política para temporales/huérfanos; reutilizar el patrón no significa compartir su tabla de operaciones.
- **B. Archivo primero y una transacción final con compensación:** **Pros:** flujo inicial más corto. **Contras:** una caída puede impedir la compensación; sin reconciliación deja basura y no garantiza conocer si la confirmación se aplicó.

**Decidir:** datos mínimos de auditoría (actor/fecha/destino/antes/después/fuente/lector/revisión), límites de información sensible en logs y quién puede consultarlos. Nuevas migraciones solo después de aprobar el modelo; probar base limpia y actualización desde V14 con archivos/referencias. No editar V14 aplicada, no reutilizar la autorización anterior de limpieza para borrar datos nuevos y no aplicar cambios a la instalación persistente sin autorización específica y respaldo DB+archivos verificable.

## 7. Interfaz con la futura generación: registrar sin implementar

### P32 — Versiones seleccionables, colecciones y frontera de salida

**Prioridad:** F; solo el contrato de datos de extracción se define ahora. **Referencia:** HU-004–HU-006/MD-006–MD-013 frente a SPEC001.

**Problema y relevancia:** la SPEC escrita mezcla extracción con selección de versiones históricas, bloques repetibles, vista previa DOCX/PDF, conversiones, borradores e historial de generaciones. Implementarlos ahora contradice la instrucción actual. No obstante, persistir conceptos sin orden o perder procedencia complicaría su consumo futuro.

- **A. Entregar datos confirmados y colecciones estructuradas mediante contratos propios:** **Pros:** permite aceptar extracción independientemente, conserva orden/tipos/identidad y no modifica la seguridad del analizador de plantillas. **Contras:** el motor futuro deberá acordar cómo vincular colecciones y qué presupuesto/revisión consumir; no se puede demostrar todavía una salida Word completa.
- **B. Definir ahora interfaces mínimas futuras sin implementarlas:** **Pros:** reduce cambios posteriores si se limita a identidad, revisiones y tipos de datos. **Contras:** puede sobreconstruir una API especulativa; los nombres de tablas/endpoints de generación no deben fijarse como requisito de extracción.
- **C. Implementar también generación para probar el recorrido completo:** **Pros:** valida integración de extremo a extremo. **Contras:** fuera de alcance expresamente; requiere otra autorización, dependencias y criterios de fidelidad. No se recomienda para esta SPEC.

**Aplazar expresamente:** elegir última versión versus cualquier versión, una plantilla por tipo, sintaxis/semántica de bloques repetibles, tratamiento de opcionales en salida, motor de renderizado, conversión PDF, fidelidad Word, instantáneas de generaciones, exportación individual/ZIP y eliminación de generados. No habilitar el botón Generar documentos. `{{CLAVE}}` y el rechazo de Jinja libre de SPEC001 permanecen intactos.

## 8. Calidad y condición de entrega de la extracción

### P33 — Medir exactitud sin confundir lectura con aceptación de negocio

**Prioridad:** B para aceptar; D para ampliar cobertura. **Referencia:** CA-01–CA-15 y nueva frontera de alcance.

**Problema y relevancia:** una suite que solo comprueba que el lector devuelve texto no prueba que identifique correctamente campos o conceptos. Tampoco es suficiente un porcentaje general: un error en importe/cliente puede ser más grave que omitir un texto opcional. No hay umbrales, corpus anotado ni criterios de aceptación de la revisión humana definidos.

- **A. Casos anotados por perfil/campo/fila con resultado esperado:** medir aciertos, omisiones y falsos positivos, y comprobar persistencia solo de lo aceptado. **Pros:** evidencia reproducible y diagnóstico de regresiones; separa exactitud del OCR, correspondencia y validación. **Contras:** requiere preparar/verificar anotaciones y muestras representativas.
- **B. Aceptación manual por archivos reales sin métricas acordadas:** **Pros:** rápida para comprobar utilidad y experiencia del responsable. **Contras:** no basta para regresión ni para comparar cambios de lector; puede ocultar errores raros y depende del conjunto observado.

**Orientación:** A más B, sin prometer reconocimiento universal ni establecer porcentajes ficticios. Definir umbrales por campo crítico y comportamiento permitido cuando no se reconoce algo; preferir «requiere revisión» a una asociación incorrecta presentada como cierta.

**Matriz mínima por concretar:**

- PDF digital, escaneado, mixto, rotado, ilegible, cifrado/dañado; OCR incorrecto y fallo/timeout del proceso.
- XLS si se mantiene; XLSX con varias hojas, celdas fusionadas, identificadores con ceros, fórmulas sin valor utilizable, filas ocultas, subtotales y alternativas.
- Fechas ambiguas, negativos/cero/alta precisión, separadores regionales, nombres reales inválidos para el destino y campos sin correspondencia.
- Cancelación sin cambios, confirmación selectiva, permisos por destino, cliente compartido, modificaciones concurrentes y catálogo cambiado.
- Reintentos, doble clic, pérdida de respuesta, reanálisis con otro perfil, reinicio del servidor y referencia a fuente eliminada lógicamente.
- Migración desde esquema anterior, integridad de bytes, ausencia de ejecuciones activas/red desde lectores y límites de recursos.
- Revisión JavaFX con datos paginados, captura conservada tras fallo y aceptación humana del recorrido. Una compilación/HTTP no sustituye la aceptación visual.

### 8.1. Redistribución provisional de la trazabilidad

| Elementos de SPEC002 actual | Situación con el alcance corregido |
|---|---|
| HU-001 / CA-01–CA-05 / MD-001–MD-002 | Extracción de PDF, seguridad, OCR y conservación. Mantener intención; cerrar P02/P03/P07–P09/P29/P30. |
| HU-002 / CA-06–CA-10 / MD-003 | Importación de presupuestos. Mantener intención salvo recorte aprobado; cerrar P10–P14/P18. |
| HU-003 / CA-11–CA-15 / MD-004–MD-005 | Revisión e incorporación. Alcance exacto a confirmar en P01 y P19–P26; no darlo por implementado por existir formularios de obra. |
| MD-014 | Seguridad y errores se mantienen, aplicados a extracción/importación, no a generación/exportación. |
| HU-004 / CA-16–CA-20 / MD-006–MD-008 | Selección para generación diferida. La validación de datos y persistencia de conceptos permanece, pero no la sustitución de bloques ni el cambio de política de versiones. |
| HU-005 / CA-21–CA-25 / MD-009–MD-010 | Vista previa de salida, generación y conversiones fuera de esta SPEC. Revisar una propuesta no satisface esos criterios antiguos. |
| HU-006 / CA-26–CA-30 / MD-011–MD-013 | Borradores/historial/exportación de generaciones fuera. La continuidad/auditoría de extracción requiere criterios propios, no reutilizar estos como si fueran iguales. |

La nueva condición de entrega deberá demostrar **fuente → análisis → candidatos con evidencia → revisión → incorporación válida si se aprueba P01-B → consulta de datos confirmados**, junto con fallos seguros. No exigirá producir un DOCX/PDF nuevo. Mantener IDs antiguos como trazabilidad histórica al reescribir la spec, evitando renumerarlos silenciosamente.

## 9. Orden de decisiones y preguntas para continuar

1. **Cerrar el producto de esta entrega:** P01, P02 y P03. ¿Solo identificar o también confirmar/persistir? ¿PDF/XLS/XLSX y OCR completos en esta entrega? ¿Qué archivos exactos sirven de referencia?
2. **Cerrar significado y destinos:** P04–P06 y P13–P21. ¿Qué campos se extraen, cómo se mapean, cómo se conserva un presupuesto y qué cambios contractuales no pueden aplicarse? Resolver especialmente nombres reales frente a nombres técnicos.
3. **Cerrar confirmación y autorización:** P22–P24/P28. ¿Unidad atómica o por bloques? ¿Qué permisos y conflictos se comprobarán? ¿Qué define repetición de la misma operación?
4. **Elegir tecnología y ejecución con límites:** P07–P12/P25/P29, usando el corpus aprobado. Aprobar dependencias/binarios y probar sus restricciones antes de comprometer una arquitectura.
5. **Cerrar experiencia y conservación:** P15/P26/P27/P30/P31. ¿Cómo se revisa evidencia, se retoma, cancela y conserva el original? ¿Cómo se recupera una operación interrumpida?
6. **Acordar aceptación y redactar `plan.md`:** P33 y la trazabilidad de §8.1. Separar investigación, implementación, migraciones, UI, pruebas y puesta en servicio; no iniciar código o migraciones solo por redactar este inventario.
7. **Guardar P32 para la SPEC de generación:** ninguna decisión de renderizado, conversión o selección histórica debe bloquear un lector de fuentes independiente.

## 10. Qué no autoriza este documento

- No aprueba bibliotecas, servicios externos, OCR en la nube, cambios de roles ni ampliaciones de arquitectura.
- No modifica `spec.md`, README, MEMORY, ERS, DAS o Diccionario. Sus diferencias están registradas para una actualización posterior autorizada.
- No ejecuta extracción sobre documentos reales, ni cambia base persistente, archivos, migraciones o configuración.
- No reutiliza autorizaciones anteriores para limpiar datos ni restaura archivos eliminados localmente.
- No declara ninguna fase del motor implementada ni aceptada. No crea commits, push ni PR.
