# SPEC002 — Seguimiento de implementación y trazabilidad

**Inicio:** 2026-11-17.

**Objetivo:** registrar por bloque F lo autorizado, creado, modificado, probado y pendiente, con referencias al [plan](plan.md), las [decisiones P01–P33](pendients.md) y la [spec](spec.md). No confundir propuesta, implementación y aceptación.

## Estado por bloque

| Bloque | Estado | Evidencia / condición para continuar |
|---|---|---|
| F0 — alcance y ficha técnica | **En curso; sin integrar** | Host: 10 comprobaciones. Contenedor experimental: 13 comprobaciones y 28 aserciones lector/OCR correctas; perfil acotado viable. Falta delegar límites por worker y completar evaluación ampliada. |
| F1 — contratos/modelo/migraciones | Pendiente | Depende de ficha F0 aprobada; no hay migraciones nuevas de SPEC002. |
| F2 — denominaciones/concurrencia | Pendiente | Depende de F1 y de ratificar las claves/reglas documentales. |
| F3 — fuentes/trabajos/aislamiento | Pendiente | Solo procesos experimentales F0; no hay cola/worker de aplicación ni aislamiento verificado del destino. |
| F4 — PDF/OCR | Pendiente | OCR y PDF mínimo probados en F0; no hay adaptador de aplicación ni segmentación/mezcla automática. |
| F5 — Excel/DOCX fuente | Pendiente | Lectura mínima y cachés probadas en F0; no hay adaptadores integrados ni corpus completo. |
| F6 — mapeo/revisión | Pendiente | Política v1 documentada en ficha; sin mapeo o borrador persistente implementados. |
| F7 — confirmación obra/cliente | Pendiente | Justificaciones/campos contractuales y concurrencia por definir. |
| F8 — presupuestos | Pendiente | Política de control decimal seleccionada; sin modelo, confirmaciones ni revisiones persistidas. |
| F9 — API/JavaFX | Pendiente | No existe interfaz de extracción. |
| F10 — pruebas y aceptación | Pendiente | Requiere corpus, umbrales y recorrido manual aprobados. |
| F11 — operación/despliegue | Pendiente | Requiere autorización específica de destino, respaldo y verificación. |

## F0 — 2026-11-17: inicio y alineación

### Alcance y decisiones

- Solicitud: implementar por bloques F, consultar decisiones restantes y registrar rutas, archivos, decisiones y evidencias en `Review.md`.
- F0 toma como aprobadas las elecciones P01–P33 de `plan.md` §2; no presupone aprobados productos/versiones, límites concretos, fuentes de precedencia ni acceso a archivos de negocio.
- P01-B: revisión y confirmación selectiva; P02-C: PDF/XLS/XLSX y DOCX fuente; P32-A: datos confirmados consultables, **sin generación ni exportación**. P33: pruebas anotadas y aceptación manual.
- La spec histórica mencionaba operación/etapa y generación. Se incorpora una nota de prevalencia, se corrige el resultado esperado/precondición y se redefine la condición de entrega sin borrar HU/CA históricos.
- El responsable autorizó crear y analizar **ejemplos sintéticos**, sin analizar documentos reales hasta autorización por caso. Autorizó evaluar PDFBox, Apache POI y Tesseract mediante documentación y prueba aislada; no aprobó incorporarlos como dependencias definitivas. La precedencia contrato/presupuesto se decidirá mediante una matriz por campo, que se presentará antes de aprobarla.
- Precedencia parcialmente aprobada: recomendar el **contrato** para número contractual, contratista, fecha de inicio y terminación contractual; recomendar la **hoja Excel de presupuesto seleccionada** para partidas, conceptos, unidades, cantidades y precios frente al PDF. Se conservan alternativas y confirmación humana. Monto contratado, monto antes de IVA e IVA quedan pendientes de elección individual, al igual que las demás filas de `precedencia-f0.md`.
- El responsable confirmó posteriormente **contrato** para los tres importes: contratado, antes de IVA e IVA. La prioridad es recomendación, no confirmación automática. Autorizó al asistente a acordar las demás decisiones no triviales; esa indicación no modifica la restricción de consultar antes de añadir dependencias o cambiar arquitectura, ni autoriza borrar datos.
- Posteriormente autorizó evaluar y, **solo después de comprobar versiones, licencias y pruebas aisladas**, incorporar Apache PDFBox/POI y Tesseract local. La autorización está condicionada: todavía no se han añadido al proyecto ni se han completado todas esas comprobaciones.
- Bajo esa delegación se eligió la opción conservadora para los campos sin precedencia indicada: **no recomendar automáticamente** nombres, representante, ubicación, descripción, terminación real, estado, totales de hojas ni adicionales. La tabla registra estas decisiones y conserva pendiente la desambiguación técnica de etiquetas. Si la fuente preferida contiene candidatos contradictorios no se selecciona ninguno automáticamente.

