# Spec 002 — Extracción y revisión de fuentes

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Estado:** Implementado parcialmente; verificación integral y aceptación pendientes
**Actualización:** 2026-11-20
**Issue:** pendiente de referencia
**Plan:** [plan.md](plan.md) · **Seguimiento:** [review.md](review.md)

## Resultado esperado

Un usuario autorizado analiza documentos de una obra existente, revisa sus datos con evidencia y confirma selectivamente cambios en obra, cliente y presupuestos. También puede capturar datos manualmente y retomar una revisión.

## 1. Contexto y fuentes

- Los documentos de entrada contienen información contractual y presupuestal que debe revisarse antes de incorporarla. El resultado es información consultable, no documentos generados.
- Fuentes: [Spec 001](../001/spec.md), ERS/DAS/Diccionario en `docs/informative/`, código, configuración y migraciones.
- Código presente: módulos `extraction` y `budgets` del servidor, ventana JavaFX, lectores y migraciones V15–V18. Su presencia no demuestra aceptación ni operación segura en un despliegue concreto.
- Los documentos iniciales y la versión histórica de esta spec contemplaban generación y etapas. El alcance aprobado de esta entrega no las incluye. El Diccionario inicial no define el modelo completo de partidas y revisiones.

## 2. Alcance

**Incluye:** fuentes PDF digitales, escaneados o mixtos, XLS, XLSX y DOCX; OCR local; mapeo asistido y captura manual; evidencia, borradores y confirmaciones independientes; presupuestos y revisiones consultables.

**No incluye:** generación/renderizado, vista previa de salida, exportación o historial de generaciones; creación/reasignación automática de obra o cliente; convenios, estimaciones, aprobaciones interpersonales, servicios remotos ni compatibilidad universal. DOC, DOCM, XLSM, CSV e imágenes independientes están fuera del alcance.

La extracción no requiere paquete, etapa ni versión de plantilla. Un DOCX fuente se lee como contenido, no como plantilla. La captura manual forma parte de la revisión, pero la API actual exige al menos un adjunto para crear el trabajo.

## 3. Actores, permisos y precondiciones

| Actor | Permiso necesario | Precondiciones / límites |
|---|---|---|
| Usuario que consulta trabajos y evidencia | `WORKS_READ`; `CLIENTS_READ` para leer datos de cliente. | Obra y cliente existentes. |
| Usuario que crea, revisa o cancela trabajos y confirma obra o presupuesto | `WORKS_READ` y `WORKS_WRITE`. | Las fuentes deben pertenecer a la obra. |
| Usuario que confirma datos de cliente | `WORKS_READ`, `CLIENTS_READ` y `CLIENTS_WRITE`. | Se advierte que el cliente es compartido. |
| Administrador del catálogo documental | Permisos de [Spec 001](../001/spec.md). | La extracción no concede permisos de administración del catálogo. |

El servidor autoriza cada operación; ocultar controles no reemplaza la autorización. No se añade asignación usuario–obra ni permisos nuevos por esta spec.

## 4. Reglas y datos

- **Candidatos y evidencia:** la lectura no confirma datos. Conserva fuente/hash, ubicación, literal, propuesta, alternativas y diagnóstico; no elige por orden de llegada.
- **Precedencia sugerida:** contrato para número, contratista, fechas y montos contractuales, antes de impuestos e impuesto; Excel seleccionado para conceptos, unidades, cantidades y precios. Otros campos o contradicciones en la fuente preferida no se eligen automáticamente.
- **Mapeo:** solo destinos admitidos y adicionales existentes; no escribir IDs/relaciones, crear campos globales ni ejecutar rutas o expresiones del archivo. Los perfiles de obra declarativos, opcionales y versionados solo sugieren mapeos.
- **Correcciones:** conservar nombres técnicos y claves históricas; las denominaciones documentales son atributos separados. Completar o corregir requiere decisión humana; corregir un valor existente exige motivo y justificación de 10–2000 caracteres. No se infiere una modificación contractual ni se gestionan convenios.
- **Hojas y conceptos:** agrupar explícitamente hojas como partes, resúmenes o alternativas. Los conceptos son ordenados, admiten códigos textuales repetidos y requieren descripción; clave, unidad, cantidad, precio e importe pueden faltar. Ausencia no es cero; notas y resúmenes no se suman automáticamente como conceptos.
- **Interpretación:** conservar fórmula, caché y formato sin evaluar fórmulas, macros ni vínculos. Distinguir caché ausente, cero, error y posible obsolescencia. Fechas y números ambiguos requieren convención explícita; no inventar moneda, impuesto ni precisión perdida de XLS.
- **Control de importes:** `CONTROL_V1` calcula cantidad × precio con decimales exactos, redondea cada concepto a dos decimales `HALF_UP` y suma controles. Conserva total declarado y suma exacta; muestra toda diferencia no nula y exige justificación, sin corregir el original. Valores inválidos o incoherentes bloquean el bloque afectado.
- **Confirmación e historial:** obra, cliente y cada presupuesto se confirman por separado con `If-Match`, revisión del destino y clave idempotente. Cancelar lo pendiente no revierte bloques confirmados; las revisiones confirmadas de presupuesto son inmutables y la principal se elige expresamente.
- **Estados:** los estados técnicos `QUEUED`, `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`, `CANCELLED` son independientes de `DRAFT`, `CONFIRMED`, `CANCELLED` de revisión. Terminar la lectura no implica aceptación; reanalizar crea otro trabajo y conserva el anterior.
- **Retención y límites:** conservar y verificar la fuente original antes de confirmar. No hay purga automática aprobada de fuentes, borradores o auditoría; el scratch es temporal. Los límites y el aislamiento requeridos están en [plan.md](plan.md).

