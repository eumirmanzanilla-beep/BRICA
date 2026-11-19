# Seguimiento de implementación — Spec 001

**Inicio:** 2026-11-09. **Rama actual:** `feat/23-documents-secure-reference-catalog`. **Plan:** [plan.md](plan.md). **Spec:** [spec.md](spec.md).

**Última actualización:** 2026-11-11. La sección 14 conserva la evidencia de implementación del 2026-11-10; la sección 15 registra la sincronización documental y la 16, la organización en commits y nuevas verificaciones. Ninguna ejecución automática equivale a aceptación manual.

Este archivo es el registro de avance, no una aprobación automática del módulo. Cerrar cada tarea únicamente cuando exista evidencia, indicando límites y pendientes. No confundir reglas internas probadas con un criterio aceptado de extremo a extremo.

## 1. Alcance autorizado y estado

El responsable solicitó iniciar la implementación, mantener este registro y consultar decisiones no triviales. Las aprobaciones 1–13 son antecedentes sucesivos; la 14 y la sección 14 establecen la reanudación vigente:

1. **F2 aislada primero:** catálogo y resolución internos con persistencia y pruebas, sin endpoints ni habilitación del módulo. F1 no se considera resuelta.
2. **Solo dos atributos de texto:** `WORK_NAME` y `CLIENT_NAME`. Fechas e importes quedan pendientes.
3. **Diccionario:** modificar únicamente el límite de `VERSION_PLANTILLA_CAMPO.marcador` de 50 a 54, conservando contenido y formato restantes.
4. **Borrado lógico:** no permitir operaciones futuras con obras/clientes eliminados; conservar todos los archivos históricos generados con su información.
5. **Catálogo inicial:** alta, listado y renombrado. Nombre visible máximo 50; tipo, origen y ruta determinados por el servidor. Sin edición semántica ni borrado por ahora.
6. **Nueva prioridad:** resolver F0/F1 antes de F3. Diccionario como definición de atributos; decisiones que no resuelva se presentan con descripción, impacto, soluciones y pros/contras.
7. **A1/B1/C1/D1/E1 aprobadas:** sesión de servidor; todas las API de negocio protegidas; Spring Security/soporte de pruebas y BCrypt; administración local inicial; prototipos y campos construidos conjuntamente, no formatos de producción inventados.
8. **Parámetros confirmados después:** sin caducidad por inactividad ni límite absoluto; cookie solo en memoria del escritorio y pérdida al reiniciar servidor. Mínimo 8 caracteres de contraseña, una mayúscula y un número; BCrypt coste 10. Usuario exacto, sin normalización. Ocho permisos separados de lectura/escritura/documentales. Usuario ACTIVE/INACTIVE y rol 1/0, verificación vigente por operación.
9. **D-02/D-07 confirmadas:** paquetes asociados a un cliente, sin concepto de operación/etapa ni secuencia/momento obligatorio para generar; nombres por comparación exacta, sin recorte ni conversión automática de mayúsculas. Se mantienen permisos, elegibilidad y revisión de versiones.
10. **Preliminar mínimo D-03/D-10:** mantener solo nombres; fechas/importes siguen diferidos, conservando para su futura incorporación el formato propuesto (`dd/MM/uuuu`, dos decimales con punto sin agrupación/moneda). No inferir reglas de cálculo/redondeo.
11. **D-04 en evaluación y D-06 diferida:** estudiar Python sin aprobar aún biblioteca o cambio de arquitectura; rutas, almacenamiento interno y despliegue definitivo pasan a otra iteración. No convertir esos aplazamientos en aceptación del flujo completo.
12. **D-09 y F0-07/E1:** convenios/cambios contractuales fuera del flujo actual; no ampliar correcciones de fechas. Construir juntos una nueva plantilla sobre otros documentos por elegir. El presupuesto que posee el responsable se destina a extracción; no es una plantilla documental ni se ha identificado/analizado aquí su archivo. El acta demostrativa queda solo como fixture técnico.
13. **Actualización documental autorizada:** ajustar esta spec, su plan/seguimiento y `MEMORY.md`; preservar el historial y el trabajo local. No modificar código, fuentes DOCX, Spec 002 ni añadir dependencias en esta tarea.
14. **Reanudación funcional hasta F6:** D-03 conserva solo nombres; D-04 elige docxtpl y aprueba proceso Python local con JSON/timeout, sin servicio ni DB; D-06 autoriza prueba de almacenamiento del servidor aislada con casos construidos; D-12 reutiliza tipo documental. Captura manual en el flujo de la entidad de origen. Borrador/No disponible siguen visibles y administrables, pero no son elegibles para paquetes confirmados ni generación. Se autoriza avanzar técnicamente sin presentar como realizadas las comprobaciones manuales pendientes; no iniciar F7 ni el motor.

**Estado general:** flujo preliminar implementado hasta F6; **53 tareas: 38 cerradas técnicamente y 15 parciales, en verificación, pendientes o diferidas**. Baseline de esta reanudación: 130 servidor + 14 cliente. Verificación final de la nueva entrega: **150 servidor + 18 cliente = 168 pruebas Maven**, sin fallos/errores/omitidas; una de ellas ejecuta además 9 pruebas Python. `clean verify` empaqueta ambos módulos. Carga real de los FXML en WSL y analizador dentro de Docker correctos. F0/F1 mantienen evidencia manual pendiente; fechas/importes, plantilla operativa, almacenamiento definitivo e historial del motor no se aceptan por esta entrega.

### Resumen vigente por fase

| Fase | Cerradas técnicamente / total | Pendiente para el cierre |
|---|---|---|
| F0 | 6/7 | Elegir y revisar conjuntamente la plantilla operativa; los fixtures no la sustituyen. |
| F1 | 3/5 | Recorrido manual de acceso/consola y matriz de permisos desde el escritorio. |
| F2 | 11/13 | Fechas/corrección de fechas e importes diferidos; no ampliar el mínimo de nombres. |
| F3 | 5/5 | Bloque técnico verificado con casos construidos; revisión manual y formato operativo siguen pendientes de aceptación. |
| F4 | 6/7 | Recorrido de recuperación/reinicio; almacenamiento definitivo y fallos operativos fuera del preliminar. |
| F5 | 5/6 | Contrato de procedencia parcialmente verificado; historial de generaciones reales depende del motor. |
| F6 | 2/5 | Retorno al paquete sin pérdida, revisión visual de valores/históricos y cierre/reapertura en Windows. |
| F7 | 0/5 | Fuera del alcance autorizado; no iniciada como fase de aceptación e integración. |

**Próxima acción del responsable:** ejecutar [manual-test.md](manual-test.md), registrar observaciones por escenario y las dos evidencias separadas de CA-35. Tratar D-06 mediante [storage-test.md](storage-test.md), sin decidir por ello el despliegue definitivo. Un cierre técnico de tarea no equivale a aceptar un CA de extremo a extremo.

## 2. Fuentes y línea base

- Leídos `AGENTS.md`, constitución, comando `/feature`, memoria, Spec 001 y plan actual. Se respeta la indicación local de trabajar únicamente sobre la Spec 001.
- ERS, DAS y Diccionario ahora contienen información. La nueva solicitud sustituye la exclusión temporal del DAS/Diccionario utilizada al redactar el plan. Extraído y leído el texto de los tres DOCX; no modificados ERS/DAS.
- ERS: RF-03/05 y RNF-02/04 sustentan lectura desde fuente común; CU-03/RF-11/12/15, paquetes; CU-08/RF-13/14, versiones y análisis; RES-03, no alterar datos de obras al modificar plantillas; RF-26, roles.
- DAS: presentación → aplicación → dominio, infraestructura por puertos; autorización en aplicación; usuario registrado, contraseña hasheada y un rol con permisos; archivos físicos en servidor y metadatos/ruta relativa/hash en PostgreSQL.
- Diccionario: clave y nombre visible de campo máximo 50, tipo 30, origen 100, ruta 150. Su descripción de PAQUETE_DOCUMENTAL y atributo `operacion_etapa` son referencias heredadas incompatibles con D-02; no definir un catálogo para conservarlas. D-12 quedó resuelta por tipo documental; la identidad compuesta de versión y los metadatos ARCHIVO se implementaron en V7. El despliegue definitivo sigue pendiente. El DOCX no se modifica en esta actualización.
- Entorno del diagnóstico inicial: WSL/Linux, Java 25.0.4.1, Wrapper Maven 3.8.7, Docker Desktop disponible, PostgreSQL 18.6. El entorno de la reanudación se registra en la sección 14.
- Baseline histórico DBHub: instancia contrastada con Docker, base `brica`, usuario `brica_mcp`, solo lectura; entonces V1–V4 y tablas de negocio. En el diagnóstico posterior del login ya se observaron V6 y cuentas/roles activos, sin consultar contraseñas ni escribir mediante DBHub. No confundir aquel baseline con el estado posterior.
- Puerto de Compose observado durante el diagnóstico: 5432; su valor por defecto sigue siendo 5433. F1 inicial no editó `.env`; el arreglo independiente posterior añadió únicamente la opción local HTTP al archivo ignorado por Git. No se modificaron `dbhub.toml` ni la configuración de OpenCode.
- Baseline previo a editar código: `./mvnw test` → **BUILD SUCCESS, 43 pruebas, 0 fallos, 0 errores, 0 omitidas**; cliente sin pruebas existentes. Advertencias previas de Jansi/Mockito/agentes dinámicos, no fallos.

## 3. Tareas del plan, una por una

Estados: **cerrada** = evidencia comprobada; **verificación** = implementada, prueba final pendiente; **pendiente** = no implementada; **bloqueada** = necesita decisión/dependencia; **diferida** = fuera del preliminar por decisión expresa, no terminada. Las fases completas permanecen abiertas mientras existan tareas o criterios pendientes.

### F0 — Decisiones y baseline (parcial)

| ID | Tarea | Estado / evidencia |
|---|---|---|
| F0-01 | Revisar rama, diff, instrucciones y fuentes vigentes. | **Cerrada:** diagnóstico de sección 2; trabajo local preservado. |
| F0-02 | Comprobar Java, Maven, Docker y destino de DBHub. | **Cerrada:** versiones e identidad contrastadas. |
| F0-03 | Ejecutar baseline antes de cambios funcionales. | **Cerrada:** baseline inicial de 43 pruebas y baseline de reanudación de 130 servidor + 14 cliente, ambos exitosos; evidencia separada en secciones 2 y 14. |
| F0-04 | Resolver decisiones necesarias para el primer bloque. | **Cerrada:** cinco respuestas explícitas de sección 1. |
| F0-05 | Alinear Diccionario y spec para marcador de 54. | **Cerrada:** una celda cambiada; un byte diferente en `word/document.xml`, resto de entradas ZIP idénticas, XML y ZIP válidos. |
| F0-06 | Cerrar decisiones necesarias y registrar las diferidas según fase. | **Cerrada para el preliminar:** aprobación 14; docxtpl/proceso local, almacenamiento de prueba aislado, identidad por tipo y estados confirmados. Precisión monetaria, servidor final y plantilla operativa no se inventan ni bloquean el avance técnico autorizado. |
| F0-07 | Preparar DOCX representativos y fixtures de análisis/seguridad/estático. | **Parcial:** conservar los cuatro fixtures técnicos; no adoptar el acta como primera plantilla real. Falta elegir otro documento y construir juntos contenido/campos/obligatoriedad. Presupuesto disponible para extracción, aún no identificado/analizado en el repositorio; no es plantilla de salida. |