### Línea base observada

- Rama: `feat/23-documents-secure-reference-catalog`, con modificaciones sin confirmar de SPEC001 y archivos locales. Son trabajo preexistente; no se sobrescriben ni se descartan.
- Ya existen `specs/002/context.md` vacío, `specs/002/spec.md`, `specs/002/pendients.md` y `specs/002/plan.md`; `pendients.md`/`plan.md` estaban sin seguimiento en Git.
- `docs/` contiene muestras locales, entre ellas `PPTO 01 041126.pdf`, `PPTO 01 041126.xlsx`, plantillas de prueba y `contrato_danado.pdf`. Su presencia no autoriza analizar contenido, publicarlo ni tomarlos como corpus completo. Faltan muestras identificadas inequívocamente para XLS, PDF escaneado/mixto y documento DOCX fuente representativo; deben solicitarse o confirmarse.
- El código actual de SPEC001 incluye migración V14 local, catálogo tipado y adjuntos, pero no un extractor PDF/Excel/OCR. El análisis DOCX existente identifica marcadores de plantilla; no extrae valores de documentos fuente.
- `MEMORY.md` conserva notas históricas anteriores a V14; no se han reescrito ni convertido en afirmaciones del estado de SPEC002.

### Directorios y archivos de este avance

| Acción | Ruta | Trazabilidad |
|---|---|---|
| Directorios creados | Ninguno. | Se reutiliza `specs/002/`. |
| Archivo creado | `specs/002/Review.md` | Bitácora solicitada; plan F0 y seguimiento F0–F11. |
| Archivo creado | `specs/002/corpus-f0.md` | Diez casos sintéticos propuestos y anotaciones iniciales; P03-B/P33. No son archivos de prueba generados. |
| Archivo creado | `specs/002/precedencia-f0.md` | Matriz de preguntas para acordar precedencia campo a campo; P06-B. No contiene reglas aprobadas. |
| Archivo modificado | `specs/002/spec.md` | Alcance vigente y condición de entrega; P01-B, P02-C, P32-A, P33. |
| Código/migraciones/configuración | Ninguno. | No se han aprobado aún bibliotecas ni la ficha técnica de F0. |

## F0 — Alineación de propuestas DP-01–DP-38

- El responsable aportó propuestas que conservan los formatos aprobados PDF/XLS/XLSX/DOCX, OCR local y la combinación de lectura genérica, perfiles opcionales y revisión humana. Se contrastaron sin considerar aprobados automáticamente los detalles que el propio texto deja pendientes.
- Se mantiene P12-B: agrupación explícita de hojas/partes/resúmenes/alternativas. Se mantiene P14-B: revisiones presupuestales inmutables y principal seleccionada explícitamente. DP-33–DP-36 se registran para generación posterior, fuera de SPEC002.
- DP-31 propone tres estados visibles de borrador. Deben coexistir con estados técnicos de ejecución y resultados independientes de bloque; sus transiciones y semántica de confirmación parcial quedan pendientes, no se ocultan bajo una etiqueta de éxito.
- No se autorizó inspeccionar documentos reales ni crear fixtures a partir de contenido real. No se han identificado inequívocamente las rutas de las muestras AR-SO y catálogo sin precios; debe preguntarse antes de leerlas. La prueba aislada anterior usó únicamente datos sintéticos.
- Se conservaron los cambios preexistentes. Al revisar el estado se observó `specs/002/context.md` eliminado localmente y `docs/templates/` sin seguimiento; no se restauraron ni alteraron. El archivo `specs/002/precedencia-f0.md` ya no estaba presente al inicio de esta reanudación; no se recreó sin investigar.

| Acción | Ruta | Trazabilidad |
|---|---|---|
| Archivo creado | `specs/002/dp-alineacion.md` | Correspondencia DP-01–DP-38 con P/F, pendientes y límites de autorización. |
| Archivo modificado | `specs/002/plan.md` | Alineación de perfiles opcionales, presupuestos sin precio, borradores/confirmaciones separadas y condiciones de DP-27. |
| Archivo modificado | `specs/002/Review.md` | Registro de esta reanudación y del estado real de la rama. |
| Código, dependencias, migraciones o datos | Ninguno. | F0 aún no reúne sus criterios de salida; no se ha iniciado F1. |

### Comprobaciones y límites

