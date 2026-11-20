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
- Fuentes: decisiones P01–P33 y DP-01–DP-38 registradas en el plan y seguimiento, [Spec 001](../001/spec.md), ERS/DAS/Diccionario en `docs/informative/`, código, configuración y migraciones.
- Código presente: módulos `extraction` y `budgets` del servidor, ventana JavaFX, lectores y migraciones V15–V18. Su presencia no demuestra aceptación ni operación segura en un despliegue concreto.
- Los documentos iniciales y la versión histórica de esta spec contemplaban generación y etapas. El alcance aprobado de esta entrega no las incluye. El Diccionario inicial no define el modelo completo de partidas y revisiones.

## 2. Alcance

**Incluye:** fuentes PDF digitales, escaneados o mixtos, XLS, XLSX y DOCX; OCR local; mapeo asistido y captura manual; evidencia, borradores y confirmaciones independientes; presupuestos y revisiones consultables.

**No incluye:** generación/renderizado, vista previa de salida, exportación o historial de generaciones; creación/reasignación automática de obra o cliente; convenios, estimaciones, aprobaciones interpersonales, servicios remotos ni compatibilidad universal. DOC, DOCM, XLSM, CSV e imágenes independientes están fuera del alcance.

La extracción no requiere paquete, etapa ni versión de plantilla. Un DOCX fuente se lee como contenido, no como plantilla. La captura manual forma parte de la revisión, pero la API actual exige al menos un adjunto para crear el trabajo.

## 3. Actores, permisos y precondiciones

| Actor / acción | Permiso y precondición |
|---|---|
| Consultar trabajo y evidencia | `WORKS_READ`; obra y cliente existentes. La lectura de datos de cliente requiere `CLIENTS_READ`. |
| Crear, revisar, cancelar; confirmar obra o presupuesto | `WORKS_READ` y `WORKS_WRITE`. Las fuentes deben pertenecer a la obra. |
| Confirmar cliente | `WORKS_READ`, `CLIENTS_READ` y `CLIENTS_WRITE`; advertir que el cliente es compartido. |
| Administrar el catálogo documental | Permisos de Spec 001; extraer no concede administración del catálogo. |

El servidor autoriza cada operación; ocultar controles no reemplaza la autorización. No se añade asignación usuario–obra ni permisos nuevos por esta spec.

## 4. Reglas y datos

- Una lectura produce candidatos, no datos confirmados. Conservar fuente/hash, ubicación, literal, valor propuesto, alternativas y diagnóstico; no elegir por orden de llegada.
- Precedencia v1 **como sugerencia**: contrato para número, contratista, fechas contractuales y montos contratado/antes de impuestos/impuesto; Excel seleccionado para conceptos, unidades, cantidades y precios. En otros campos o ante contradicción en la fuente preferida, no elegir automáticamente.
- Permitir mapeo a destinos admitidos y adicionales existentes; no escribir IDs/relaciones, crear campos globales ni ejecutar rutas o expresiones del archivo. Perfiles de obra declarativos, opcionales y versionados solo sugieren mapeos.
- Conservar nombres técnicos y claves históricas; las denominaciones documentales son atributos separados. No inferir una modificación contractual: completar o corregir captura requiere decisión humana; una corrección de un valor existente exige motivo y justificación de 10–2000 caracteres. Los convenios quedan fuera.
- Seleccionar y agrupar explícitamente hojas como partes, resúmenes o alternativas. Conceptos ordenados y códigos textuales repetibles; descripción obligatoria; clave, unidad, cantidad, precio e importe pueden faltar. Ausencia no es cero. No sumar automáticamente notas o resúmenes como conceptos.
- Conservar fórmula, caché y formato sin evaluar fórmulas, macros o vínculos. Caché ausente, cero, error y posible dato obsoleto son distintos. Fechas y números ambiguos necesitan convención explícita; no inventar moneda, impuesto ni precisión perdida de XLS.
- `CONTROL_V1` compara cantidad × precio con decimales exactos, redondea cada concepto a dos decimales `HALF_UP` y suma controles; conserva el declarado y la suma exacta. Una diferencia no nula se muestra y requiere justificación explícita; no se corrige el original. Valores inválidos o incoherentes bloquean el bloque afectado.
- Obra, cliente y cada presupuesto se confirman por separado. `If-Match`, revisión del destino y clave idempotente evitan sobrescrituras y duplicados; cancelar lo pendiente no revierte bloques confirmados. Revisiones de presupuesto confirmadas son inmutables; la principal se elige expresamente.
- Estados técnicos `QUEUED`, `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`, `CANCELLED` son independientes de `DRAFT`, `CONFIRMED`, `CANCELLED` de la revisión. Lectura terminada no significa aceptación; reanálisis crea otro trabajo y conserva el anterior.
- La fuente original se conserva y verifica antes de confirmar. No hay purga automática aprobada de fuentes, borradores o auditoría; el scratch de cada ejecución es temporal. Los límites y el aislamiento requeridos están en [plan.md](plan.md).

## 5. Requisitos funcionales

| ID | Requisito | Prioridad / fuente |
|---|---|---|
| MD-001 | Validar y analizar PDF, XLS, XLSX y DOCX fuente asociados a la obra. | Must · P02/P29 |
| MD-002 | Leer PDF digital y aplicar OCR local donde corresponda, con procedencia y advertencias. | Must · P07–P09 |
| MD-003 | Proponer y mapear hojas, partidas, conceptos y montos sin fusionar alternativas. | Must · P10–P15 |
| MD-004 | Revisar, corregir, capturar manualmente, excluir y confirmar por destino. | Must · P01/P21–P23 |
| MD-005 | Conservar fuentes y presupuestos vinculados, con historial y evidencia. | Must · P13–P15/P30–P31 |
| MD-014 | Autorizar en servidor, limitar recursos y comunicar fallos recuperables. | Must · P28–P31 |