### F1 — Permisos y contratos base (parcial, validación manual pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F1-01 | Elegir mecanismo de autenticación, usuarios/roles y credenciales. | **Cerrada:** A1/B1/C1/D1 y parámetros confirmados; V6, contraseña/BCrypt y servicios de aprovisionamiento probados. Migración desde V4 conserva negocio y no crea cuentas por defecto. El responsable reportó crear dos cuentas por consola; su recorrido completo sigue pendiente en F1-03/sección 11. |
| F1-02 | Implementar autorización por operación en aplicación y protección HTTP. | **Cerrada para casos de uso existentes:** permisos vigentes en Clientes/Obras/Contratos/campos/valores; HTTP real verifica 401/403, revocación/desactivación, CSRF y rechazo de llamadas de aplicación no autorizadas. HTTPS exigido salvo opción explícita de desarrollo. Las operaciones futuras deberán incorporar su permiso y sus pruebas. |
| F1-03 | Acordar impacto en API existente, sesión/CSRF y autenticación del escritorio. | **Parcial:** 14 pruebas del cliente tras arreglo independiente, incluyendo cookie tardía y diagnóstico seguro de errores. Prueba cruzada con cliente HTTP real, Spring y PostgreSQL desechable correcta; no equivale a validación visual. Responsable usa WSL y reportó crear cuentas; no confirmó aún acceso correcto por interfaz. Recorrido completo de consola/JavaFX pendiente. |
| F1-04 | Definir y publicar DTO/rutas/errores autorizados. | **Cerrada para contratos implementados:** catálogo, análisis multipart, versiones, operaciones recuperables, paquetes con ETag y selección/valores. HTTP real comprueba respuestas y `ProblemDetail` de revisión desactualizada. |
| F1-05 | Verificar usuarios sin permiso, consulta, administración y ambos. | **Parcial por recorrido manual:** matriz real PostgreSQL/HTTP ampliada a plantillas, archivos, análisis, paquetes y opciones; mutaciones con CSRF y permisos independientes. Guía manual entregada; no declarar CA-17/42 aceptados integralmente. |

### F2 — Esquema, catálogo y resolución (parcial)

| ID | Tarea | Estado / evidencia |
|---|---|---|
| F2-01 | Crear V5 para catálogo, conservando V1–V4. | **Cerrada:** tabla `reference_fields`, restricciones, unicidad y revisión optimista; Flyway e Hibernate validan. |
| F2-02 | Validar migración limpia y upgrade V4 con datos de negocio. | **Cerrada:** PostgreSQL 18; comparación de filas completas de cliente/obra/contrato, segunda migración sin cambios. |
| F2-03 | Implementar clave canónica y marcador exacto 50/54. | **Cerrada:** validación exacta de dominio y ahora casos DOCX reales, incluidas claves de 50 y claves que empiezan por dígitos; sin cambiar la sintaxis para adaptarla a Jinja. |
| F2-04 | Lista aprobada de nombres; derivar tipo, origen y ruta sin evaluación libre. | **Cerrada:** dominio y DB rechazan origen/tipo/ruta ajenos a la lista. |
| F2-05 | Registrar campo y rechazar duplicados, incluidos concurrentes. | **Cerrada:** servicio, adaptador y restricción única; carrera real produce un éxito y un duplicado. |
| F2-06 | Listar con paginación y renombrar sin cambiar semántica. | **Cerrada:** validación de nombre, orden estable, conservación de definición y rechazo de entidad obsoleta mediante `@Version`. |
| F2-07 | Resolver valores de dos obras desde su cliente asociado, sin escrituras. | **Cerrada:** proyección de una consulta, recarga tras corrección y comparación de filas de negocio completas. |
| F2-08 | Distinguir presente/faltante/inválido de fallo de lectura. | **Cerrada para texto:** dominio y adaptador probados; fallo de origen se propaga como error, no faltante. Otros tipos pendientes F2-12. |
| F2-09 | Excluir obras y clientes eliminados de nuevas resoluciones. | **Cerrada:** filtros de ambos y conservación de contratos/filas probados. Archivos generados históricos se verificarán en F4/F7. |
| F2-10 | Proteger semántica de campos vinculados a versiones reales. | **Cerrada técnicamente:** asociaciones V7 reales, renombrado conserva vínculos/configuración, FK impide eliminar el campo y API rechaza edición semántica. CA-05/40 conservan recorrido manual pendiente. |
| F2-11 | Corregir fecha desde Obras manteniendo precondiciones. | **Diferida:** D-09 no autoriza ampliar fechas ni convenios. CA-04 ahora habla de un campo genérico, no exige añadir fechas. Con solo nombres obligatorios no se inventa un faltante de negocio por SQL; su aceptación completa continúa pendiente. |
| F2-12 | Resolver fechas/importes y acordar formato común. | **Diferida:** D-01/03/10 mantienen solo nombres para el preliminar y los formatos propuestos para la futura ampliación. Sin cálculo/redondeo inventados ni atributos habilitados anticipadamente. |
| F2-13 | Verificar suite completa, empaquetado y diff. | **Cerrada técnicamente:** primer bloque de 114 pruebas y verificación final de reanudación de 150 servidor + 18 cliente, con ambos módulos empaquetados; no confundir las ejecuciones históricas. Detalle en sección 14. |

### F3 — Análisis DOCX y revisión (verificada técnicamente con fixtures)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F3-01 | Evaluar/aprobar biblioteca, integración y alcance de procesamiento. | **Cerrada:** docxtpl 0.20.2 y proceso local aprobados, dependencias fijadas, timeout/salida/concurrencia acotados; intérprete ausente produce 503 seguro. |
| F3-02 | Analizar cuerpo, tablas, encabezados/pies y runs con ubicaciones. | **Cerrada con fixtures:** cuatro apariciones reconocidas; copia normalizada y renderizado efímero con docxtpl verifican runs/tabla/encabezado/pie y escape XML. Original inalterado. |
| F3-03 | Diagnosticar desconocidos, sintaxis, partes no soportadas y párrafos distintos. | **Cerrada:** diagnósticos estructurados; comentarios, texto oculto, instrucciones y cuadros de texto no se ignoran; entre párrafos se rechaza; catálogo vinculado en Java, sin Jinja libre. |
| F3-04 | Proponer obligatoriedad heredada y exigir clasificar nuevos. | **Cerrada:** integración con dos versiones reales, conservados/nuevos/retirados y obligatoriedad revisable; primera versión incompleta rechazada; configuración anterior conservada. |
| F3-05 | Confirmación explícita de estático y pruebas hostiles. | **Cerrada para prueba construida:** estático exige aceptación explícita; ZIP traversal/duplicados/límites, relaciones externas, XML con entidades y sintaxis Jinja rechazados. |

### F4 — Archivos y versiones (implementada en almacenamiento aislado de prueba)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F4-01 | Cerrar mapeo de versión, ARCHIVO y asociaciones. | **Cerrada:** V7, identidad compuesta plantilla/número, FK al archivo y al campo, marcador 54. Flyway en vacío/upgrade y Hibernate validate correctos. |
| F4-02 | Almacenamiento, metadatos, hash y comprobación de integridad. | **Cerrada solo para D-06 de prueba:** puerto y adaptador separados, raíz configurable, publicación atómica, metadatos/hash, lectura desde adaptador nuevo, traversal/symlinks rechazados e integridad comprobada. Despliegue final no aceptado. |
| F4-03 | Reanalizar y pedir nueva revisión cuando cambie archivo/catálogo. | **Cerrada:** bytes distintos con mismo nombre o renombrado del catálogo devuelven nuevo análisis sin crear versión; contrato 409 `ProblemDetail` probado por HTTP. |
| F4-04 | Estados pendiente/completado/fallido, recuperación e idempotencia. | **Cerrada técnicamente:** insumos durables antes de I/O, recuperación administrativa explícita, reenvío devuelve estado/resultado y UUID conflictivo se rechaza. Sin limpieza/retención automática no aprobada. |
| F4-05 | Numeración concurrente y última revisión válida seleccionable. | **Cerrada:** PostgreSQL real, bloqueo por plantilla al completar; dos intenciones distintas reciben números distintos y dos reenvíos comparten operación. Última completada es seleccionable; fallo no reemplaza anterior. |
| F4-06 | Inactivación, eliminación lógica e histórico inmutable. | **Cerrada técnicamente:** DRAFT/UNAVAILABLE siguen administrables y permiten versiones, fuera de nuevas selecciones; eliminada solo lectura. Archivo/versiones anteriores conservados. No existen aún archivos de generaciones de negocio. |
| F4-07 | Verificar fallos por frontera, pérdida de respuesta y reenvíos. | **Parcial:** inyección antes de escribir, después de publicar y durante metadatos; rollback y recuperación sin duplicado; corrupción excluye selección y descarga. Reinicio/recorrido manual pendientes; no simula cortes de energía ni todos los fallos del almacenamiento definitivo. |

### F5 — Paquetes y aplicabilidad (implementada; historial del motor pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F5-01 | Alta por cliente y unicidad exacta de nombres, sin operación/etapa. | **Cerrada:** restricciones V7 y pruebas de duplicado exacto, diferencias de mayúsculas/espacios; sin catálogo ni filtro de etapas. |
| F5-02 | Borrador persistente, reapertura y descarte seguro. | **Cerrada en servidor:** guardado/carga posterior y descarte sin perder versiones; captura parcial permitida, nunca aplicable para uso. Recorrido de cierre del escritorio en F6-05. |
| F5-03 | Confirmación y edición atómica con precondición de revisión. | **Cerrada técnicamente:** ETag/If-Match, edición obsoleta rechazada, cliente confirmado fijo y primera composición preservada. |
| F5-04 | Opciones aplicables por cliente, filtros y orden visual nombre/ID. | **Cerrada técnicamente:** relación real obra/cliente, solo paquetes confirmados con opciones elegibles y archivos verificables; orden visual estable, sin secuencia de uso. |
| F5-05 | Selección única por documento y resolución vigente de sus campos. | **Cerrada técnicamente:** identidad por tipo documental, una alternativa por tipo y versión compuesta vigente; valores solo de la versión, estáticos sin campos. |
| F5-06 | Verificar cambios entre selección/resolución y contrato de procedencia. | **Parcial:** versión nueva y revisión de paquete obsoleta rechazadas; respuesta identifica plantilla/versión y solicitud identifica paquete. Historial persistido de generaciones reales reservado al motor, sin generaciones ficticias. |

### F6 — Escritorio (implementado, recorrido Windows pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F6-01 | Navegación, clientes HTTP, DTO, vistas y diálogos pequeños. | **Cerrada técnicamente:** cuatro pestañas, diálogo separado y DTO explícitos. FXML raíz/documental/diálogo cargados realmente con JavaFX 25 en WSL; no equivale a Windows. |
| F6-02 | Red/archivos asíncronos y actualizaciones en hilo JavaFX. | **Cerrada técnicamente:** transporte y lectura de DOCX asíncronos, callbacks con `Platform.runLater`, controles durante petición y sin reintentos automáticos de escrituras; pruebas HTTP del cliente. |
| F6-03 | Registro de plantilla desde paquete y retorno sin pérdida. | **Verificación:** navegación entre pestañas conserva editor/selección y recarga plantillas; protección de salida con guardar/descartar/cancelar. Pendiente recorrido manual. |
| F6-04 | Valores/diagnósticos/obligatoriedad/históricos y revisión actualizada. | **Verificación:** pantallas implementadas, 409 renueva análisis sin confirmar y mantiene UUID para consultas/reenvíos. Pendiente revisión visual/funcional del responsable. |
| F6-05 | Cierre/reapertura de borradores, doble clic y validación Windows. | **Parcial:** estado durable probado en servidor, cierre interceptado para guardar cambios, contrato FXML y carga WSL correctos; guía manual Windows entregada, sin afirmar que ya fue realizada. |

### F7 — Aceptación e integración (pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F7-01 | Suite completa y verificaciones de migración/concurrencia/fallos. | **Pendiente:** por entrega; bloque inicial tiene su propia evidencia abajo. |
| F7-02 | Recorrido manual y dos verificaciones separadas de CA-35. | **Pendiente:** versión y paquete reales, no sustituir por snapshot automatizado de catálogo. |
| F7-03 | Verificar permisos reales y ausencia de acceso no autorizado. | **Pendiente:** bloquea aceptación/habilitación. |
| F7-04 | Contrato con motor: texto, estáticos, selección y procedencia histórica. | **Pendiente:** no implementar motor ni modificar Spec 002 en este bloque. |
| F7-05 | README actualizado, criterios pendientes visibles y revisión del diff. | **Pendiente:** cerrar por entrega sin presentar lógica interna como módulo disponible. |

## 4. Decisiones, discrepancias y pendientes