- Revisados rama/estado, `AGENTS.md`, plan de SPEC002 y estado de `specs/002/`. No se han ejecutado pruebas de software ni una prueba de concepto de lectores en este avance documental.
- En el entorno se observó OpenJDK 25.0.5; el ejecutable `tesseract` no está disponible en PATH. Esto no demuestra que no esté disponible en otros hosts o imágenes.
- Se consultó documentación de Apache POI, PDFBox 3 y Tesseract vía Context7: POI permite consultar resultados de fórmula en caché sin evaluador, PDFBox documenta posiciones/renderizado y riesgos de memoria, y Tesseract documenta salida TSV/idiomas. Son capacidades documentadas, no prueba de funcionamiento, selección de versión ni aprobación de dependencias. El repositorio local Maven inspeccionado no contiene JARs POI/PDFBox.
- Se consultaron metadatos de Maven Central: PDFBox **3.0.8** y POI OOXML **6.0.0** son candidatos de evaluación, no versiones definitivas aprobadas. Se descargaron en la caché Maven para una prueba fuera del repositorio; no se modificaron POM ni imagen de BRICA.
- Prototipo aislado `/tmp/opencode/brica-spec002-f0-poc/Probe.java` con `pom.xml` propio: se construyeron archivos **sintéticos** XLS y XLSX con `0012` textual y fórmula `B1*C1`, DOCX fuente sin marcadores con párrafo y tabla, y PDF digital simple. POI leyó `0012` y el valor de caché `10.5` en ambos formatos; leyó párrafo/tabla DOCX. PDFBox leyó el texto del PDF. La fórmula se **evaluó únicamente al fabricar la caché del fixture**; la lectura de la prueba usa el valor almacenado, no evalúa la fórmula. Esto no implica autorizar evaluación de fórmulas en la aplicación.
- La medición de ese proceso conjunto fue 1,846 s según el programa y pico RSS 277360 KiB según `/usr/bin/time -v`; incluye ejecución Java desde fuente/JVM y los cuatro casos juntos, no representa capacidad, rendimiento bajo carga ni un límite seguro. Log privado: `/tmp/opencode/brica-spec002-f0-poc/run.log`. Apareció un aviso Log4j por falta de proveedor en el prototipo; falta comprobar integración de logging con Spring Boot. No se leyó ningún archivo de negocio ni se accedió a PostgreSQL.
- **Aún sin comprobar:** OCR/Tesseract (no instalado en PATH), PDF mixto y geometría/tablas, caché ausente/desactualizada, muestras grandes/hostiles, aislamiento sin red/credenciales, licencias y vulnerabilidades de todas las dependencias transitivas. La prueba mínima no cumple la salida completa de F0 y no justifica todavía incorporar bibliotecas al proyecto.
- No se han modificado bases, volúmenes, documentos locales, dependencias ni infraestructura; sin commits, push o cambio de rama.
- Pendiente: decisiones concretas de `plan.md` §5, corpus autorizado y evaluación de lectores/aislamiento. No se declara F0 completada.

## F0 — 2026-11-17: banco ejecutable autorizado y decisiones v1

### Autorización y alcance real

- El responsable confirmó **OK** al bloque propuesto: pruebas sintéticas reproducibles, aislamiento/seguridad, decisiones técnicas y esta bitácora; avanzar a F1 únicamente si F0 cumple su salida. Se revisaron rama/estado nuevamente, conservando todos los cambios preexistentes de SPEC001 y las eliminaciones locales.
- Se implementó código **de evaluación F0**, no código funcional de importación del servidor. No se añadieron módulos Maven, dependencias a BRICA, migraciones, endpoints, UI ni generación documental. No se leyeron fuentes reales, `.env`, cuentas o tablas de PostgreSQL.
- Se consultó documentación actual POI/PDFBox/Tesseract mediante Context7 y se verificó comportamiento con las versiones candidatas, sin equiparar documentación a prueba. `TemplateAnalysisService` permanece intacto y separado de DOCX fuente.

### Directorios/archivos creados o modificados

| Acción | Ruta | Resultado / P y DP |
|---|---|---|
| Directorio creado | `specs/002/experiments/` | Experimentos independientes, sin incorporarlos como módulo Maven o fuente de aplicación. P09/P29/P33. |
| Archivo creado | `specs/002/experiments/F0Probe.java` | Generación sintética y lectura PDF/OCR/XLS/XLSX/DOCX, posiciones, cachés y rechazos. P02/P07–P11/P15/P18; DP-04/06/17/22. |
| Archivo creado | `specs/002/experiments/evaluate.py` | Launcher monohilo Bubblewrap, entorno saneado, montajes de lectura, límites por proceso, corte de salida/timeout y manifiesto de integridad. P29/P33; DP-05. |
| Archivo creado | `specs/002/experiments/expected.json` | Anotaciones sintéticas independientes del resultado del lector, incluyendo ausencia/cero/caché obsoleta y precio ausente. P15/P33. |
| Archivo creado | `specs/002/experiments/audit_dependencies.py` | Inventario de 17 artefactos Maven, hashes, licencias directas/avisos y consulta OSV con fallo explícito si falla la consulta. P09/P29. |
| Archivo creado | `specs/002/experiments/policy_examples_test.py` | Tres ejemplos decimales ejecutables para criterios F0; no es implementación Java del cálculo de negocio. P18; DP-23/24/27. |
| Archivo creado | `specs/002/experiments/README.md` | Preparación aislada, versiones, comandos, evidencias y límites de lo demostrado. |
| Archivo creado | `specs/002/ficha-tecnica-f0.md` | Decisiones v1 de límites, precisión/justificación, precedencia, perfiles, estados, correcciones y retención; diferencia políticas elegidas de controles medidos. |
| Archivos modificados | `specs/002/plan.md`, `corpus-f0.md`, `dp-alineacion.md` | Estado de ejecución real, cobertura parcial, remisión a ficha v1 y bloqueo antes de integrar. |
| Archivo modificado | `specs/002/Review.md` | Toda la trazabilidad de este bloque, incluidos fallos intermedios y pendientes. Se conserva este nombre con mayúscula, sin crear otro `review.md`. |