## 5. Requisitos funcionales

| ID | Requisito | Prioridad | Fuente |
|---|---|---|---|
| MD-001 | Validar y analizar PDF, XLS, XLSX y DOCX fuente asociados a la obra. | Must | ERS RF-04; P02/P29 para formatos y pertenencia. |
| MD-002 | Leer PDF digital y aplicar OCR local donde corresponda, con procedencia y advertencias. | Must | ERS RF-04; P07–P09 para OCR y evidencia. |
| MD-003 | Proponer y mapear hojas, partidas, conceptos y montos sin fusionar alternativas. | Must | ERS RF-04; P10–P15 para mapeo y alternativas. |
| MD-004 | Revisar, corregir, capturar manualmente, excluir y confirmar por destino. | Must | ERS RF-03/RF-05; RF-09 solo en cuanto a corrección de datos; P01/P21–P23. |
| MD-005 | Conservar fuentes y presupuestos vinculados, con historial y evidencia. | Must | ERS RF-03; P13–P15/P30–P31 para historial y evidencia. |
| MD-014 | Autorizar en servidor, limitar recursos y comunicar fallos recuperables. | Must | ERS RF-26 para permisos; P28–P31 para límites y recuperación. |

RF-03, RF-04, RF-05 y RF-26 se aplican aquí solo a los aspectos incluidos en esta entrega. RF-09 contempla también una nueva generación documental, que permanece fuera de alcance. El ERS no especifica todas las capacidades detalladas; las decisiones P citadas proceden del [plan](plan.md) y [seguimiento](review.md), no se presentan como requisitos del ERS.

Los IDs históricos MD-006–MD-013 corresponden a funcionalidades futuras y permanecen reservados. Los criterios de aceptación se renumeraron para esta versión; los CA de versiones anteriores no deben interpretarse con la numeración actual. La selección de plantillas, generación y exportación siguen fuera de alcance.

## 6. Historias y criterios de aceptación

Las historias expresan el comportamiento esperado, no su aceptación: la implementación es parcial y su verificación integral sigue pendiente.

### HU-001 — Analizar fuentes PDF

Como usuario autorizado, quiero analizar documentos PDF de una obra y revisar sus propuestas con evidencia, para incorporar información confiable.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-01 | Un PDF digital reconocible de una obra. | Solicito el análisis. | Se proponen datos y origen sin modificar datos vigentes. |
| CA-02 | Un PDF escaneado o mixto. | Solicito el análisis. | Se aplica OCR donde corresponda; los resultados requieren revisión. |
| CA-03 | Una fuente con datos ausentes, ambiguos o inconsistentes. | Reviso el análisis. | Se identifican los pendientes y se conservan propuestas válidas. |
| CA-04 | Un PDF cifrado, dañado, ilegible o no admitido. | Solicito el análisis. | Se informa el impedimento; no se confirman valores y se permite sustituir o capturar manualmente. |
| CA-05 | Una fuente válida de la obra. | La incorporo. | Se conserva el original vinculado a la obra y su clasificación. |
| CA-06 | Una o varias fuentes contienen valores distintos para un dato. | Consulto el análisis. | Veo literales, propuestas, ubicaciones y alternativas; la precedencia solo sugiere cuando corresponde y no decide por orden de llegada. |
| CA-07 | La lectura de un PDF ha terminado. | Consulto el trabajo. | Distingo el estado técnico del de revisión y ningún candidato se aplica hasta una confirmación explícita. |