| ID | Decisión / estado |
|---|---|
| R-01 | Aprobaciones del responsable en sección 1 incorporadas al plan; no se interpretan como aprobación de todas las D pendientes. |
| R-02 | `ReferenceKey` centraliza validación exacta, sin convertir mayúsculas ni recortar claves. El futuro analizador reutilizará esta regla; no se presupone que ya recorra DOCX. |
| R-03 | Metadatos del campo persistidos con longitudes del Diccionario y nombres ingleses del código. Tipo/origen/ruta solo se derivan de enum cerrado. Consulta de nombres en una proyección de obra/cliente, sin cargar grafos ni aceptar otro cliente del solicitante. |
| R-04 | Unicidad en PostgreSQL, además de comprobación previa; traducir únicamente la restricción de clave duplicada, no ocultar otros fallos de integridad. Revisión optimista para renombrados concurrentes. |
| R-05 | Textos se conservan tal como están en origen; `null` y texto vacío/solo espacios son faltantes. Datos incompatibles con texto son inválidos; no convertir objetos arbitrarios con `toString`. No hay todavía tipo numérico/fecha/booleano habilitado. |
| R-06 | Conservación histórica para eliminados aprobada; aquí se prueba exclusión de nuevas lecturas y conservación de contratos/filas. No se afirma haber verificado archivos históricos generados, aún inexistentes. |
| R-07 | Diferencias previas código/Diccionario: representante legal requerido vs nullable; longitudes de nombres; ubicación/fecha inicial requeridas vs nullable; importes/atributos aún inexistentes en `Work`. No se cambia negocio existente ni se copian datos de `Contract.data`. Consultar antes de extender fuentes/validaciones. |
| R-08 | ERS CU-02 separa corrección ordinaria de cambio contractual por convenio: D-09 necesita esa distinción antes de permitir modificar fechas. |
| R-09 | DAS incluye diagramas mediante referencias/elementos gráficos. Se revisó su texto, pero no se verificó visualmente un diagrama de secuencia ni una plantilla representativa. Pendiente antes del flujo documental correspondiente. |
| R-10 | Diagnóstico inicial: modelo confirmado, autenticación sin decidir. Sustituido por A1/B1/C1/D1: mecanismo implementado y probado. Despliegue TLS operativo y recorrido manual siguen pendientes; no simular aceptación completa. |
| R-11 | Skills aplicadas: `brica-document-generation`, `brica-postgres` y `spring-data-jpa`. Context7 consultado para transacciones/proyecciones de Spring Data JPA y API de migración dirigida de Flyway; documentación JPA disponible 4.1.0, comportamiento contrastado con las dependencias efectivas del proyecto mediante integración. No se añadieron versiones ni dependencias. |
| R-12 | La fila R-11 corresponde al primer bloque. F1 añade únicamente dependencias de seguridad aprobadas, administradas por Boot 4.1.1 (Security 7.1.1). También se aplican skills REST, refactor y diseño de interfaz; se conserva la identidad visual existente para el acceso. |
| R-13 | Sesión sin límites temporales por decisión expresa. Cierre normal: intentar logout y descartar cookie; cierre abrupto: sesión remota huérfana en memoria hasta reinicio o invalidación. No hay mecanismo de recordar acceso ni credenciales persistentes. Riesgo operativo documentado, no corregido con un timeout no autorizado. |
| R-14 | Los hashes no se cifran reversiblemente. La política de creación/cambio se valida en aplicación; el encoder permite el hash de protección temporal interno que usa Spring durante la autenticación. La comparación BCrypt rechaza más de 72 bytes y nunca trunca. |
| R-15 | D-02 sustituye la lista de etapas propuesta y conserva la asociación al cliente. El Diccionario aún contiene `PAQUETE_DOCUMENTAL.operacion_etapa` y la precondición de Spec 002 todavía menciona operación: discrepancia identificada, pendiente alinear por separado. No modificar fuentes ni inventar migraciones en esta tarea. Las operaciones técnicas de registro/idempotencia o autorización no son etapas de negocio y no se eliminan. |
| R-16 | El presupuesto mencionado por el responsable es insumo de extracción, no plantilla ni prueba de generación. No inferir su formato, campos o reglas hasta recibir/identificar el archivo y acordar su análisis. Se elegirá otro documento para construir la plantilla juntos. |
| R-17 | Arreglo independiente de login: el backend HTTP exigía HTTPS; se habilitó explícitamente HTTP local en `.env` ignorado y se recreó solo el backend. El cliente distingue HTTPS/CSRF/conexión/credenciales sin mostrar secretos. Verificaciones de ese arreglo se resumen ahora con autorización para sincronizar documentación; no se atribuyen a esta tarea nuevas ejecuciones de Maven. |

## 5. Cambios de las primeras entregas (histórico)

- `brica-server/src/main/resources/db/migration/V5__create_reference_fields.sql`: solo catálogo de campos, sin modificar negocio existente ni crear anticipadamente paquetes/versiones.
- `brica-server/src/main/java/com/brica/documents/domain/`: clave, atributo aprobado, campo, metadatos tipados y resultado de resolución de texto.
- `brica-server/src/main/java/com/brica/documents/application/`: servicios internos de alta/listado/renombrado/resolución, puertos y errores específicos.
- `brica-server/src/main/java/com/brica/documents/infrastructure/`: repositorios/adaptadores JPA y lectura proyectada de nombres, con exclusión de eliminados y traducción segura de errores.
- `brica-server/src/test/java/com/brica/documents/`: pruebas de dominio, aplicación, adaptador, migración y persistencia/concurrencia.
- `docs/DiccionarioDatos_BRICA.docx`: exclusivamente longitud de marcador 54 en la celda autorizada; ZIP/XML verificados sin cambiar las demás entradas. Se usó transformación binaria mínima porque el parche de texto no edita DOCX.
- `specs/001/plan.md`: premisas actualizadas por nuevas fuentes y aprobaciones, conservando evidencia histórica del plan original.
- `specs/001/review.md`: seguimiento solicitado.
- `README.md`: distinguir catálogo interno implementado de API/interfaz aún inexistentes; reflejar V5 y enlazar seguimiento.

Trabajo local preexistente preservado: `AGENTS.md`, README, ERS/DAS, incorporación del Diccionario, specs y ajuste local del plan a trabajar únicamente con Spec 001. No se hizo commit, push ni PR.

## 6. Verificación por entrega

| Comprobación | Resultado |
|---|---|
| Baseline `./mvnw test`, antes de código nuevo. | **Correcto:** 43 pruebas; cliente sin pruebas. |
| DBHub identidad/esquema/historial Flyway. | **Correcto:** instancia Docker BRICA PostgreSQL 18.6, V1–V4. Consultas solo lectura; V5 no aplicada a desarrollo. |
| Modificación mínima del Diccionario. | **Correcto:** ZIP íntegro, XML válido, una sustitución de carácter en la celda y demás entradas descomprimidas idénticas. Apertura visual en Word pendiente. |
| `./mvnw -pl brica-server -Dtest='com.brica.documents.**.*Test' test`, PostgreSQL 18. | **Correcto:** 69 pruebas nuevas antes de añadir dos casos extra de restricciones/optimismo; ambos se verifican con la suite completa. |
| `./mvnw test` después del cambio. | **Correcto:** 114 pruebas, 0 fallos/errores/omitidas; 43 previas + 71 nuevas. Cliente sin pruebas. |
| `./mvnw clean verify`. | **Correcto:** recompilación limpia, 114 pruebas sin fallos/errores/omitidas y empaquetado de servidor y cliente. Advertencias deprecadas previas, sin fallo de build. |
| Revisión de diff, espacios y archivos nuevos. | `git diff --check` correcto; paquetes/rutas, espacios, enlaces y saltos finales de 32 archivos nuevos/actualizados comprobados. Sin controladores documentales; revisión de lógica y contratos internos sin hallazgos verificables nuevos en este alcance. |
| JavaFX en Windows, primer bloque F2. | No ejecutado: ese bloque no modificaba cliente. F1 sí cambia el acceso; recorrido visual ahora requerido y pendiente. |
| F1 `./mvnw test --no-transfer-progress`. | **Correcto:** 124 servidor + 4 cliente antes de las seis pruebas adicionales del servidor y dos regresiones del cliente. Evidencia intermedia, no cifra final. |
| F1 `./mvnw clean verify --no-transfer-progress`, primera recompilación. | **Correcto:** 130 servidor + 4 cliente, cero fallos/errores/omitidas, ambos módulos empaquetados. |
| F1 mismo comando, recompilación final tras regresión de cookies e imports. | **Correcto:** 130 servidor + 6 cliente = **136**, cero fallos/errores/omitidas; ambos módulos empaquetados. Incluye el transporte manual de cookies por generación de sesión, no una clase anterior. |
| F1 respuestas tardías de otra sesión. | **Correcto:** HTTP local concurrente con barreras, respuestas 200 y 401 con cookie obsoleta. El cliente rechaza el resultado, conserva usuario/cookie actual y no notifica cierre del nuevo acceso. |
| F1 comprobaciones estáticas. | **Correcto:** paquetes y salto final en 87 Java modificados/nuevos; XML, controladores y manejadores FXML; enlaces locales; V1–V4 byte a byte contra HEAD; `git diff --check` sin errores. No sustituye cargar FXML en JavaFX. |
| F1 verificaciones manuales y despliegue, cierre inicial. | **Evidencia histórica:** entonces no se habían creado cuentas reales ni aplicado migraciones manualmente en desarrollo. Posteriormente el responsable reportó aprovisionar cuentas y el backend local llegó a V6. Siguen pendientes el recorrido completo JavaFX/consola, revisión de plantilla y despliegue TLS definitivo. |
| Arreglo independiente posterior: `./mvnw test --no-transfer-progress`. | **Correcto:** 130 servidor + 14 cliente = **144**, sin fallos/errores/omitidas. Informes existentes contrastados en esta actualización; no son un nuevo `clean verify`. |
| Arreglo independiente: prueba cruzada sin interfaz gráfica. | **Correcto:** cliente HTTP real + backend Spring real + PostgreSQL desechable; cuenta creada con el servicio administrativo, login, cookie, logout y rechazo de contraseña incorrecta. No modifica cuentas de desarrollo ni acredita acceso visual del responsable. |
| Actualización documental autorizada. | Solo spec/plan/review de 001 y `MEMORY.md`; decisiones nuevas, criterios afectados y fuentes discrepantes señalados. Sin ejecución Maven ni cambios de código/dependencias. Validación documental al cierre de esta tarea. |

## 7. Estado de aceptación

**Ningún CA se declara aceptado completamente de extremo a extremo todavía.** Hay catálogo, análisis, asociaciones/versiones reales, paquetes e interfaz, con evidencia automatizada y carga JavaFX en WSL. CA-17/19/20/42 cuentan con matriz de permisos reales ampliada a las nuevas API; el recorrido del responsable sigue pendiente. CA-05/40 ya tienen asociaciones reales e invariantes verificadas, no solo modelos simulados.

CA-04/10 no tienen un escenario manual de dato opcional ausente con los dos nombres obligatorios actuales; no se debilita negocio ni se introducen nulos por SQL para simular aceptación. CA-25/41 en sus tipos diferidos e historial de generaciones de CA-14/21/22/23 esperan sus entregas. CA-35 exige dos comprobaciones manuales separadas sobre versiones y paquetes, aunque la preservación automática de filas ya se verifica.

Fechas/importes, plantilla conjunta y despliegue definitivo siguen pendientes. D-06 sí permite ahora la prueba de archivos del servidor aislada. Paquetes por cliente sin etapas, nombres exactos y selección por tipo están implementados. Los 42 criterios conservan numeración; la evidencia manual se recoge en [manual-test.md](manual-test.md), sin iniciar F7.

## 8. Bitácora