### Ejecución y resultados verificables

1. **Preparación OCR externa:** se descargaron/desempaquetaron paquetes en `/tmp/opencode/brica-spec002-f0-ocr/`, sin `sudo`, sin `apt install` ni cambios de paquetes del host. Tesseract 5.5.0 + Leptonica 1.86.0 y modelos `spa+eng` 4.1.0. Dependencias iniciales faltantes se identificaron con `ldd` y se desempaquetaron en la misma raíz. Versiones completas en `experiments/README.md`; no mezclar binarios de `resolute` con la imagen de producción `noble`.
2. **Banco final:** `python3 specs/002/experiments/evaluate.py --classpath /tmp/opencode/brica-spec002-f0-poc/classpath --ocr-root /tmp/opencode/brica-spec002-f0-ocr/root --work /tmp/opencode/brica-spec002-f0-run-06` → **10/10 comprobaciones correctas**, con **28 aserciones lector/OCR** dentro de la última comprobación. Las ocho pruebas iniciales cubren archivos del host ocultos, entorno saneado, entrada solo lectura, red del host inaccesible/namespace distinto, espacio virtual, tamaño de archivo, salida excesiva y timeout con hijo; las otras dos son versión OCR y lectores/OCR. Códigos `-9` de timeout/salida son resultados esperados, no procesos exitosos de lectura.
3. **Lectores:** PDF digital de dos páginas con posiciones; PDF escaneado y mixto reconocen exactamente `PRUEBA_01`, `16/07/2025`, `0012`, `10.50` (4/4 tokens por página, 8/8 entre ambas) y cajas no vacías. Se preserva el encabezado digital como evidencia separada. Cifrado/daño distinguidos; 41 páginas y página sobredimensionada rechazadas antes de rasterizar. No existe todavía selección automática de regiones ni desduplicación.
4. **Excel/DOCX:** XLSX mantiene código `0012`, hojas/filas ocultas, fusión y cachés `10.50`, ausente, `99.00` y cero. Las cachés se fabrican directamente en `<v>`; **no se invoca evaluador de fórmulas ni al fabricar ni al leer estos fixtures**. XLS binario mantiene `0007` y formato `12.500`; DOCX lee párrafo/tabla sin marcadores, descripción y precio vacío. POI rechaza entrada ZIP de 2 MiB muy compresible. Esto no prueba aún todos los límites acumulados o contenedores activos.
5. **Integridad:** 11 archivos sintéticos; sus SHA-256 antes/después coinciden. `report.json` registra `sources_unchanged=true`, `all_passed=true`. No se conservaron binarios de negocio ni fixtures reales en Git.
6. **Tiempo/memoria de evaluación final:** lectores/OCR juntos **2,248 s** de pared; timeout esperado **1,002 s**. Pico acumulado de hijos observado **193756 KiB**, incluye javac/generador y no es pico agregado de JVM/OCR ni garantía de capacidad. CPU de descendientes dentro del PID namespace tampoco queda completamente atribuida al launcher.
7. **Auditoría Maven final:** `python3 specs/002/experiments/audit_dependencies.py --classpath /tmp/opencode/brica-spec002-f0-poc/classpath --output /tmp/opencode/brica-spec002-f0-audit-02.json` → **17 coordenadas válidas**, respuesta OSV 2026-11-17T19:57:58.941779Z sin coincidencias informadas. Son PDFBox/pdfbox-io/fontbox 3.0.8; POI/poi-ooxml/poi-ooxml-lite 6.0.0; commons-logging 1.4.0, commons-codec 1.23.0, commons-math3 3.6.1, SparseBitSet 1.3, XMLBeans 5.4.1, commons-compress 1.28.0, commons-lang3 3.18.0, commons-io 2.22.0, CurvesAPI 1.08, Log4j API 2.26.1, commons-collections4 4.6.0. No es auditoría del BOM Spring Boot ni garantía de ausencia de vulnerabilidades. Se verificaron los avisos Apache-2.0 principales y los copyrights Tesseract/modelos/Leptonica; revisión completa de componentes nativos/redistribución pendiente.
8. **Política decimal:** `python3 -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v` → **3/3 pruebas correctas**. Caso diferencial de redondeo por concepto `1.02` frente a redondeo final `1.01`, signos/HALF_UP y conservación de escala original. No afirmar que el servidor ya implementa esa política.

