# Spec 003 — Generación documental a partir de datos de obra

**Estado:** Propuesto  
**Fecha / actualización:** 2026-11-25  
**Issue:** pendiente  
**Plan:** [plan.md](plan.md) · **Seguimiento:** [review.md](review.md)

## Resultado esperado

Una persona autorizada puede elaborar documentos DOCX para una obra a partir de un paquete y plantillas configuradas, reutilizando los valores de referencia y conservando la procedencia y el historial de cada generación.

## 1. Contexto y fuentes

- **Problema y usuarios:** el personal interno ya puede administrar obras y configuración documental, pero no producir documentos finales desde las plantillas.
- **Fuentes consultadas:** ERS, DAS y Diccionario en `docs/informative/`; [spec 001](../001/spec.md); `docs/constitution.md`; README de ambos módulos; código documental, migraciones y plantilla de prueba `brica-server/src/test/resources/documents/acta-inicio-prototipo.docx`.
- **Estado comprobado:** existen campos, versiones, paquetes, análisis DOCX, selección y resolución de valores de obra/cliente. `DocumentSelectionService` comprueba revisión, elegibilidad e integridad; no es un motor de generación. No existen los módulos `generation` propuestos en los README ni generación/exportación final.
- **Discrepancias:** el ERS y el Diccionario contemplan paquetes por etapa, mientras la spec 001 y el código vigente no exigen etapas. El README del servidor indica V18 como última migración, pero en esta rama existen V19 y V20. Los árboles de los README son propuestas, no inventarios.

## 2. Alcance

### Incluye

- Selección para una obra de paquete aplicable y plantillas elegibles; fijación de sus versiones para cada ejecución.
- Resolución y revisión de valores de referencia, procedencia, presentación y faltantes, reutilizando las fuentes existentes.
- Generación de DOCX, conservación de historial y acceso autorizado a las salidas. Contemplar borrador recuperable, revisión previa y descarga, con detalles sujetos a las decisiones abiertas.
- Manejo de errores, permisos, concurrencia, integridad del archivo y pruebas con datos ficticios.

### No incluye

- Extracción adicional, cambios al CRUD de obras o administración general de plantillas y paquetes ya cubiertos por otras specs.
- Implementación de convenios, estimaciones, etapas, impuestos o reglas de vigencia futuras; tampoco PDF, conversión de formatos, despliegue ni integración con servicios externos.
- Cambiar por esta spec las fuentes de verdad de obra/cliente o modificar automáticamente datos de negocio al generar.

## 3. Actores, permisos y precondiciones

| Actor | Permiso necesario | Precondiciones / límites |
|---|---|---|
| Usuario interno | Autenticación y permisos de lectura, generación y descarga a definir | Obra disponible y paquete aplicable listo; el servidor verifica la autorización. |
| Administrador documental | Permisos existentes de configuración | Configura previamente las plantillas y campos; esta spec no amplía sus facultades. |

La lectura actual de valores usa `DOCUMENT_VALUES_READ`; no se presume que ese permiso autorice crear o descargar documentos.

## 4. Reglas y datos

- **Reglas:** una ejecución pertenece a una obra y registra paquete, revisión y versión exacta de cada plantilla utilizada. Las nuevas selecciones reutilizan las reglas vigentes de elegibilidad y versión; los borradores conservan su selección fijada. La corrección de información se realiza en su fuente y una nueva generación no modifica las anteriores.
- **Validaciones y estados:** identificar campos obligatorios y opcionales, presentes, faltantes o inválidos antes de confirmar. No emitir como definitiva una salida con campos obligatorios sin resolver. Los estados exactos y el tratamiento de opcionales están pendientes de decisión.
- **Entradas/salidas y procedencia:** obra, selección de paquete/plantillas, valores y su origen; salida DOCX asociada a ejecución y versión. El resolver actual obtiene atributos permitidos de obra, cliente y adicionales; no evalúa rutas arbitrarias. Registrar el contexto suficiente para explicar la salida, sujeto a la decisión de captura de datos.
- **Persistencia e historial:** los archivos físicos corresponden al almacenamiento administrado por el servidor; los metadatos y relaciones se conservarán de acuerdo con el diseño aprobado. No sobrescribir salidas históricas. Controlar cambios concurrentes y reintentos; la política específica queda pendiente.
- **Seguridad:** autorización en servidor, sesión y CSRF existentes, límites de procesamiento y validación de DOCX. No exponer rutas internas, secretos ni datos reales en pruebas o registros.

## 5. Requisitos funcionales

| ID | Requisito | Prioridad | Fuente |
|---|---|---|---|
| REQ-001 | Ofrecer paquetes y plantillas aplicables a la obra y verificar selección y revisión en servidor. | Must | Spec 001; `DocumentSelectionService` |
| REQ-002 | Fijar versión de cada plantilla al iniciar la ejecución y conservarla al reanudar. | Must | Constitución IV; DAS |
| REQ-003 | Resolver y mostrar campos, origen, estado, obligatoriedad y presentación usando información autorizada. | Must | ERS; resolver existente |
| REQ-004 | Identificar faltantes o inválidos y evitar confirmar con obligatorios pendientes. | Must | ERS |
| REQ-005 | Generar DOCX sustituyendo marcadores admitidos con valores resueltos y conservando la plantilla original. | Must | ERS; solicitud |
| REQ-006 | Guardar y retomar una elaboración parcial vinculada a la obra. | Must | ERS; DAS |
| REQ-007 | Ofrecer revisión previa antes de confirmar; representación renderizada o listado de datos sujetos a D-003. | Must | ERS |
| REQ-008 | Consultar historial y descargar salidas autorizadas sin alterar documentos anteriores. | Must | ERS; Constitución IV |
| REQ-009 | Informar fallos sin declarar éxito si el archivo o sus metadatos no se guardaron correctamente. | Must | Constitución III–IV |