- **Planificación previa:** creado el plan de Spec 001, corregidas numeración/referencias cruzadas de specs y actualizado README desde la antigua Iteración 0. No se reeditó Spec 002 durante este bloque de implementación; esos cambios ya eran locales al iniciar.
- **2026-11-09 — Inicio:** inspección de fuentes y repositorio, baseline exitoso y confirmación DBHub; consultas de decisiones mediante el responsable.
- **2026-11-09 — Primer bloque:** implementación interna del catálogo/resolución de nombres y alineación autorizada del Diccionario. Verificación en curso; continuar únicamente dentro de este alcance aprobado.
- **2026-11-09 — Pruebas:** cerradas F2-01–F2-09 individualmente con evidencia; suite completa de 114 pruebas correcta. F2-13 espera el empaquetado final. No se cierra la fase F2 completa ni ningún CA de extremo a extremo.
- **2026-11-09 — Cierre del bloque:** `clean verify` correcto y F2-13 cerrada. Comprobadas V1–V4 byte a byte contra HEAD; DBHub confirma desarrollo aún en V4. Sin hallazgos nuevos verificables en revisión del bloque. 15 tareas cerradas individualmente, ninguna fase ni criterio de extremo a extremo declarado terminado indebidamente.
- **2026-11-09 — Base F1 verificada:** cerradas F1-01, F1-02 y F1-04 dentro del alcance explícito de sus filas. Conteo actualizado a 18/53. F1-03/F1-05 parciales; recorrido manual y operaciones documentales futuras no acreditados. Recompilación final: 136 pruebas, ambos artefactos y protección ante cookies tardías comprobados. Sin commit, push o PR.
- **Arreglo independiente posterior del login:** diagnóstico de HTTPS/HTTP local y mensajes seguros; suite `test` de 144 pruebas y prueba cruzada real correctas. El responsable reportó aprovisionar cuentas y ejecutar el cliente desde WSL; acceso visual de esas cuentas aún sin confirmación. No se generó documentación durante aquel arreglo.
- **Actualización documental aprobada:** decisiones de paquetes por cliente sin etapas, nombres exactos, Python en evaluación, D-06 diferida y nueva plantilla conjunta incorporadas a spec/plan/review y memoria. Se preservan evidencia previa, conteo 18/53 y pendientes; no se edita código, fuentes DOCX ni Spec 002.

## 9. Siguiente punto de decisión

Siguiente paso: ejecutar la guía Windows/Word, registrar ambos contrastes de CA-35 y escoger conjuntamente la plantilla operativa. D-04 y D-12 están resueltas; no volver a tratarlas como bloqueos. Tratar el despliegue definitivo de D-06 por separado. Conservar el límite F6 y no iniciar F7, extracción ni generación sin otra solicitud.

## 10. Entrega F1 — implementación verificada, aceptación parcial

- V6: tablas de permisos, roles, relación rol-permiso y usuarios, con los atributos del Diccionario, sin cuentas/contraseñas por defecto y sin tablas de sesiones persistentes. Ocho claves de permiso iniciales.
- Aplicación: autorización con datos vigentes antes de cada caso de uso protegido. Jerarquía no concede permisos. Se conservan precondiciones ETag/If-Match y las reglas previas de negocio.
- API: `GET /api/v1/auth/csrf`, `POST /api/v1/auth/login` (formulario), `GET /api/v1/auth/session`, `POST /api/v1/auth/logout`; solo salud/preparación/login públicos. Nuevos contratos de campos y resolución según el plan, sin endpoints de plantillas/paquetes aún.
- Administración D1: perfil `security-admin` exclusivamente sin servidor web; terminal interactiva para crear roles/usuarios, cambiar permisos/estados y restablecer contraseñas sin argumentos ni archivos de secretos.
- Cliente: transporte de sesión común en memoria, login/logout asíncronos, protección ante respuestas de sesiones anteriores y sin reintentos automáticos de escrituras. No se rediseñan los formularios de negocio.
- F0/E1: cuatro DOCX reproducibles, prototipo explicativo y guía de participación del responsable. No hay registro automático de campos o plantillas en la base.
- Primera ejecución F1 del servidor: 124 pruebas, 10 fallos y 15 errores. Hallazgos: política de alta aplicada indebidamente al hash interno de autenticación de Spring y cadena de seguridad ausente en los slices MVC. Corregidos en código/pruebas; se repiten verificaciones sin desactivar seguridad ni omitir pruebas.
- Resultado de esa entrega: `clean verify` correcto, **130 servidor + 6 cliente**. Posteriormente, el arreglo independiente del login pasó `test` con **130 + 14** y prueba cruzada real. Ninguna de esas verificaciones acredita apertura/recorrido JavaFX ni aprobación de una plantilla; pendientes de sección 11.

## 11. Recorrido manual pendiente de F1

Usar una base de prueba identificada, no datos de producción. El responsable ya reportó crear dos cuentas desde la consola y utilizar WSL para el escritorio; eso acredita un intento de aprovisionamiento, no todo el recorrido ni un acceso visual exitoso. No pedirle crear cuentas repetidas como diagnóstico. La configuración local HTTP de desarrollo se debe habilitar por separado en servidor y cliente; Java/Maven no cargan `.env` automáticamente.

1. Completar la verificación de consola con `DB_*` configuradas y perfil `security-admin` sin web. Reutilizar cuentas de prueba existentes; comprobar rechazo de contraseña débil, duplicados y permiso desconocido, y probar cambio de contraseña/estado y `exit` sin guardar secretos aquí.
2. Comprobar backend/URL y habilitación explícita de HTTP solo para desarrollo local. El responsable abre actualmente el cliente en WSL: `BRICA_ALLOW_HTTP=true ./mvnw -pl brica-client javafx:run`. La validación Windows requerida por AGENTS permanece separada y pendiente; no darla por realizada por usar WSL.
3. Revisar carga FXML, etiquetas de usuario/contraseña, foco/Enter y legibilidad de mensajes. Verificar acceso válido e inválido y que un segundo Enter no envíe otro login mientras el primero está pendiente.
4. Con cuenta de consulta, listar Clientes/Obras; intentar guardar y comprobar rechazo del servidor sin cambios. Con escritura y lectura, recorrer alta/edición y conflicto ETag de Obras. No hay aún formulario de Contratos ni pantallas documentales; no acreditarlos con esta revisión.
5. Cerrar sesión y volver a entrar con otra cuenta. Desactivar cuenta/rol o retirar permisos mientras mantiene sesión y comprobar el siguiente acceso. Ante 401, volver al login sin reenvío automático de escrituras.
6. Cerrar y reabrir el escritorio: debe solicitar login, no recordar cookie ni contraseña. Reiniciar el servidor: la sesión previa deja de servir. El intento asíncrono de logout al cerrar no garantiza invalidación remota si el proceso termina antes de enviarlo; no prometer más que lo implementado.
7. Elegir con el responsable el nuevo documento de salida, crear su plantilla y revisar juntos contenido, marcadores y obligatoriedad. El acta en `src/test/resources/documents/` se conserva solo como fixture técnico; el presupuesto para extracción no la sustituye como plantilla de salida. Apertura y revisión en Word pendientes cuando exista el formato acordado.

Registrar fecha, sistema, caso, resultado y evidencia de cada recorrido. **Estado actual: parcial por reporte del responsable; recorrido completo pendiente.** Servicios probados por integración y transporte probado sin UI no equivalen a aceptación visual ni a consola completamente ejercitada.

## 12. Decisiones de F0 — registro histórico anterior a la reanudación

Las propuestas de esta sección conservan el razonamiento histórico. D-03/D-04/D-06/D-12 y la prohibición anterior de iniciar F3 quedan sustituidas por la aprobación 14, el plan actualizado y la sección 14; no son bloqueos vigentes.

Las respuestas del responsable de la sección 1 sustituyen las propuestas anteriores indicadas abajo. Una decisión resuelta no equivale a código implementado. Se conserva en la bitácora la evidencia de entregas previas.

### D-02 — Operaciones o etapas
1. **Descripción / decisión resuelta:** eliminar completamente ese concepto; el responsable confirmó que cada paquete sigue perteneciendo a un cliente. La anterior lista `START/ESTIMATE/AGREEMENT/CLOSURE` queda descartada.
2. **Cómo afecta al programa:** los futuros paquetes, filtros, DTO y restricciones no tendrán operación/etapa. No imponer orden ni momento de generación; conservar permisos, selección elegible y versión vigente.
3. **Solución aprobada:** paquetes por cliente, opciones por obra usando su cliente asociado. CA-13/16/31/32, CD-005/006 y el plan se actualizan en esta tarea, sin crear tablas ni endpoints de paquetes.
4. **Pros y contras:** simplifica selección y permite uso libre de plantillas; ya no habrá clasificación por etapa. El orden alfabético de presentación sigue siendo visual, no una secuencia de uso. Las referencias heredadas en fuentes se alinearán por separado.

### D-03/D-10 — Formato y futuros atributos
1. **Descripción / decisión vigente:** el responsable mantiene la propuesta mínima para una versión preliminar sencilla: solo nombres. Fechas/importes siguen diferidos; los importes ya están definidos en el Diccionario, no implementados en `Work`.
2. **Cómo afecta al programa:** consulta y motor deben entregar el mismo texto, validado desde el tipo real.
3. **Solución mantenida:** texto actual sin alterar contenido; para futura ampliación, fecha `dd/MM/uuuu`, importes con dos decimales y punto, sin agrupación ni moneda. Acordar reglas de captura/cálculo/redondeo en esa entrega, no habilitar atributos ahora.
4. **Pros y contras:** reduce el alcance inmediato y conserva una conversión uniforme; no cubre CA-04/41 ni las necesidades de formatos futuros hasta implementar y comprobar esos datos.

### D-04 — Biblioteca DOCX
1. **Descripción / pendiente de análisis:** el responsable propone estudiar Python por facilidad de procesamiento. Es viable integrar otro lenguaje, pero falta comprobar si conviene; no se ha aprobado implementarlo. POI deja de ser la opción propuesta por defecto.
2. **Cómo afecta al programa:** reconstrucción de párrafos/runs, cobertura de regiones y reglas compartidas con el futuro motor; añadir Python implicaría otro runtime, protocolo y manejo de errores. No trasladar permisos ni datos de negocio fuera del backend Java por suposición.
3. **Propuesta pendiente:** comparar `python-docx` y `docxtpl` con fixtures y la nueva plantilla conjunta; medir soporte y coste de integración. Evaluar proceso local frente a servicio separado sin elegir aún. No instalar bibliotecas, fijar versiones ni cambiar arquitectura sin aprobación posterior. Alcance de evaluación en sección 4.4 del plan.
4. **Pros y contras:** Python puede facilitar desarrollo documental, pero añade despliegue y pruebas entre lenguajes. Context7/documentación consultada: `docxtpl` restringe etiquetas habituales a un mismo run, mientras CA-11 exige soportar marcadores repartidos. No dar esa compatibilidad por demostrada ni usar sustitución plana como analizador completo.

### D-06 — Almacenamiento y límites
1. **Descripción / diferida:** el responsable no dispone del servidor de destino; rutas, almacenamiento interno y despliegue definitivo se tratarán en otra iteración. El entorno local Java/Docker existente no equivale a ese destino.
2. **Cómo afecta al programa:** queda pendiente la aceptación del flujo completo de archivos/versiones, conservación histórica y recuperación; no puede darse por cubierta con almacenamiento temporal.
3. **Decisión aprobada:** no concretar ahora raíz, volumen, límites ni retención. Las cifras sugeridas anteriormente (10 MiB/100 MiB/2.000 entradas) eran propuestas no aprobadas y no rigen este preliminar. Retomar diseño y autorización en su iteración; cualquier prueba local tendrá alcance propio.
4. **Pros y contras:** evita diseñar prematuramente la infraestructura y permite concentrarse en el mínimo; aplaza las evidencias de persistencia/recuperación. No autoriza borrados ni procesamiento de archivos sin controles.

### D-07 — Nombres de paquete
1. **Descripción / resuelta:** el responsable eligió comparación exacta; el ámbito de unicidad pasa a ser el cliente, sin operación/etapa.
2. **Cómo afecta al programa:** API/DB deberán rechazar el mismo nombre exacto para un cliente, sin recortar espacios ni convertir mayúsculas automáticamente.
3. **Solución aprobada:** conservar el valor capturado, compararlo exactamente y mantener validaciones de presencia/longitud del Diccionario. Sustituye la normalización anteriormente propuesta; implementación pendiente de F5.
4. **Pros y contras:** regla simple y explícita, pero admite variantes visualmente próximas. No extender ni cambiar otras reglas de identificadores por analogía.

### D-09 — Corrección de fechas
1. **Descripción / alcance confirmado:** los cambios contractuales por convenio quedan fuera del flujo actual y se tratarán en el futuro.
2. **Cómo afecta al programa:** `general-information` sigue sin modificar fechas. Esa exclusión no autoriza por sí sola correcciones ordinarias de fechas ni cambios contractuales.
3. **Decisión vigente:** mantener el mínimo de nombres y CA-04 diferido con las fechas. Antes de ampliarlo, acordar reglas de corrección y conservar precondiciones existentes; no reabrir convenios en esta entrega.
4. **Pros y contras:** mantiene el recorte sencillo sin inventar reglas contractuales; el criterio de recarga de fechas sigue pendiente de implementación y prueba.