### Incidencias intermedias corregidas (sin ocultar intentos fallidos)

- `run-01`: el generador llamó `unsetV()` sobre un nodo ya ausente; se corrigió comprobando `isSetV()`. No fue un fallo de importación en la aplicación, sino del generador sintético.
- `run-02`: `/usr/bin/java` era enlace a `/etc/alternatives`, no montado; el launcher usa ahora la ruta real del JDK, sin montar `/etc` entero.
- `run-03`: faltaba `java.security` (enlace de configuración Debian/Ubuntu); se montó solo ese archivo de configuración en lectura, no credenciales ni la configuración completa del host.
- `run-04`: POI intentó escribir el XLSX al cerrar el modo predeterminado. El montaje de solo lectura impidió la escritura. Se corrigió usando `PackageAccess.READ` y fábrica XLS con `readOnly=true`; no se rebajó la protección OS.
- El getter numérico devolvió `0.0` para fórmula XLSX sin `<v>`. Se comprobó presencia estructural separadamente para distinguir ausencia de cero. No extrapolar esa técnica a XLS sin evaluación adicional.
- La primera auditoría no interpretaba el POM sin namespace de XMLBeans, produciendo coordenada inválida; se corrigió normalización de namespace/validación obligatoria y se repitió sobre las 17 coordenadas válidas. El archivo anterior no es evidencia válida de XMLBeans.
- `run-05` y `run-06` finalizaron correctamente; `run-06` incluye además diagnóstico de salida truncado a 16 KiB por canal y comprobación explícita de namespace de red distinto del host. Sigue visible el aviso Log4j del prototipo sin proveedor: no se agregó una dependencia para ocultarlo.

### Decisiones nuevas adoptadas y límites

- **DP-24:** `CONTROL_V1`, cálculo exacto cantidad × precio y HALF_UP por concepto a 2 decimales, suma de importes de control; valor declarado y suma exacta conservados separadamente. No establece precisión oficial ni supone moneda/IVA.
- **DP-27:** diferencias no nulas visibles y aceptables con justificación explícita de 10–2000 caracteres; números no interpretables, relaciones incoherentes, falta de fuente, permisos o revisión vigente bloquean. No evaluar fórmulas para solucionar discrepancias.
- **DP-31/37:** borrador/confirmado/cancelado separados de estado técnico e indicadores/resultados por bloque; cancelar nunca revierte confirmados. Retención sin purga automática de fuentes/borradores/auditoría; scratch privado solo se limpia bajo raíz administrada y lease comprobado. Ninguna recuperación repite confirmaciones.
- **DP-07/09:** perfiles declarativos de obra, versionados, opcionales y sin datos reales/código; mínimo de mapeo en ficha, sin prometer un visor PDF todavía no construido.
- Se concretaron nombres/claves documentales nuevos, límites y política de corrección contractual en la ficha, conservando SPEC001. Son contratos de diseño para fases posteriores, no columnas ni comportamiento ya disponibles.

### Evidencia temporal y verificación de integridad

Los informes privados están fuera del repositorio y **no tienen garantía de retención durable**. Código/anotaciones/comandos permanecen en `experiments/`; los siguientes hashes permiten verificar esta ejecución si sus artefactos siguen disponibles:

| Artefacto | SHA-256 |
|---|---|
| `/tmp/opencode/brica-spec002-f0-run-06/report.json` | `b96b98cd8d5a6320df7084d5fb428cf4bf27912e10cab34a121c183af618eda3` |
| `/tmp/opencode/brica-spec002-f0-run-06/manifest.json` | `c6a12f25a11bd31c4c81c32fc80d38203e5ddd561ed6970ef749f8375c251d39` |
| `/tmp/opencode/brica-spec002-f0-audit-02.json` | `45f66973a97ff6966a8eae6ce08dbfab81294bc67fba78f8bd1b52e3ebb10278` |

### Bloqueo comprobado y siguiente paso