## 6. Historias y criterios de aceptación

### HU-001 — Preparar una generación

Como usuario autorizado, quiero seleccionar la configuración y revisar valores para comprobar qué documento se elaborará.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-001 | Una obra con paquete listo y plantilla elegible | Inicio la selección | Veo opciones aplicables y la versión que quedará fijada. |
| CA-002 | La selección o revisión del paquete cambió | Solicito iniciar con datos obsoletos | El servidor rechaza la selección y solicita actualizar opciones. |
| CA-003 | Hay valores faltantes o inválidos | Reviso los campos | Veo marcador, origen, estado y obligatoriedad; no puedo confirmar con obligatorios pendientes. |
| CA-004 | Una plantilla tiene marcadores repetidos y campos de distintos tipos | Resuelvo los valores | Cada aparición usa el valor y presentación aprobados; el origen es trazable. |

### HU-002 — Continuar y generar

Como usuario autorizado, quiero guardar y completar la elaboración para obtener documentos consistentes.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-005 | Una elaboración parcial guardada | La retomo después de registrar una versión nueva | Recupero la selección y versión fijadas; no se sustituye silenciosamente la plantilla. |
| CA-006 | Una plantilla representativa y valores válidos de prueba | Confirmo | Obtengo un DOCX con sustituciones verificables, sin modificar el original; se registra su procedencia. |
| CA-007 | Datos de obra corregidos tras una generación | Creo otra generación | La nueva usa los datos conforme a la política aprobada y la anterior permanece accesible e inalterada. |
| CA-008 | Falla el renderizado o el almacenamiento | Confirmo | No se informa éxito ni queda una salida incompleta presentada como definitiva; se informa cómo recuperar o reintentar. |
| CA-009 | Dos sesiones o un reintento concurren sobre la misma elaboración | Guardan o confirman | Se aplica la política aprobada sin pérdida silenciosa ni duplicados inadvertidos. |

### HU-003 — Consultar resultados

Como usuario autorizado, quiero revisar y obtener documentos previos para utilizarlos sin perder su historial.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA-010 | Una generación finalizada | Consulto su historial y descargo una salida | Se identifica obra, versión y ejecución y se obtiene el archivo correspondiente. |
| CA-011 | Un usuario sin el permiso requerido | Consulta, modifica, confirma o descarga | El servidor rechaza la operación sin revelar contenido protegido. |

## 7. Requisitos no funcionales y restricciones

| ID | Requisito/restricción | Verificación |
|---|---|---|
| NFR-001 | Red y procesamiento no bloquean el hilo JavaFX. | Pruebas de cliente y recorrido visual en Windows. |
| NFR-002 | Mantener aislamiento, límites y validaciones del analizador DOCX; justificar cualquier dependencia nueva. | Pruebas de seguridad y revisión de configuración. |
| NFR-003 | Conservar historial y migraciones existentes; Flyway evoluciona el esquema si se aprueba. | Integración en base nueva y actualizada con datos ficticios. |
| NFR-004 | Verificar sustituciones, tablas, encabezados y pies admitidos, así como formato y marcadores pendientes. | Pruebas DOCX y apertura manual de salida de prueba. |
| NFR-005 | Mantener errores comprensibles y permisos aplicados por el servidor. | Pruebas API, seguridad y recorrido de errores. |

## 8. Decisiones abiertas

| ID | Pregunta / alternativas | Impacto | Responsable / estado |
|---|---|---|---|
| D-001 | ¿Qué permisos y roles autorizan crear, retomar, consultar y descargar? | Seguridad y migración de permisos. | Responsable del proyecto / Pendiente |
| D-002 | ¿Cuándo se fijan valores de obra: inicio, revisión o confirmación? ¿Cómo avisar cambios posteriores y qué instantánea conservar? | Reproducibilidad y concurrencia. | Responsable / Pendiente |
| D-003 | ¿La revisión previa requiere DOCX renderizado o basta una revisión estructurada? ¿Se admite descargar borradores? | Alcance, almacenamiento y experiencia. | Responsable / Pendiente |
| D-004 | ¿Cómo tratar opcionales vacíos, inválidos, documentos estáticos y marcadores no resueltos? | Contenido y validación de salida. | Responsable / Pendiente |
| D-005 | ¿Qué estados, reintentos, idempotencia y recuperación se requieren ante fallo parcial o concurrencia? | Persistencia y operación. | Responsable / Pendiente |
| D-006 | ¿Se descarga un archivo por vez o también varios agrupados? ¿Retención y acceso histórico tras baja lógica? | API, almacenamiento y privacidad. | Responsable / Pendiente |
| D-007 | ¿Qué captura adicional pertenece al borrador y puede sobrescribir valores de origen, si acaso? | Precedencia y fuente de verdad. | Responsable / Pendiente |

## 9. Condición de aceptación

Con datos ficticios, una obra y un paquete configurado, una persona autorizada selecciona, revisa, guarda y retoma una elaboración, confirma la generación y descarga el DOCX; corrige un dato en su origen, genera otra salida y comprueba que la anterior permanece intacta. Se verifican faltantes, permisos, concurrencia, fallos e integridad según las decisiones aprobadas.

Las pruebas automáticas deben cubrir dominio, API, seguridad, persistencia y procesador documental en entornos aislados. La aceptación manual requiere Windows para JavaFX y apertura de DOCX en un editor compatible, incluyendo cuerpo, tablas, encabezados y pies cuando la plantilla los contenga. Registrar plantilla, versión, entorno, resultados y responsable en `review.md`; todo permanece pendiente hasta contar con evidencia.