### D-12 — Identidad de documento y alternativas
1. **Descripción / pendiente:** eliminar etapas no define qué identidad documental agrupa las plantillas alternativas.
2. **Cómo afecta al programa:** el servidor necesita esa identidad para impedir dos selecciones del mismo documento.
3. **Soluciones propuestas:** acordar una lista de tipos/identidades documentales a partir de formatos reales antes de F5; confirmar si `tipo_documento` del Diccionario es la identidad suficiente o si el negocio exige otra agrupación. No añadir una nueva entidad sin resolverlo.
4. **Pros y contras:** reutilizar el tipo simplifica el modelo si representa exactamente el documento; si es una categoría amplia, agrupa documentos distintos indebidamente. Una identidad independiente evita esa ambigüedad, pero cambia el modelo y requiere aprobación.

### F0-07/E1 — Primer formato y obligatoriedad
1. **Descripción / dirección aprobada:** construir juntos la plantilla sobre otro documento aún por elegir; el responsable no cuenta todavía con una plantilla de salida. El acta existente no se adopta como formato operativo.
2. **Cómo afecta al programa:** se necesita acordar contenido, campos y obligatoriedad antes de verificar utilidad y presentación. Los fixtures existentes solo acreditan estructura técnica.
3. **Siguiente trabajo propuesto:** elegir el documento, redactar la plantilla y revisar cada marcador/origen con el responsable. Mantener `NOMBRE_OBRA` y `NOMBRE_CLIENTE` como ejemplos, no campos obligatorios de un formato aún inexistente. El presupuesto que el responsable tiene disponible se analizará como insumo de extracción cuando aporte su archivo/formato y autorice ese trabajo separado; no se da por recibido ni leído.
4. **Pros y contras:** el diseño conjunto permite un formato útil sin atribuir aprobación a una demostración; requiere participación del responsable y evidencia visual posterior. No inventar información de cliente ni reutilizar el presupuesto automáticamente como salida.

**D-02/D-07 resueltas; D-03/D-10 mantienen el mínimo; D-06 diferida; D-09 acotada; D-04/D-12 y la nueva plantilla siguen pendientes. D-05 está implementada con validación manual parcial; D-11 conserva alta/listado/renombrado. Esta actualización no autoriza implementar F3, una integración Python ni nuevas dependencias.**

## 13. Cierre histórico de la actualización documental

- Actualizados `spec.md`, `plan.md` y este seguimiento con el alcance aprobado; memoria resumida en `../../MEMORY.md`.
- Conservados los IDs de 53 tareas y los 42 criterios; 18 tareas siguen cerradas. Decisiones resueltas y trabajo diferido no se presentan como implementación terminada.
- Fuentes discrepantes identificadas y sin modificar: `docs/DiccionarioDatos_BRICA.docx` (`operacion_etapa`) y la precondición de `specs/002/spec.md`. ERS/DAS y guía de fixtures se mantienen como fuentes/evidencia previas, no se reescriben en esta tarea.
- Verificación de esta tarea: espacios, enlaces locales, trazabilidad, longitud de memoria y conservación del contenido de archivos ajenos. Los 144 tests corresponden a la ejecución previa, no se ejecutó Maven por editar documentación. Sin commit, push ni PR.

## Entrega separada — 2026-11-10

- Issue #22: convenciones, rama `docs/22-repo-contribution-workflow`; solo `AGENTS.md` y `CONTRIBUTING.md`.
- Issue #23: rama `feat/23-documents-secure-reference-catalog`; seis commits de fuentes, catálogo interno, seguridad/API, escritorio, fixtures e instrucciones. Ambas ramas parten de `main` actualizado; PR en borrador, sin merge.
- `./mvnw test` sobre snapshots del índice antes de cada commit de código: catálogo interno 114 servidor; seguridad 126 servidor; escritorio 126 servidor + 14 cliente; fixtures 130 servidor + 14 cliente. Sin fallos, errores ni omitidas.
- `./mvnw clean verify` final: 130 servidor + 14 cliente, sin fallos, errores ni omitidas. Integración ejecutada con PostgreSQL 18 desechable en Testcontainers.
- DOCX fuente y fixtures: ZIP/XML válidos; fuentes de requisitos/diseño, no documentos operativos. Fixtures reproducidos sin diferencias y sin datos reales. Markdown, enlaces locales, FXML/controladores y espacios comprobados.
- `.env` y `dbhub.toml` conservados localmente fuera de commits; respaldo privado con SHA-256. Solo dependencias de seguridad ya aprobadas; sin analizador Python ni generación/versiones/paquetes.
- Pendiente: recorrido visual de JavaFX/Word en Windows y aceptación completa de F0/F1/Spec 001. Se mantienen los bloqueos y discrepancias registrados anteriormente.

## 14. Reanudación aprobada hasta F6 — 2026-11-10

### Decisiones aplicadas

1. **D-03 / descripción:** mínimo de dos nombres; nuevas propuestas de fechas/importes se conservan en el plan sin implementarlas. **Impacto:** no ampliar Obras, DTO de negocio ni cálculos. **Solución:** texto desde origen y faltantes/invalidación aislados. **Pros/contras:** evita reglas monetarias inventadas, pero no permite aceptar todavía CA-04/10 con un origen opcional real ni CA-41.
2. **D-04 / descripción:** docxtpl elegido. **Impacto:** requiere entorno Python. **Solución aprobada:** proceso local, JSON, timeout, salida acotada y dos trabajadores; sin servicio ni DB. **Pros/contras:** aislamiento del backend y biblioteca existente; coste de empaquetar dos runtimes y normalizar marcadores divididos. Solo admite la sintaxis canónica, no expresiones Jinja.
3. **D-06 / descripción:** ruta del servidor para casos construidos, tratamiento independiente del despliegue. **Impacto:** archivo y base no comparten transacción. **Solución:** puerto/adaptador, archivo inmutable, operación durable y recuperación administrativa explícita; guía separada. **Pros/contras:** puede comprobarse el flujo sin elegir servidor final; no acredita respaldo, retención ni cortes de energía. Sin limpieza automática de huérfanos.
4. **D-12 / descripción:** tipo documental suficiente. **Impacto:** agrupa alternativas sin otra entidad. **Solución:** comparación exacta y una selección por tipo. **Pros/contras:** mínimo modelo; exige que el administrador use coherentemente el mismo tipo para alternativas del mismo documento.
5. **Estados y captura manual:** Vigente/Borrador/No disponible son válidos y administrables; solo Vigente es elegible para uso. Borrador/No disponible permiten agregar versiones y conservan composición/historial. Eliminada es solo lectura. La captura manual pasa por la entidad de origen y sus permisos; no hay overrides documentales.

### Archivos y evidencia

- Código en `documents/{domain,application,api,infrastructure}`, puerto/adaptador de archivos separado y proceso en `brica-server/src/main/python/`. Nueva V7; V1–V6 conservadas. No se modifican fuentes DOCX ni Spec 002.
- Cliente: DTO/transporte documental y controladores/vistas separados de campos, plantillas, diálogo de versión, paquetes y valores. Conserva la paleta y estilos administrativos existentes; no añade dependencias visuales.
- Baseline antes de editar: `./mvnw test --no-transfer-progress`, **130 servidor + 14 cliente**, todo correcto. Java 25.0.5.0.3, Python 3.14.4 y Docker 29.9.2 en este entorno.
- Preparación Python en `/tmp/opencode/brica-docx-venv`: `venv` no tenía ensurepip en WSL; se instalaron las dependencias fijadas con `uv pip` en ese entorno aislado, sin modificar paquetes del sistema.
- Pruebas de la nueva entrega con `BRICA_DOCUMENT_PYTHON` explícita: primera suite `test`, 145 servidor + 18 cliente; primer `clean verify` ampliado, 149 + 18; `clean verify` final tras añadir regresión de timeout, **150 servidor + 18 cliente = 168**, sin fallos/errores/omitidas. Nueve casos Python se ejecutan dentro de una prueba Maven; no sumar dos veces esa prueba envolvente. Logs locales de baseline/final: `/tmp/opencode/brica-f0-baseline.log` y `/tmp/opencode/brica-documents-final-verify.log`.
- `DocumentConfigurationIntegrationTest`: PostgreSQL 18/HTTP real, análisis y confirmación multipart, reanálisis, estático explícito, herencia/nuevos/retirados, conservación de versión anterior/filas de negocio, FK de campos, UUID repetido/conflictivo, carreras reales, recuperación, rollback, integridad y matriz de permisos.
- `PythonDocxAnalyzerTest`/suite Python: proceso real, parser seguro, claves 50/54 y numéricas, runs/tablas/encabezados/pies, desconocidos en Java, entre párrafos/regiones rechazados, ZIP/XML hostiles y renderizado efímero con escape. Ese renderizado es una prueba de compatibilidad, no el motor implementado.
- `ServerDocumentFileStoreTest`: lectura desde adaptador nuevo, inmutabilidad, traversal y symlinks. Las pruebas no apuntan al almacenamiento ni a las cuentas de desarrollo.
- Cliente: errores 409 con análisis actualizado sin auto-confirmación/reintento, multipart/CSRF, nombres exactos, revisión de paquete, límite previo de carga y contrato FXML. Carga real con JavaFX en WSL de raíz documental (incluye sus cuatro pantallas), diálogo de versión y vista principal correcta. No es aceptación visual Windows.
- `docker compose build brica-server` y proceso analizador dentro de la imagen correctos: cuatro apariciones, cero diagnósticos. Escritura/lectura de archivo construido y eliminación de ese mismo archivo correctas en el volumen de prueba con UID 10001. No se recreó el backend de desarrollo ni se aplicó V7 manualmente a esa base. Se añadió únicamente el volumen de desarrollo para archivos construidos.
- Guías entregadas: [manual-test.md](manual-test.md) y [storage-test.md](storage-test.md). Pendiente que el responsable registre resultado/evidencia manual y diseñe la plantilla operativa; F7 y motor fuera del alcance. Sin commit, push, PR ni merge en esta reanudación.

## 15. Sincronización del seguimiento — 2026-11-11

- Consolidado el resumen por fases, conservando las 53 tareas, los 42 criterios de la spec y el conteo de 38 cierres técnicos / 15 tareas no cerradas.
- Diferenciados el baseline inicial, el de reanudación y la suite final; actualizadas referencias que todavía presentaban D-12 y el mapeo de versiones/ARCHIVO como pendientes.
- Conservadas las decisiones y evidencias históricas. No se declara ejecutado el recorrido Windows/Word, ni aceptada la plantilla operativa, el despliegue definitivo o el historial del motor.
- Cambio limitado a este archivo; comprobados enlaces locales, conteos del ledger y espacios. Sin nuevas pruebas Maven, cambios de código, commits ni publicación.

## 16. Organización autorizada en commits — 2026-11-11

El responsable canceló la adaptación de los documentos recién subidos y autorizó guardar la implementación existente en cinco commits por alcance sobre `feat/23-documents-secure-reference-catalog`. No se amplía funcionalidad ni se crea el TXT propuesto. Cada commit usa `Refs #23`; no se cierra el issue ni se hace push, PR o merge.

| Commit | Alcance | Verificación del estado exacto preparado |
|---|---|---|
| `27044d1` · `feat(documents): add restricted DOCX analysis` | Proceso Python, dependencias fijadas, adaptador, contratos/errores compartidos y pruebas del analizador. | 134 servidor + 14 cliente; cero fallos, errores u omisiones. |
| `cc81f00` · `feat(documents): add recoverable versions and document packages` | Dominio, persistencia, servicios/API, V7 y pruebas de versiones/archivos/paquetes/selección. Se mantienen juntos por sus dependencias y esquema compartido. | 150 servidor + 14 cliente; cero fallos, errores u omisiones. |
| `c9dc595` · `chore(server): package document processing runtime` | Docker, propiedades documentales, almacenamiento de prueba y exclusiones de archivos locales. | 150 servidor + 14 cliente; Compose válido, imagen construida y pruebas de analizador/almacenamiento con UID 10001 y red deshabilitada. |
| `14b8228` · `feat(desktop): add document configuration workflows` | DTO/HTTP, controladores/FXML, navegación, protección de salida y pruebas del escritorio. | 150 servidor + 18 cliente; cero fallos, errores u omisiones. Carga real de FXML en WSL correcta. |
| Este commit · `docs(documents): record implementation status and test procedures` | Spec/plan/review, guías manual/de almacenamiento/fixtures, README y memoria. | Enlaces, 53 tareas (38 cierres técnicos), 42 criterios y memoria de hasta 50 líneas comprobados; aceptación manual sigue pendiente. |