- El host permite Bubblewrap; la imagen actual no incluye Bubblewrap/OCR. Pruebas Docker efímeras sin red, DB, volúmenes ni secretos devolvieron **EPERM** al crear namespaces USER+NET, tanto endurecidas (`--read-only --cap-drop ALL --security-opt no-new-privileges`) como con permisos por defecto y UID 10001. No se atribuye aún el fallo a una regla concreta de seccomp/AppArmor.
- No se probaron cgroup/cuotas de memoria/tareas/scratch agregados; RLIMIT/heap/timeout por sí solos **no** satisfacen P29. Conforme a F0.6, se detiene la integración y se consulta antes de cambiar imagen/perfil de seguridad. No usar contenedor privilegiado, Docker socket, `SYS_ADMIN` ni lectores dentro del proceso backend como atajo.
- **F0 abierta; F1 no iniciada.** Siguen pendientes: aislamiento reproducible en el destino, recursos agregados, seguridad/licencias del runtime nativo y del grafo Spring integrado, OCR degradado/rotado y geometría/mezcla, archivos activos, cachés XLS y resto del corpus. Validación real requiere identificación/autorización explícita.
- Comprobaciones editoriales: `git diff --check -- specs/002` correcto para archivos seguidos; validación de sintaxis Python/JSON correcta. La inspección de todos los textos detectó espacios finales preexistentes en `pendients.md:74,102`; no se modificó ese archivo ajeno al bloque. Verificación de espacios de archivos creados/modificados realizada aparte. No se ejecutó la suite Maven completa: no se modificó la aplicación y estas pruebas aisladas no se declaran regresión F10.
- Sin instalación en el host, cambios de Compose/Dockerfile/POM de aplicación, alteración de datos/volúmenes/base persistente, despliegue, commits, push ni cambios de rama.

## F0 — 2026-11-17: imagen y perfil de aislamiento experimentales

### Autorización, alcance y causa del EPERM

- El responsable respondió **Sí** a evaluar una imagen experimental y un perfil específico, sin alterar el despliegue ni conceder privilegios amplios. Se revisaron rama/estado, reglas del repositorio, launcher F0 y ficha; todos los cambios previos se conservaron.
- Se consultaron documentación Docker y libseccomp mediante Context7; la traza `strace` del contenedor identificó `clone(NEWNS|NEWCGROUP|NEWUTS|NEWIPC|NEWUSER|NEWPID|NEWNET|SIGCHLD) → EPERM` bajo seccomp default. El mensaje genérico de Bubblewrap no prueba que el host prohíba namespaces: el perfil específico hizo funcionar el mismo setup sin capacidades del host.
- Imagen **separada** `brica-spec002-f0-sandbox:experimental`, basada en la imagen local del servidor `noble`, sin iniciar el JAR. Root solo en build para instalar paquetes experimentales; ejecución UID 10001, rootfs de lectura, `cap-drop ALL`, `no-new-privileges`, red Docker `none`, sin Docker socket/DB/secretos/volúmenes persistentes. Las variables DB del experimento son únicamente marcadores ficticios para verificar que no pasan al payload.
- El contexto Docker se creó fuera del proyecto con una lista explícita de archivos de experimento. **No se envió la raíz de BRICA ni `docs/templates/` al build**. No se copiaron ni leyeron documentos reales.

### Archivos y decisiones implementadas en este bloque

| Acción | Ruta | Responsabilidad / trazabilidad |
|---|---|---|
| Directorio creado | `specs/002/experiments/container/` | Evaluación reproducible de P29/DP-05 dentro de una imagen del mismo linaje de despliegue. No es servicio o módulo de aplicación. |
| Archivo creado | `specs/002/experiments/container/Dockerfile` | Imagen de prueba `noble` con Bubblewrap/Tesseract/libseccomp/strace; no modifica `brica-server/Dockerfile`. |
| Archivo creado | `specs/002/experiments/container/prepare_context.py` | Contexto externo mínimo, solo archivos nombrados, sin documentos/configuración del proyecto. |
| Archivo creado | `specs/002/experiments/container/create_profile.py` | Perfil exterior derivado de allowlist Moby fijada por commit/hash, con tres reglas añadidas y rechazo de upstream/perfil/arquitectura no revisados. |
| Archivo creado | `specs/002/experiments/container/payload_filter.py` | Filtro BPF obligatorio posterior a setup; bloquea montajes/namespaces/red/ptrace/BPF, conserva fork/threads para Java/OCR. |
| Archivo creado | `specs/002/experiments/container/probe.py` | Pruebas dentro de contenedor/sandbox: archivos, capacidades, filtros, red, cuota tmpfs, timeout/salida, lectores y stress acotado de PID/memoria. |
| Archivo creado | `specs/002/experiments/container/run_container.py` | Launcher en host que fija image ID y hash de perfil, valida `docker inspect`/cgroup, monta solo copias sintéticas, registra y retira únicamente IDs creados por su invocación. Docker no es accesible desde el payload. |
| Archivo creado | `specs/002/experiments/container/README.md` | Modelo de aislamiento, reproducción, versiones, criterios y límites de lo demostrado. |
| Archivo creado | `specs/002/experiments/container_profiles_test.py` | Tres tests unitarios nuevos del pin/upstream deny-by-default y de los flags/syscalls adicionales exactos. |
| Archivo modificado | `specs/002/experiments/evaluate.py` | Parámetro opcional `pass_fds` para que Bubblewrap reciba el filtro BPF; por defecto conserva el comportamiento del launcher host. |
| Archivos modificados | `specs/002/experiments/README.md`, `specs/002/ficha-tecnica-f0.md`, `specs/002/plan.md`, `specs/002/Review.md` | Distinguir bloqueo inicial, prueba de namespaces viable y pendiente de recursos **por worker**, sin declarar F0 terminada. |