### HU-002 — Importar presupuestos y DOCX fuente

Como usuario autorizado, quiero analizar presupuestos y documentos DOCX, para revisar sus datos y mantener separadas las alternativas.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-08 | Un XLS/XLSX con una o varias hojas. | Lo cargo. | Se identifican hojas y se permite seleccionarlas y agruparlas. |
| CA-09 | Una hoja con conceptos. | Solicito la extracción. | Se proponen clave, descripción, unidad, cantidad, precio e importe con agrupaciones identificables. |
| CA-10 | Una importación con fórmulas o totales. | La reviso. | Se distinguen filas de cálculo y discrepancias sin sustituir valores declarados. |
| CA-11 | Dos hojas identificadas como alternativas. | Confirmo la selección. | Se conservan separadas, sin fusionarlas ni sustituirlas automáticamente. |
| CA-12 | Una estructura desconocida o datos ambiguos. | Reviso la importación. | Se indican los pendientes y puedo corregir, excluir o cancelar sin perder datos vigentes. |
| CA-13 | Un DOCX fuente con texto y tablas, sin marcadores. | Lo analizo. | Se presentan contenido y ubicación estructural, sin inventar páginas Word. |
| CA-14 | Hojas con partes, resúmenes o alternativas. | Las selecciono y agrupo. | Puedo indicar su agrupación; notas y resúmenes no se suman automáticamente como conceptos y las alternativas permanecen separadas. |
| CA-15 | Una celda con fórmula o caché ausente, cero, con error o posiblemente obsoleta. | Reviso la lectura. | Se conserva la fórmula, la caché disponible y el formato, distinguiendo esas situaciones sin evaluar fórmulas, macros ni vínculos. |
| CA-16 | Un concepto con algún valor faltante o una fecha o cifra ambigua. | Reviso los datos. | La ausencia no se convierte en cero; la descripción es obligatoria y las convenciones ambiguas requieren una decisión explícita. |
| CA-17 | Un importe declarado distinto del control calculado con `CONTROL_V1`. | Reviso y confirmo el bloque. | Se muestran ambos importes y la diferencia, se conserva el declarado y se exige justificación explícita antes de confirmar. |

### HU-003 — Revisar, confirmar y retomar

Como usuario autorizado, quiero revisar los datos, guardar mi avance y confirmar los cambios, para incorporar solo información válida.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-18 | Existen candidatos. | Abro la revisión. | Veo propuestas, valores actuales y evidencia; puedo corregir o descartar. |
| CA-19 | Un candidato difiere del dato vigente. | Confirmo la selección. | Solo se aplican cambios explícitamente aprobados y válidos. |
| CA-20 | Datos incompletos o inválidos. | Intento confirmarlos. | Se explican las validaciones y se conserva la revisión para corregirla. |
| CA-21 | Una revisión con cambios pendientes. | Cancelo sin confirmar esos cambios. | Obra y cliente permanecen intactos respecto de lo pendiente; los bloques confirmados antes no se revierten. |
| CA-22 | Un presupuesto confirmado. | Consulto su revisión. | Se obtiene su colección ordenada de conceptos, sin convertirlos en campos escalares. |
| CA-23 | Una revisión guardada. | La reabro. | Recupero decisiones, presupuestos y revisiones base; cambios concurrentes exigen nueva revisión. |
| CA-24 | Una confirmación cuya respuesta se perdió. | Reintento la misma operación. | Se devuelve el resultado original; otra intención con la misma clave se rechaza. |
| CA-25 | Un usuario sin permiso o una fuente de otra obra. | Se solicita la operación. | El servidor la rechaza sin modificar destinos. |
| CA-26 | Una lectura en curso. | Falla, se cancela o se reinicia el servidor. | Se informa el estado; no se aplican datos automáticamente ni se repiten confirmaciones. |
| CA-27 | Un presupuesto nuevo o una revisión. | Lo confirmo. | Se conserva la revisión anterior, si existe, y la principal solo cambia por elección explícita. |
| CA-28 | Un candidato o dato faltante. | Lo reviso. | Puedo mapearlo a un destino admitido o adicional existente compatible, capturar o corregir su valor y excluirlo; no se crean campos globales ni se escriben identificadores o relaciones. |
| CA-29 | Una corrección de un valor existente. | La apruebo. | Debo indicar motivo y justificación de 10–2000 caracteres; la corrección no se interpreta automáticamente como convenio. |
| CA-30 | Datos seleccionados para obra, cliente o presupuesto. | Confirmo el destino. | Solo se confirma el destino seleccionado con sus permisos y revisión vigentes; confirmar cliente advierte que es compartido y no confirma otros bloques. |
| CA-31 | La fuente original no está disponible o no supera la verificación. | Intento confirmar. | Se informa el impedimento y no se confirma el bloque afectado. |
| CA-32 | Fuentes de una revisión anterior. | Solicito reanalizarlas. | Se crea otro trabajo y se conserva el anterior, sin sobrescribir sus decisiones ni repetir confirmaciones. |