- Antes de cada commit de código se exportó el índice a una copia temporal privada y se ejecutó `./mvnw -Dbrica.documents.python=<intérprete aislado> test --no-transfer-progress`, con `BRICA_DOCUMENT_PYTHON` explícita. Se comprobó que el árbol probado coincidiera con el árbol confirmado; no se usaron los archivos todavía no preparados para hacer pasar un estado intermedio.
- El intérprete de esta verificación es Python 3.12.16, con las mismas dependencias fijadas; no se confunde con Python 3.14.4 de la entrega anterior ni se modifican los paquetes del sistema.
- El primer intento del tercer estado, ejecutado junto al build Docker, falló por conexión rechazada al PostgreSQL desechable de Testcontainers al iniciar Flyway. La repetición secuencial pasó sin cambiar fuentes ni omitir pruebas. No se atribuye una causa raíz no demostrada ni se borra la evidencia del intento fallido.
- Verificación final del conjunto: `clean verify`, **150 servidor + 18 cliente = 168**, sin fallos/errores/omisiones y ambos módulos empaquetados. La suite Python de 9 casos permanece incorporada a Maven. Las verificaciones no utilizaron la base de desarrollo ni aplicaron V7 allí.
- Respaldo privado previo y comprobación SHA-256: los contenidos funcionales originales se conservan; solo este seguimiento y `MEMORY.md` reciben notas de organización. `main` no cambia. No se ejecuta reset, limpieza de trabajo local ni reescritura de commits anteriores.
- Fuera de los commits y conservados sin modificación: `docs/Plantilla_Prueba_BRICA.docx`, `docs/PPTO 01 041126.pdf`, `docs/PPTO 01 041126.xlsx`, `.env`, `dbhub.toml` y configuración personal de OpenCode. No se copian datos empresariales de los documentos a las guías ni a fixtures.
- Se mantienen las 38 tareas cerradas técnicamente y las 15 parciales/en verificación/pendientes/diferidas. La organización del historial no completa F0/F1 manualmente, no inicia F7 ni el motor y no autoriza publicar los nuevos commits.

## 17. Correcciones solicitadas — implementación en curso

- Alcance aprobado: ampliar atributos de Cliente/Obra y adicionales tipados, preservar precisión de importes, diálogos de campos/plantillas/paquetes/valores, selección de cliente desde obra o cliente, y nombres de plantilla por comparación exacta. Las reglas de la spec anterior que limitaban atributos a nombres requieren actualización cuando sus correspondientes cambios estén implementados; no se consideran completadas por esta nota.
- Estado de obra: En Proceso, En pausa, Terminada y Cancelada; se aprobó migrar obras existentes a En Proceso. Los campos adicionales son claves globales tipadas como texto, fecha o importe, con valores por obra y faltante cuando no existen.
- Importes: valor original preciso para servidor/motor. La presentación compara el valor absoluto y el siguiente cero en la última posición decimal; si faltan como máximo cinco unidades de esa posición, redondea hacia ese cero; en otro caso conserva el valor. El signo se conserva. Ejemplos confirmados: `1.184 → 1.184`, `1.186 → 1.19`, `-1.184 → -1.184`, `-1.186 → -1.19`. Esta regla sustituye las propuestas anteriores y puede mostrar más de dos decimales.
- Avance parcial: `AmountPresentation` y 11 pruebas unitarias; aún no está conectada a resolución ni a JavaFX. El listado ordinario de plantillas excluye eliminadas y ofrece un filtro explícito para incluirlas y consultar su historial; la comprobación de integración está implementada, pero pendiente de ejecutarse.
- Verificación de esta continuación: prueba unitaria de importes y pruebas del módulo cliente correctas. El intento de suite completa falló al inicializar Testcontainers porque Docker Desktop no estaba disponible; **no se considera aprobada la integración ni la suite completa**. No se han aplicado migraciones ni modificado los tres documentos locales. No se han implementado todavía las demás correcciones aprobadas ni actualizado su aceptación.

### Continuación con Docker disponible

- Docker Desktop volvió a estar disponible. La baseline anterior pasó con PostgreSQL desechable y Python aislado; después se incorporaron diálogos de creación, edición descriptiva y confirmación de eliminación de campos. No se habilitó la edición semántica ni la creación de atributos adicionales todavía.
- El servidor permite eliminar campos sin asociaciones y devuelve `409 REFERENCE_FIELD_IN_USE` cuando una FK de versión protege el campo. La autorización de administración sigue vigente; el rechazo y la eliminación se verifican con HTTP real y PostgreSQL. No se borran referencias históricas.
- Verificado el listado ordinario sin plantillas eliminadas y el filtro explícito para incluirlas en el historial. El historial sigue siendo de solo lectura; la validación visual en Windows permanece pendiente.
- `BRICA_DOCUMENT_PYTHON=/tmp/opencode/brica-work-python/bin/python ./mvnw -Dbrica.documents.python=/tmp/opencode/brica-work-python/bin/python test --no-transfer-progress`: **163 pruebas de servidor y 18 de cliente**, sin fallos, errores ni omisiones. El intento fallido anterior queda registrado como evidencia histórica; no se ejecutó `clean verify` en esta continuación.
- **Plan incompleto:** faltan atributos de obra/cliente, adicionales tipados, migración y conexión del formato de importes, unicidad de plantillas, diálogos de plantillas/paquetes/valores y navegación por obra/cliente. No se han aplicado migraciones, cambiado documentos reales ni declarado aceptación completa. La guía manual se actualizará al implementar los recorridos faltantes.

### Continuación: atributos y conflicto de datos existentes

- V8 propuesta (no aplicada a desarrollo): datos documentales adicionales de obra, cuatro estados, importes `NUMERIC` sin escala fija, JSONB por obra y ampliación de restricciones de campos. Se conservan V1–V7. Las obras existentes se inicializan como `IN_PROGRESS`, según aprobación.
- Catálogo ampliado con los atributos fijos de Cliente/Obra y creación de campos adicionales TEXT/DATE/AMOUNT. Se añadió consulta/actualización de datos documentales con permisos de obra y `If-Match`, validación de claves/tipos y conservación del importe preciso. La resolución distingue el importe preciso del texto presentado; fecha en español. El escritorio incorpora un diálogo preliminar de datos de obra y alta de campo adicional. Estos cambios aún no constituyen el cierre completo del plan ni se han validado visualmente en Windows.
- Plantillas: creación/edición pasan a diálogo y se explica que la finalidad documental y el formato DOCX son conceptos diferentes. V9 propone unicidad exacta global, pero **no debe aplicarse a desarrollo todavía**: la comprobación de solo lectura encontró tres registros con nombre `Plantilla Prueba`, IDs 1, 2 y 3, uno de ellos no eliminado. Requiere decisión del responsable sobre unicidad entre activas versus renombrar registros históricos. V9 se verificó solamente en bases desechables sin esos duplicados.
- DBHub se contrastó con el contenedor PostgreSQL 18.6 por base `brica` y fecha de inicio del proceso. Solo consultas de lectura; Flyway de desarrollo permanece en V7. No arrancar el backend con las nuevas migraciones contra esa base hasta resolver V9: Flyway podría aplicar V8 antes de fallar en V9.
- Pruebas: primera ejecución tras introducir V9 falló porque una prueba previa intentaba crear repetidamente el mismo nombre; se ajustó el fixture de prueba, no la regla. Repetición `./mvnw ... test`: **167 servidor + 18 cliente**, sin fallos/errores/omitidas. No se ejecutó `clean verify` ni validación visual.
- Pendiente: decisión de duplicados y comportamiento de migración; completar los diálogos/contexto de paquetes y valores, captura integral y revisión de permisos/concurrencia, actualizar guía manual/spec y ejecutar aceptación técnica final. La implementación no está terminada; no hacer commit, push ni presentar aceptación completa.

### Eliminación autorizada y permisos de selectores

- El responsable autorizó eliminar las tres plantillas duplicadas y, tras informar el efecto, retirar la referencia de la tercera del borrador `Nose`. Se respaldaron las filas afectadas de forma privada en `/tmp/opencode/brica-template-removal-c_bc5hp7/before.json`; la transacción comprobó identidad, dependencias y revisión antes de borrar. IDs 1–3 eliminados; paquete 1 conservado con su otra plantilla y revisión incrementada. La consulta posterior de solo lectura confirmó cero grupos de nombres duplicados. No se aplicaron V8/V9 a desarrollo.
- Repetición de la suite tras la eliminación: el primer intento superó el límite de 120 segundos sin finalizar; el segundo terminó con **167 pruebas de servidor + 18 de cliente**, sin fallos/errores/omitidas. No se ejecutó `clean verify` ni se realizó validación visual en Windows.
- Decisión aprobada: los selectores de obra requieren `WORKS_READ` y los de cliente `CLIENTS_READ`, además del permiso documental correspondiente. No se amplían silenciosamente los permisos documentales.
- Avance posterior: valores permite seleccionar obras paginadas mediante el endpoint existente; paquetes permite seleccionar cliente u obra y toma el cliente de los datos devueltos por el servidor. Los formularios de paquetes y valores aún no se han convertido en los diálogos solicitados ni se han validado visualmente. Si se elimina una definición adicional sin vínculos, una actualización de obra conserva su valor histórico y rechaza modificaciones de esa clave; comprobado mediante prueba HTTP/PostgreSQL.
- La suite completa pasó tras incorporar los selectores (167 servidor y 18 cliente); la prueba específica de conservación pasó después de corregir un error del fixture. Aún faltan pruebas específicas de permisos de selectores, validación de FXML e interfaz, actualización de la guía y verificación final; no se ha ejecutado `clean verify` ni aplicado V8/V9 a desarrollo.

### Roles propuestos para revisión

- Alcance aprobado: Gerencial (4) y Administrativo (3) con los ocho permisos actuales; Practicante (2) e Invitado (1) con los cuatro permisos de lectura existentes. El flujo de propuestas y aprobación queda fuera de este sprint; no se concede escritura directa a Practicante.
- V10 incorpora esos roles sin crear ni modificar cuentas. Ante un nombre de rol ya existente, falla en lugar de sobrescribir datos. No se ha aplicado a desarrollo; tampoco se han eliminado ni creado cuentas allí. La sustitución solicitada requiere autorización posterior sobre esa base. La contraseña solicitada no se ha incluido en código, migraciones ni documentación.
- En bases Testcontainers se verificaron migración, preservación ante conflicto y la suite completa (168 servidor + 18 cliente); posteriormente se añadió y ejecutó una prueba HTTP específica de acceso para los cuatro roles. La interfaz visual y los diálogos documentales pendientes aún no están aceptados.

### Continuación de interfaz y concurrencia

- La consulta de valores se abrió en un diálogo reutilizando el selector de obra y las operaciones existentes. Paquetes conserva el formulario embebido: su conversión a diálogo y la comprobación de continuidad de captura siguen pendientes. No se ha ejecutado aceptación visual JavaFX/Windows.
- El ETag de obra ahora codifica las claves y valores adicionales ordenados y con longitudes, evitando colisiones entre representaciones de mapas diferentes; se añadió una prueba que comprueba orden y ambigüedad.
- `verify` sin `clean` falló en el primer intento por conexión rechazada a un contenedor PostgreSQL de prueba; la repetición pasó con **170 pruebas de servidor y 18 de cliente**, sin fallos ni omitidas. El contrato FXML se verificó en pruebas, no se cargó visualmente: no hay `xvfb-run` disponible. `git diff --check` pasó. No se tocó la base de desarrollo ni se realizaron commits.
- Se añadió un recorrido actual preliminar a `manual-test.md`; las instrucciones históricas restantes aún necesitan reconciliación completa. Falta preparar un entorno desechable accesible para la revisión visual y completar los diálogos y la documentación de especificación.