### Política de preparación y filtro de payload

- Upstream: `moby/profiles`, commit `9ee3204f0d0cfa19ca2c6a7b0fcf5a2b353954ae`, `seccomp/default.json`; SHA-256 `3320527d4568265ac97a077609b1bfd4cfd0d789eeb9239b7e094b1526675406`. Mantiene `SCMP_ACT_ERRNO` por defecto y la allowlist original. El perfil derivado queda fijado a SHA-256 `5ef564bebbaee166b6b5ed0c8fe72746019ee95b37dfbefe7241306fa0a01b2d`.
- Adiciones observadas: `clone` con flags exactos `0x7e020000|SIGCHLD`, `unshare(NEWUSER)` y `mount/umount2/pivot_root`. No permitir unshare/clone arbitrarios, no usar `unconfined` ni añadir `SYS_ADMIN`. El primer perfil de investigación solo permitía unshare/montajes y aún fallaba por clone; se añadió únicamente la combinación observada y se retiraron variantes unshare que no eran necesarias.
- El filtro interior obligatorio se aplica tras setup, con filtros heredados por hijos. Deniega `unshare/setns`, montaje (incluidas APIs modernas), clone con flags de namespaces, ptrace/memoria ajena/BPF y sockets distintos de AF_UNIX; clone3 devuelve ENOSYS para fallback normal. Se comprueba `Seccomp=2`, al menos dos filtros, `NoNewPrivs=1`, `CapEff=0` y denegaciones efectivas, no solo la presencia de opciones CLI.
- Root del sandbox de lectura, montajes fuente/clases/JARs/runtime de lectura, entorno saneado, marcadores privados/backend/cgroup ocultos. `/tmp` es el único scratch administrado y tiene cuota de 128 MiB. No crear `/dev` como otro tmpfs escribible: bind de null/random/urandom y directorio en root de lectura. El filtro impide crear un tmpfs alternativo.
- Este perfil exterior abre llamadas de **preparación**: no aplicarlo sin el filtro interior, no instalarlo como política global ni afirmar que es un perfil de producción auditado. Solo Linux amd64; otras arquitecturas fallan explícitamente.

### Evidencia medida

- Paquetes efectivos de la imagen: Bubblewrap `0.9.0-1ubuntu0.3`, libseccomp `2.5.5-1ubuntu3.1`, Tesseract `5.3.4-1build5`, Leptonica `1.82.0-3build4`, modelos eng/spa `1:4.1.0-2`. Se evalúa el runtime **noble**, no se copian los binarios resolute del experimento host ni se asegura que ya terminó su auditoría de seguridad/licencias.
- Builds separados y trazas bajo `/tmp/opencode/brica-spec002-f0-container/`. Las ejecuciones `container-run-01`, `-02` y `-03` pasaron los tres modos **normal, pids y memory**. El tercero usa perfil con pin verificado, validación previa de restricciones y lectura de configuración efectiva; 13 comprobaciones en total (9 + 2 + 2, incluyendo el acuse del payload repetido por modo).
- **Normal:** namespace/red diferente del launcher, sockets de red/montajes/nuevos namespaces denegados tras setup, fuente/root de lectura, entorno/archivos privados ocultos, timeout con hijo y salida limitada. Cgroup normal: `memory.max=1073741824`, `memory.swap.max=0`, `pids.max=64`, `cpu.max=100000 100000`. Cuota CPU verificada como configuración, no como benchmark.
- **Scratch:** varios archivos acotados individualmente llegan a **134217728 bytes** y el siguiente incremento falla con ENOSPC. Es comprobación agregada de tmpfs, no solo RLIMIT_FSIZE por archivo.
- **PID:** intento finito de hasta 80 hijos, EAGAIN tras **59**; `pids.events:max` aumenta en **2** en la tercera ejecución. Todos los hijos recolectados. Límite de 64 incluye launcher/Bubblewrap y demás tareas del cgroup; no significa 64 conceptos o trabajos simultáneos.
- **Memoria:** contenedor de stress a **256 MiB**, sin swap, dos hijos intentan mantener 160 MiB cada uno. Salidas `[0,-9]`, incremento `memory.events:oom_kill=1`. Launcher y contenedor finalizan con código 0; Docker registra `OOMKilled=true` porque mató un descendiente. Es el resultado esperado de la prueba de límite, no una garantía de recuperación del backend.
- **Lectores/OCR:** 28 aserciones correctas también en la imagen `noble`; tercera ejecución: **4,122 s** de pared para el grupo completo, con cuota de 1 CPU. Los 11 hashes fuente coinciden antes/después. No se promete rendimiento bajo carga ni se infiere calidad universal del OCR.
- **Control negativo:** la imagen con el seccomp default vuelve a rechazar el payload con código 1 (`baseline-final.json`). Se verifica fallo cerrado; ningún lector se ejecuta en fallback sin filtro/aislamiento. El texto genérico de Bubblewrap se conserva como diagnóstico, no se usa para atribuir una restricción global al kernel.
- **Regresión del banco host:** nueva ejecución `/tmp/opencode/brica-spec002-f0-run-07/report.json`, 10/10 comprobaciones correctas tras extender `pass_fds`. **Tests unitarios:** `python3 -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v` → 6/6 (tres de perfil nuevas y tres decimales previas).