Los IDs históricos MD-006–MD-013 y CA-16–CA-30 describían selección de plantillas, generación o exportación futuras; quedan reservados fuera de esta entrega, sin reasignarlos. CA-22 no figuraba en la versión histórica.

## 6. Historias y criterios de aceptación

### HU-001 — Analizar fuentes PDF

| ID | Dado / cuando | Entonces |
|---|---|---|
| CA-01 | PDF digital reconocible de una obra; solicito análisis. | Se proponen datos y origen sin modificar datos vigentes. |
| CA-02 | PDF escaneado o mixto; solicito análisis. | Se aplica OCR donde corresponda; los resultados requieren revisión. |
| CA-03 | Hay datos ausentes, ambiguos o inconsistentes. | Se identifican para revisión y se conservan propuestas válidas. |
| CA-04 | PDF cifrado, dañado, ilegible o no admitido. | Se informa el impedimento; no se confirman valores y se permite sustituir o capturar manualmente. |
| CA-05 | El usuario incorpora una fuente válida. | Se conserva el original vinculado a la obra y su clasificación. |

### HU-002 — Importar presupuestos y DOCX fuente

| ID | Dado / cuando | Entonces |
|---|---|---|
| CA-06 | XLS/XLSX con una o varias hojas; lo cargo. | Se identifican hojas y se permite seleccionarlas y agruparlas. |
| CA-07 | Hoja con conceptos; solicito extracción. | Se proponen clave, descripción, unidad, cantidad, precio e importe con agrupaciones identificables. |
| CA-08 | Hay fórmulas o totales; reviso la importación. | Se distinguen filas de cálculo y discrepancias sin sustituir valores declarados. |
| CA-09 | Dos hojas son alternativas; confirmo. | Se conservan separadas, sin fusionarlas ni sustituirlas automáticamente. |
| CA-10 | Hay estructura desconocida o datos ambiguos. | Se indican los pendientes y puedo corregir, excluir o cancelar sin perder datos vigentes. |
| CA-31 | DOCX fuente con texto y tablas, sin marcadores; lo analizo. | Se presentan contenido y ubicación estructural, sin inventar páginas Word. |

### HU-003 — Revisar, confirmar y retomar

| ID | Dado / cuando | Entonces |
|---|---|---|
| CA-11 | Existen candidatos; abro revisión. | Veo propuestas, valores actuales y evidencia; puedo corregir o descartar. |
| CA-12 | Un candidato difiere del dato vigente; confirmo. | Solo se aplican cambios explícitamente aprobados y válidos. |
| CA-13 | Selecciono datos incompletos o inválidos. | Se explican las validaciones y se conserva la revisión para corregirla. |
| CA-14 | Cancelo sin confirmar. | Obra y cliente permanecen intactos; si hubo bloques confirmados antes, no se revierten. |
| CA-15 | Hay presupuesto confirmado; consulto su revisión. | Se obtiene su colección ordenada de conceptos, sin convertirlos en campos escalares. |
| CA-32 | Guardo una revisión y la reabro. | Recupero decisiones, presupuestos y revisiones base; cambios concurrentes exigen nueva revisión. |
| CA-33 | Reintento una confirmación idéntica tras perder respuesta. | Se devuelve el resultado original; otra intención con la misma clave se rechaza. |
| CA-34 | Usuario sin permiso o fuente de otra obra. | El servidor rechaza la operación sin modificar destinos. |
| CA-35 | La lectura falla, se cancela o se reinicia el servidor. | Se informa el estado; no se aplican datos automáticamente ni se repiten confirmaciones. |
| CA-36 | Confirmo un presupuesto nuevo o una revisión. | Se conserva la revisión anterior y la principal solo cambia por elección explícita. |

## 7. Requisitos no funcionales y restricciones

- El lector opera aislado, sin red ni credenciales, con límites efectivos por trabajo y proceso; un subproceso o `-Xmx` no bastan. Debe fallar cerrado cuando no haya aislamiento.
- Procesamiento y red no bloquean JavaFX. Sesión, CSRF, permisos, paginación, captura ante errores y baja lógica siguen los contratos vigentes.
- Manejar archivos hostiles, tiempo, memoria, expansión y salida acotada; no registrar contenido sensible en logs. Los criterios de frontera se verifican por formato y en el destino real.
- Medir lectura/OCR y mapeo por campo o fila con corpus anotado, no prometer precisión universal. Pruebas técnicas no sustituyen aceptación manual.

## 8. Decisiones y verificaciones abiertas

- Confirmar aislamiento agregado independiente por worker en el despliegue, licencias/seguridad nativas y del grafo integrado, límites efectivos y recuperación ante fallos.
- Ampliar corpus para OCR degradado/rotado, páginas mixtas, tablas, contenido activo y cachés XLS; acordar umbrales y muestras reales con autorización específica por archivo.
- Validar visualmente Windows y permisos reales; acordar operación, respaldo y retención definitivos. El ERS CU-07 cita RF-28 no localizado; RF-24 es referencia provisional de exportación futura, no de extracción.

## 9. Condición de aceptación

Con datos sintéticos anotados y, solo con autorización, muestras reales: analizar PDF digital/escaneado/mixto, XLS/XLSX y DOCX; mapear y capturar; guardar/reanudar; confirmar por destino y consultar presupuesto; probar conflictos, permisos, fallos, cancelación e idempotencia. Registrar comandos, entorno, actor, resultados y evidencia en [review.md](review.md). La aceptación manual y la verificación integral siguen pendientes.