### Cierre de las correcciones implementables del sprint

Esta sección sustituye los pendientes de implementación de los apartados anteriores; conserva su evidencia histórica, sin convertirlos en aceptación Windows/Word.

- **Paquetes:** creación/edición en diálogo desplazable, selección de obra/cliente por nombre, permisos de lectura independientes, guardado y confirmación con revisión, errores conservando captura y cierre bloqueado durante peticiones. Registrar una plantilla oculta el diálogo sin vaciar la captura; `Continuar captura` la recupera. Cerrar con cambios ofrece guardar, descartar o cancelar.
- **Derivación segura:** la API admite `workId` como alternativa a `clientId`; rechaza ambos juntos. El servicio exige administración documental y `WORKS_READ`, comprueba obra/cliente vigentes y deriva el cliente desde persistencia. Pruebas HTTP verifican derivación, permisos, borrado lógico y revisión obsoleta; el cliente HTTP prueba que envía obra sin inventar cliente.
- **Obras:** el diálogo documental se extrajo a `WorkDocumentInformationDialog`. Entradas por definición adicional y tipo, texto multilínea, fechas estrictas `dd/MM/aaaa`/ISO y fechas españolas resueltas. No necesita captura de claves libres. Conserva valores históricos sin definición accesible y mantiene la captura ante errores de validación o concurrencia. En consulta no ofrece Guardar. Clientes/Obras deshabilitan sus controles de escritura para cuentas de lectura.
- **Valores:** diálogo con selectores y estados limpios al cambiar contexto; no permite cambiar selección o cerrar durante peticiones. Presenta por separado texto de importe preciso y su presentación; no genera documentos.
- **Carga gráfica real:** WSLg/DISPLAY permite ejecutar `DocumentFxSmoke` sin instalar Xvfb. La primera carga detectó un enlace incompatible de `disableProperty` con `Tab`; se corrigió, y la repetición pasó con FXML reales, contexto de obra, captura conservada al registrar plantilla, reanudación y cierre bloqueado durante carga. Esto no es aceptación de apariencia/recorrido en Windows ni de Word.
- **Entorno utilizable:** `tools/review-environment.py` crea un PostgreSQL temporal y backend local independientes; no lee `.env`, descarta configuración de conexión heredada y publica solo loopback. `tools/ReviewAccounts.java` usa la política/encoder de BRICA y transacción JDBC, rechaza bases ajenas o con usuarios y crea solo `UsuarioGerencial`/`UsuarioAdministrativo`. No añade componentes al servidor normal ni semillas de contraseñas. No automatiza borrados.
- **Revisión levantada:** URL `http://127.0.0.1:49745`, contenedor `brica-review-07553e7950a3`, base `brica_review_07553e7950a3`, proceso backend 37131 y directorio privado `/tmp/opencode/brica-review-wzj9y1j2`. Cuentas nuevas solo allí, con la contraseña de pruebas acordada, no escrita en archivos del repositorio. La BD temporal se pierde al detener ese contenedor; no es un despliegue definitivo.
- **Recorrido HTTP ejecutado en esa base:** dos clientes/obras (IDs 1 y 2), 17 campos fijos + tres adicionales, plantilla 1 con versiones 1 y 2 y paquete confirmado 1. Se analizaron 20 campos en un DOCX construido nuevo, almacenado fuera de `docs/`; se comprobaron ambos accesos, precisión `1.1861234567890123456789`, fecha española, importe negativo presentado por separado, `MISSING`, aislamiento entre clientes, historial, campo vinculado protegido, fecha inválida y `If-Match` obsoleto. Evidencia sin credenciales: `/tmp/opencode/brica-review-wzj9y1j2/evidence.json`.
- **Protección comprobada:** DBHub y el contenedor de desarrollo coinciden por base `brica`, PostgreSQL 18.6 e inicio `2026-11-12 02:58:54.230578+00`. La lectura posterior muestra Flyway V1–V7 y tres usuarios, sin aplicar V8–V10 ni sustituir cuentas. Sin commits, push, merge ni cambios de rama.
- **Documentación:** `spec.md` y `plan.md` reflejan el alcance ampliado y separan decisiones históricas. `manual-test.md` fue reconciliado con los diálogos y sustituye los comandos de arranque sobre desarrollo por revisión aislada; README enlaza esas precauciones.
- **Fuera de aceptación automatizable:** recorrido y apariencia Windows/Word, plantilla operativa conjunta, motor/exportación/historial de generaciones, despliegue y almacenamiento definitivos, propuestas/aprobación de Practicante y cualquier aplicación a la base de desarrollo sin nueva autorización. No son funcionalidades pendientes de implementar de este sprint.
- **Verificación final (2026-11-12):** `./mvnw ... verify` pasó con **172 servidor + 20 cliente**, sin fallos, errores ni omitidas (`/tmp/opencode/brica-completed-verify.log`). Tras el ajuste final del contexto de obra al paginar, se repitió `-pl brica-client verify` con las 20 pruebas y `DocumentFxSmoke` con carga gráfica real, ambos correctos. `git diff --check` correcto. Se verificó además que la provisión local rechaza el nombre `brica` antes de abrir conexión. El backend aislado responde 200 a salud. Se añadió una prueba de V9 que demuestra rechazo de nombres exactos duplicados sin renombrar ni borrar históricos.

### Corrección del bloqueo de borrado en la revisión (2026-11-12)

- El responsable confirmó que el error aparecía después de aceptar, en el cliente de `127.0.0.1:49745`, no en desarrollo. La revisión anterior ya no estaba disponible: no existía su contenedor ni su directorio temporal. Esa revisión había sido poblada por el agente con una plantilla demostrativa de dos versiones que vinculaba los 17 atributos, por lo que la protección histórica impedía borrarlos aunque el responsable no hubiera creado versiones. No se encontró evidencia de que un campo realmente libre tuviera que bloquearse; no se flexibilizó el borrado histórico ni se eliminaron asociaciones para simular el arreglo.
- El diálogo usa ahora un botón explícito **Eliminar**, distingue la comprobación preventiva de un conflicto real y muestra éxito después del `204` y la recarga; corrige la página si se elimina su último campo. También se retiró la etiqueta obsoleta «Solo nombres de obra y cliente». El servidor identifica como `REFERENCE_FIELD_IN_USE` únicamente la FK concreta de campos de versión, no cualquier error de clave foránea, y explica que las versiones históricas cuentan aunque la plantilla no esté vigente o esté eliminada.
- Se añadió una regresión HTTP que crea y elimina los 17 atributos y los tres tipos adicionales en PostgreSQL desechable, comprobando su desaparición. Se comprueba además el rechazo de un campo vinculado a una plantilla eliminada y la respuesta vacía `204` en el cliente HTTP.
- Con aviso previo se levantó **otra base de revisión**, sin restaurar datos temporales desaparecidos, en el mismo puerto mediante `tools/review-environment.py --port 49745`. Directorio `/tmp/opencode/brica-review-rd2lsqge`, contenedor `brica-review-9fe6e982ce68`, base `brica_review_9fe6e982ce68`, backend PID 24776. Se verificaron 17 borrados reales por API sobre campos creados exclusivamente para esta comprobación y se dejaron **17 campos nuevos libres** para borrado manual, con cero versiones, vínculos o paquetes. Evidencia: `field-delete-evidence.json` dentro de ese directorio. Cuentas de revisión y contraseña acordada, sin cambios en desarrollo.
- **Verificación gráfica real:** `ReferenceFieldDeleteFxSmoke` creó un campo exclusivo de prueba y ejecutó selección → confirmación **Eliminar** → `DELETE 204` → recarga → mensaje de éxito, comprobando su ausencia posterior. Los 17 campos destinados al responsable permanecen. Registro: `/tmp/opencode/brica-field-delete-fx.log`. Hay que cerrar/reabrir el cliente y acceder de nuevo: las sesiones y datos en memoria de la revisión anterior ya no son válidos.
- **Resultado final:** `verify` pasó con **173 servidor + 21 cliente**, sin fallos, errores u omitidas (`/tmp/opencode/brica-field-delete-final-verify.log`); `git diff --check` correcto. La comprobación posterior de la nueva revisión confirma 17 campos y cero versiones/vínculos. La lectura de desarrollo conserva sus dos campos, cero versiones y tres cuentas. No se hicieron commits ni cambios de rama.

## Correcciones aprobadas y sistema persistente — 2026-11-13

### Autorización y alcance

- El responsable aprobó el nuevo plan, confirmó origen interno de campos fijos y alta de adicionales, ámbitos exclusivos por obra o cliente según el punto de creación y enlace automático del paquete con su cliente. Confirmó **DOCX, PDF y XLSX**, sin DOC/XLS antiguos ni extracción automática.
- Autorizó aplicar migraciones a `brica` y sustituir exactamente `arqAdiran`, `UserTest` y `CarlosYonson` por `UsuarioGerencial` y `UsuarioAdministrativo`, conservando negocio. Esta autorización supera la restricción histórica de no tocar esa base; no autoriza commits, push, cambio de rama ni generar documentos definitivos.

### Implementación

- V11 permite ubicación y fecha de inicio nulas sin inventar fechas ni modificar datos previos. Alta de obra pide cliente, nombre y descripción; descripción conserva su carácter opcional previo. V12 agrega ámbito de obra/cliente en paquetes y plantillas, FK coherente entre obra/cliente, unicidad de paquetes por ámbito y metadatos de adjuntos de obra. V13 alinea nombres visibles con claves y predefine los 17 campos fijos preservando identificadores, definiciones previas y vínculos.
- `WorksController` se redujo y se separaron `WorkInformationForm`, `WorkCaptureDialog`, `WorkDocumentInformationDialog` y `ContextDocumentsWindow`. Obras muestra Cliente/Nombre/Descripción y cuatro iconos con ayuda/accesibilidad; barra Nueva obra/Actualizar/Eliminar/Generar documentos (este último sin implementación). Edición completa con `If-Match`; asistente de adjuntos opcionales → información → revisión; consulta de información/adjuntos; configuración contextual.
- Desde Clientes se abre configuración documental del cliente seleccionado. La ventana crea primero el paquete borrador por nombre, luego crea/vincula plantillas dentro de él y registra versiones individuales. Mantiene consulta de versiones, edición/estado, eliminación lógica de plantilla, retiro de composición y descarte de borrador. Crear nuevas plantillas todavía sin versión se hace en un borrador, no se invalida un confirmado silenciosamente.
- El backend deriva cliente desde obra y conserva el ámbito en servidor. No permite vincular plantillas de otra obra/cliente; las opciones por obra filtran paquetes exclusivos ajenos aunque compartan cliente. Los registros antiguos sin ámbito no se reasignan automáticamente. La unicidad exacta global de nombres de plantilla anterior se conserva, no se cambió de manera implícita.
- La navegación global ahora muestra solo Catálogo de campos. El alta pide clave/tipo para adicionales; no muestra selector de origen. Nombre visible = clave. Las claves fijas tienen asignación interna y no pueden solicitar otro origen. Se conserva la compatibilidad de servicios internos para asociaciones históricas; el flujo nuevo no permite renombrado independiente.
- Adjuntos: DOCX/PDF/XLSX hasta 10 MiB, contenedor/formato comprobado, rechazo de rutas ZIP inseguras/macros y límite expandido, metadatos/hash y consulta/descarga con permisos de obra y comprobación de pertenencia/integridad. No se extraen datos. El almacenamiento tiene techo de lectura de 10 MiB separado del analizador de plantillas DOCX de 1 MiB; se probó un adjunto superior a 1 MiB para evitar que pudiera guardarse y luego no descargarse.
- `AsyncDialogSave` cierra localmente antes de la petición, muestra Guardando y solo anuncia Guardado tras respuesta correcta. Un fallo restaura la captura y muestra el error; no reintenta ni declara éxito prematuramente. El registro de versión también se oculta durante confirmación y vuelve si hay errores o revisión actualizada. La paginación visible se comprueba con consulta posterior, no solo por tamaño de página.

### Respaldo e instalación efectivamente aplicada