### Pendiente no resuelto por estos contenedores

- `/sys/fs/cgroup` está montado **ro** en el contenedor y no es escribible por UID 10001. No se dispone de subárbol delegado para aislar recursos de un worker que conviva con el backend.
- Los límites agregados probados pertenecen al **contenedor experimental completo**. Aplicarlos a todo el servidor no protege al backend ante presión de memoria/PID del lector: la prueba OOM demuestra justamente la necesidad de tratar esa frontera. No se ha introducido Docker socket, servicio/broker nuevo ni privilegios como atajo.
- El bloqueo inicial de namespaces tiene una solución técnica reproducible en imagen/perfil específicos; el **despliegue/integración** requiere decidir y evaluar delegación cgroup o mecanismo local equivalente, sin alterar arquitectura por esta autorización. F0 sigue abierta; F1–F11 no iniciadas.
- Faltan además evaluación completa nativa/grafo Spring, corpus hostil/ampliado, OCR degradado/rotado y mezcla automática, cachés XLS y autorización/manual real. Estos experimentos no sustituyen F10 ni una prueba del backend real con concurrencia/recuperación.
- Sin cambios a Compose, Dockerfile/POM de aplicación, base persistente, datos/volúmenes, cuentas/permisos, servicios en ejecución, commits/push o rama. Se conserva la imagen experimental local; no se ha hecho prune ni limpieza general de imágenes/volúmenes.

### Cierre del bloque experimental autorizado

- Se añadió al filtro interior también la denegación de `socketpair` no AF_UNIX y su comprobación real. La imagen final incluye ese endurecimiento y el código vigente; la ejecución `/tmp/opencode/brica-spec002-f0-container-run-04/` pasa nuevamente **13/13 comprobaciones**, con **28/28 aserciones del lector**. Imagen final local: `sha256:26ed71ae710db5cf18f28b2a68ef2ea9b38038f3722b11538f30bebb31553506`.
- Última medición: lectores/OCR **4,234 s**; PID EAGAIN tras 59 hijos y dos eventos de límite; memoria `[-9,0]` y un evento OOM-kill esperado. Orden de muerte/salida de hijos no es determinista. Normal no produjo eventos OOM ni PID-exhaustion. Integridad de los 11 fixtures correcta en los tres modos.
- `docker ps -a --filter label=brica.experiment=spec002-f0` no devuelve contenedores restantes; el runner eliminó solo los suyos. La imagen/caches/contextos e informes experimentales se conservan. No hubo limpieza de imágenes/volúmenes ni de trabajo ajeno.
- Comprobaciones editoriales finales: `git diff --check -- specs/002` correcto; AST Python y espacios finales de fuentes/documentación de este bloque correctos. No se ejecutó Maven: no se cambió código/configuración/dependencias de la aplicación; esto no se presenta como regresión completa F10.
- Informes externos **temporales**, sin garantía de retención durable. Fuentes/instrucciones de reproducción permanecen en `experiments/container/`; hashes de la ejecución final:

| Artefacto bajo `/tmp/opencode/brica-spec002-f0-container-run-04/` | SHA-256 |
|---|---|
| `summary.json` | `34185e2768b837ea8949bf412f4362db1456efac07f425d1a8864ed888aa5a0f` |
| `normal-stdout.json` | `ae7a0ffabbb400efda7b7891af77d71ebcec86ec6ebf835b9f157613795b8c67` |
| `pids-stdout.json` | `eebcf07d5f6b6e55b5aa0445c677098d81a392fff23275f11f8af557c0a63d63` |
| `memory-stdout.json` | `d1925a3ecd4baf5e1b40f60f10aeb4d287563c126f393c2dacd237a0acb00593` |

**Resultado:** evaluación de imagen/perfil completada con éxito en este entorno; no cierre de F0 ni autorización para desplegar. El siguiente bloqueo concreto es limitar de forma independiente el worker dentro del despliegue, sin compartir la frontera OOM del backend ni introducir servicios/privilegios no aprobados.