## 7. Requisitos no funcionales y restricciones

| ID | Requisito/restricción | Verificación |
|---|---|---|
| NFR-001 | Lector aislado sin red ni credenciales, con límites efectivos por trabajo y proceso; fallar cerrado sin aislamiento. Un subproceso o `-Xmx` no bastan. | ¿Se demuestra el aislamiento y sus límites en el destino real, y se rechaza la ejecución cuando no están disponibles? |
| NFR-002 | Procesamiento y red no bloquean JavaFX; se respetan los contratos vigentes de sesión, CSRF, permisos, paginación, conservación de captura ante errores y baja lógica. | ¿El recorrido en Windows mantiene la interfaz operable y respeta esos contratos, incluso ante errores? |
| NFR-003 | Limitar archivos hostiles, tiempo, memoria, expansión y salida; no registrar contenido sensible. | ¿Las pruebas por formato y en el destino real comprueban las fronteras y la ausencia de contenido sensible en logs? |
| NFR-004 | Medir lectura/OCR y mapeo por campo o fila con corpus anotado, sin prometer precisión universal. | ¿Se registran resultados y límites por campo o fila sobre el corpus acordado? |

## 8. Decisiones abiertas

| ID | Pregunta / alternativas | Impacto | Responsable / estado |
|---|---|---|---|
| D-001 | ¿Qué umbrales de lectura/OCR y mapeo se acuerdan, y qué muestras reales se autorizan por archivo? | Aceptación y representatividad del corpus; no autoriza usar datos reales sin permiso. | Responsable por definir; pendiente. |
| D-002 | ¿Cuáles serán las políticas definitivas de operación, respaldo y retención? | Despliegue y conservación de fuentes, borradores y auditoría. | Responsable por definir; pendiente. |
| D-003 | ¿Cómo se resuelve la referencia RF-28 no localizada en ERS CU-07? | Trazabilidad de exportación futura, fuera de esta entrega; RF-24 es referencia provisional. | Responsable por definir; pendiente. |

**Verificaciones pendientes:** aislamiento agregado independiente por worker, límites efectivos y recuperación en el despliegue; licencias y seguridad nativas y del grafo integrado; ampliación del corpus para OCR degradado/rotado, páginas mixtas, tablas, contenido activo y cachés XLS; comprobación visual en Windows y de permisos reales. No son decisiones ya resueltas ni evidencia de aceptación.

## 9. Condición de aceptación

**Datos y entorno:** usar datos sintéticos anotados y muestras reales solo con autorización específica por archivo. Ejecutar el recorrido de escritorio en Windows y verificar el aislamiento y los límites en el entorno real de destino, sin asumir que la configuración experimental es apta para despliegue.

**Pruebas automáticas:** comprobar lectores y corpus por formato, mapeo, validaciones, permisos, conflictos, idempotencia, recuperación, cancelación y límites de procesamiento; ejecutar las regresiones aplicables según [plan.md](plan.md). Registrar comandos, entorno, resultados y limitaciones en [review.md](review.md).

**Aceptación manual:** con un usuario autorizado, analizar PDF digital, escaneado y mixto, XLS/XLSX y DOCX; revisar evidencia, mapear y capturar datos; guardar y reanudar; confirmar por destino y consultar el presupuesto y sus revisiones. Ejercitar permisos, conflictos, fallos, cancelación y reintentos, incluidos usuarios sin permiso. Registrar actor, entorno, resultados y evidencia por criterio en [review.md](review.md).

La aceptación exige ambas verificaciones; las pruebas automáticas no sustituyen el recorrido manual. La aceptación manual y la verificación integral siguen pendientes.