- Identidad contrastada entre DBHub y Compose: base `brica`, PostgreSQL 18.6, proceso previo iniciado `2026-11-13 15:20:53.490242+00`.
- Respaldo privado **antes de escribir**: `/tmp/opencode/brica-persistent-before-uca95dfr`, directorio 0700 y archivos 0600. `database.dump` SHA-256 `c6784972aac21543cbe899c8bd9c020cfd5cefd8800fb61ae8d29d883b196a60`; `documents.tar.gz` SHA-256 `ac8de5746ba444fd85929ef950a47acb50000f604aa8167938ed7f3e6175e17e`; `before.json` conserva filas de negocio y cuentas. El respaldo externo debe copiarse a un destino durable privado si se desea conservarlo fuera de esta sesión; no está versionado.
- Se construyó y actualizó el backend de Compose existente, con HTTP local explícito y **sin recrear volúmenes ni generar otra base de revisión**. Flyway aplicó V11/V12/V13 correctamente. El volumen de documentos conserva el nombre histórico `brica_document_test_files` para no perder archivos; es persistente, no tmpfs. Ambos contenedores tienen política `unless-stopped`.
- `ReplaceTestAccounts.java`, herramienta local no integrada al arranque, verificó base e instante de inicio PostgreSQL, bloqueó tablas, exigió coincidencia exacta de las tres cuentas antiguas y reemplazó solo esas cuentas en una transacción. Usó la política y encoder de BRICA. Ninguna contraseña se escribió en código, migraciones, `.env` o documentación. Las cuentas actuales son **UsuarioGerencial / Gerencial / nivel 4 / 8 permisos** y **UsuarioAdministrativo / Administrativo / nivel 3 / 8 permisos**. Los roles anteriores sin usuarios no se eliminaron sin solicitud.
- Comprobación posterior: clientes, obras y contratos idénticos a `before.json`; 18 campos (17 fijos y alias histórico preexistente), todos con nombre visible igual a clave. No se sembraron plantillas/versiones demostrativas ni datos de negocio para hacer pasar la revisión.
- **Reinicio real:** se reiniciaron PostgreSQL y backend; identidad nueva de PostgreSQL `2026-11-13 17:30:14.847651+00`. Datos, catálogo y cuentas coinciden con la captura posterior a migración. Los archivos anteriores conservan los mismos hashes. Se verificó login/logout de ambas cuentas y consultas contextuales de paquetes/plantillas/adjuntos en el backend ya reiniciado. Evidencia privada: `after-migration.json`, `access-proof.json`, `restart-proof.json` y `documents-after-restart.tar.gz` dentro del directorio de respaldo.
- Destino habitual: **http://localhost:8080**. El cliente debe cerrarse y abrirse con esa URL, no con la variable antigua de 49745. Las contraseñas son las acordadas para pruebas internas; no se afirma un despliegue público endurecido ni configuración TLS productiva.

### Verificación y límites

- Los primeros intentos detectaron un intérprete Python temporal ya inexistente y un mapeo `CHAR(64)` de Hibernate que requería `@JdbcTypeCode(CHAR)`, corregidos antes del despliegue. No se omitieron pruebas para ocultarlos.
- `verify` final pasó con **178 servidor + 21 cliente**, sin fallos, errores u omitidas: `/tmp/opencode/brica-persistent-final-verify.log`. Incluye migración desde esquema previo, aislamiento por ámbito, orden paquete/plantilla/versión, obra mínima, actualización completa atómica y `If-Match`, adjuntos/permisos/bytes/integridad y formato/ZIP inseguro.
- Se repitió `-pl brica-client verify` después de los últimos ajustes visuales. `NavigationFxSmoke` cargó JavaFX real, comprobó columnas mínimas, cuatro iconos por fila, omisión del primer paso y revisión de una obra sin fechas, cierre antes de respuesta y recuperación de captura ante fallo. Último cierre local medido: **2,173889 ms** (`/tmp/opencode/brica-navigation-final-fx.log`); caso anterior 9,287686 ms. Son mediciones concretas, no garantía de latencia de red o de todos los equipos.
- `git diff --check` correcto. Sin commits, push, merge, cambio de rama ni modificación de los tres documentos locales suministrados.
- Pendiente de aceptación humana: apariencia/recorrido en Windows y Word y revisión del responsable. Fuera de esta iteración: extracción, generación/exportación definitiva, aprobación de Practicante, plantilla operativa conjunta y despliegue público/TLS/retención operativa. El sistema local persistente **sí quedó instalado y disponible**, no depende del entorno temporal.

## Correcciones de SPEC001: nombres, calendarios y ciclo de vida — 2026-11-15

### Decisiones confirmadas y conservación de trabajo local

- Nombres de cliente/obra/paquete/plantilla/versión: letras ASCII y ñ/Ñ, números y `_`, al menos una letra y sin `_` inicial. Sin acentos ni espacios, sin normalización silenciosa. Las claves de campo y nombres originales de archivos conservan su contrato anterior.
- El responsable corrigió expresamente la solicitud inicial de fechas: **no** permitir terminación anterior al inicio. Se valida tanto contractual como real y se mantienen opcionales.
- Paquetes de cliente reutilizables en obras posteriores del mismo cliente; paquetes exclusivos de obra confirmados visibles desde el cliente, sin ampliar su aplicabilidad. Varias plantillas en borradores y confirmados; no sustituir las anteriores ni confirmar una composición inelegible durante la preparación.
- Versiones con nombre propio obligatorio y editable; eliminación lógica, conservación de referencias/archivos y uso de la anterior disponible. No renumerar ni reutilizar números. Adjuntos de obra con confirmación y eliminación lógica; generados fuera de alcance.
- Se reanudó inspeccionando los cambios parciales de la interrupción. Se conservaron las modificaciones locales y las cuatro eliminaciones de documentación existentes. Se corrigió la llave sobrante de `Work` y se completó la validación compartida. No se restauraron archivos que el usuario había eliminado ni se tocaron los documentos locales suministrados. Sin commits, push, merge ni cambio de rama.

### Código y pruebas

- `InputRules.validName` centraliza la regla del servidor, utilizada por Cliente, Obra, Paquete, Plantilla y Versión. `NameInputRules` centraliza la captura JavaFX: rechaza cambios/pegados inválidos enteros en lugar de quitar caracteres y guardar otro nombre. Permite captura parcial numérica, pero bloquea confirmarla sin letras. Las pruebas HTTP comprueban que no se eluda por API.
- `WorkDatePicker` añade los tres calendarios y convierte fechas con entrada estricta; lee también el texto no confirmado del editor. `WorkInformationForm` y `Work` rechazan ambas terminaciones anteriores al inicio. `WorkActionIcon` usa vectores de tamaño fijo para evitar cuadrados/variaciones de fuentes emoji, manteniendo ayuda, accesibilidad y permisos.
- Consultas contextuales de paquetes/plantillas incluyen reutilizables del cliente en la obra. El cliente lista también paquetes exclusivos de sus obras confirmadas. El escritorio usa el ámbito del paquete seleccionado al editar, no el de la ventana: no reasigna paquetes de cliente a obra por abrirlos desde ella.
- Crear plantilla en confirmado prepara una identidad en el ámbito correcto sin alterar la composición. Tras registrar una versión elegible, la incorpora con las anteriores y control de revisión. Si se cancela, puede retomarse mediante Agregar plantilla existente. Se recorre toda la paginación al buscar otras plantillas; no queda limitado a las primeras 50.
- V14 agrega `template_versions.name/deleted_at`, `document_registrations.version_name` y `work_attachments.deleted_at`, conservando archivos y vínculos. **V14 ya estaba aplicada en la instalación al reanudar y no se editó después.** Se verificó desde base limpia y desde V13 con una versión número 7, archivo, vínculo y operación completada: conserva números/hash/configuración y rellena `Version_7`.
- Registrar versiones incorpora el nombre al hash idempotente y al estado pendiente recuperable. La selección usa el máximo no eliminado; la nueva numeración usa el máximo histórico de todas las versiones. Renombrar no cambia bytes, hash ni campos; eliminar conserva consulta histórica y protección del catálogo. Una versión eliminada no se puede renombrar.
- Adjuntos: DELETE con `WORKS_WRITE` y pertenencia a obra, ocultación de lista/descarga ordinarias y bytes conservados. La acción existe en asistente y edición, no en consulta de solo lectura; requiere confirmación.
- Se actualizaron los nombres de los datos construidos en pruebas que antes usaban espacios/acentos. No se relajaron reglas ni se omitieron pruebas para mantener casos anteriores inválidos. Se recreó el intérprete temporal de pruebas que ya no existía, sin añadir dependencias al proyecto.
- `verify` pasó con **183 servidor + 22 cliente**, sin fallos, errores u omitidas: `/tmp/opencode/brica-spec001-corrections-final-verify.log`. Se repitió `-pl brica-client verify` tras los últimos ajustes del cliente: `/tmp/opencode/brica-spec001-client-final-verify.log`.
- Comprobación adicional final de ciclo de vida y migración: **27 pruebas aprobadas**, incluidas eliminación de todas las versiones, ausencia de opciones sin versión disponible, protección de campos históricos y numeración posterior sin reutilizar números (`/tmp/opencode/brica-spec001-lifecycle-final.log`). `git diff --check` correcto.
- `NavigationFxSmoke` real comprobó iconos vectoriales, pegado inválido/ñ, lectura del calendario y rechazo de una fecha inválida, asistente sin fechas y recuperación de captura tras fallo. Se corrigió la sincronización inicial del editor del DatePicker detectada por esa comprobación. Último resultado: **2,394939 ms** de cierre local (`/tmp/opencode/brica-spec001-final-fx.log`); no garantiza la latencia de red ni otros equipos.

### Limpieza puntual y actualización del sistema persistente

- Destino comprobado mediante DBHub y Compose: `brica`, PostgreSQL 18.6, inicio `2026-11-15 22:26:36.666679+00`. El puerto observado del host es 5432; no se cambió su configuración.
- Se presentaron IDs y dependencias al responsable. Eligió **limpieza lógica**: clientes 1/2, obras 1/2 y plantilla 5; la plantilla 6 ya estaba eliminada lógicamente y conservó sus dos versiones y su vínculo histórico. Se retiraron físicamente solo los paquetes incompatibles 5/6/8/10/11 y sus dos vínculos de composición. No se purgaron archivos ni generaciones, ni se borraron paquetes con nombre válido. Los paquetes válidos de clientes eliminados quedan conservados, como se avisó antes de aprobar.
- Respaldo previo privado: `/tmp/opencode/brica-spec001-corrections-before-75g7xcy6`, directorio 0700, archivos 0600. `database.dump` SHA-256 `080c0b00def67c8986bd5cb12fc21b7d75d71da7f3a932bb69e95ab2353f88d2`; `documents.tar.gz` SHA-256 `cffb5b67d4aa1fe74f4310ed58df6d8d7303956b357a41f903e5d0b4d43f51bd`. Copiar a almacenamiento privado durable para retenerlo fuera de la sesión.
- Limpieza transaccional con bloqueo y comparación exacta del snapshot antes de modificar. Un primer intento asumió erróneamente una columna `works.revision`: abortó y se revirtió completo. Se corrigió sin editar esquema y la segunda transacción se confirmó. Registros privados: `before.json`, `cleanup.log`, `cleanup-completed.log`, `after-cleanup.json`.
- Comprobación posterior: cuentas y **19 campos preexistentes** idénticos al respaldo. Clientes/obras conservan todos sus datos salvo marca de borrado. Los cuatro adjuntos permanecen y todos los archivos conservan los hashes. No se crearon clientes, obras ni plantillas de demostración en la instalación persistente.
- Se construyó y actualizó el backend de Compose, sin recrear volúmenes ni cambiar la base. Salud disponible en **http://localhost:8080**. Ambas cuentas habituales iniciaron/cerraron sesión con sus ocho permisos; consultas muestran la limpieza y el contrato nuevo de nombres/estado de versiones. Solicitudes con nombres inválidos se rechazaron sin crear datos. Evidencia: `deployment-proof.json` en el respaldo privado.

### Pendientes de aceptación

- Recorrido humano en Windows/Word, especialmente apariencia del calendario y flujo de incorporación a confirmados, según `manual-test.md` CA-43–CA-48. Las pruebas automatizadas no sustituyen esa aceptación.
- Extracción, generación/exportación definitiva, flujo de aprobación del Practicante y despliegue público/TLS siguen fuera de esta iteración. No hay consideraciones de negocio pendientes para estas correcciones aprobadas.
