# Plan de implementación — Spec 002

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Funcionalidad:** extracción, revisión y confirmación de fuentes
**Actualización:** 2026-11-20
**Estado:** implementación parcial; aislamiento operativo y aceptación pendientes
**Spec:** [spec.md](spec.md) · **Seguimiento:** [review.md](review.md)
**Límites:** la actualización documental no autoriza cambios funcionales, migraciones, datos reales, despliegue, commits ni publicación.

## 1. Resultado, alcance y fuentes

Procesar PDF/XLS/XLSX/DOCX de una obra, revisar propuestas o captura manual y confirmar por separado obra, cliente y presupuestos. No comprende generación, exportación ni alta automática de entidades. Fuentes: Spec 001, ERS/DAS/Diccionario, decisiones históricas P/DP, código, POM, configuración, migraciones y pruebas. Rama observada `actualization`; existen cambios locales ajenos que deben preservarse.

## 2. Diagnóstico del repositorio

| Área | Situación observada | Implicación |
|---|---|---|
| `brica-server/.../extraction/` | API, servicios, persistencia, lectores y worker presentes. | Comprobar requisitos concretos; código no implica cierre integral. |
| `brica-server/.../budgets/` | Modelo, servicios y API de presupuestos/revisiones presentes. | Verificar inmutabilidad, consultas y confirmaciones por bloque. |
| `brica-client/.../extraction/` | Ventana independiente, editor y mapeo presentes. | Probar recorrido real, errores y permisos en Windows. |
| `db/migration/V15–V18` | Denominaciones/revisión de cliente, trabajos, presupuestos e historial de intentos definidos. | No editar migraciones; esquema inferido del código, no consultado en base alguna. |
| `application.yml` / `brica_extraction.py` | Worker habilitado por defecto; `BRICA_EXTRACTION_CGROUP_ROOT` vacío por defecto. | Sin cgroup delegado Linux el procesador falla cerrado; no asumir despliegue listo. |
| `pom.xml` | PDFBox 3.0.8 y POI 6.0.0 declarados. | La bitácora F0 que los describe como no integrados es histórica, no estado actual. |
| `experiments/` | Banco sintético y aislamiento experimental independiente. | Conservar por ahora; extraer herramientas útiles antes de considerar su eliminación en otra tarea. |

## 3. Decisiones técnicas y de negocio

Las elecciones P01–P33 se registraron el 2026-11-17; sus letras corresponden a las alternativas originales. No atribuir aprobador o fecha individual no documentados. Los detalles DP de F0 se resumen abajo y en [review.md](review.md); una política elegida no demuestra su implementación ni su verificación.

| ID | Elección | Consecuencia para la implementación | Fases |
|---|---|---|---|
| P01 | B | Extraer, revisar y confirmar selectivamente; no solo mostrar texto. | F3, F7–F10 |
| P02 | C | Añadir DOCX fuente a PDF/XLS/XLSX; no reutilizar el analizador de plantillas como lector. | F1, F4–F5 |
| P03 | B | Lectura genérica y mapeo manual asistido, sin perfiles rígidos por cliente como condición. | F0, F4–F6 |
| P04 | A | Intentar los destinos de negocio admitidos sin depender de un paquete. | F1, F6 |
| P05 | B | Asociar candidatos a destinos permitidos y guardar opcionalmente un perfil de mapeo. | F6, F9 |
| P06 | B | Prioridad explícita por campo/clase de fuente, conservando alternativas y confirmación humana. | F0, F6 |
| P07 | B | Leer posiciones/bloques y tablas identificables en PDF. | F4 |
| P08 | A | OCR únicamente en páginas/regiones con lectura digital insuficiente. | F4 |
| P09 | A | Lectores Java; OCR como proceso local externo, sin nuevo servicio. | F0, F3–F5 |
| P10 | B | Detectar estructura Excel y permitir corregir columnas/rangos/filas. | F5, F9 |
| P11 | A | Usar valores almacenados de fórmulas; conservar la fórmula como evidencia, no evaluarla. | F5 |
| P12 | B | Usuario agrupa hojas como partes, resúmenes o alternativas; no fusionar automáticamente. | F5, F8–F9 |
| P13 | C | Cabecera/identidad/revisiones explícitas y contenido flexible acotado. | F1, F8 |
| P14 | B | Revisiones presupuestales inmutables y elección explícita de revisión principal. | F8 |
| P15 | A | Evidencia mínima: fuente/hash, ubicación, original y destino sugerido. | F1, F4–F6 |
| P16 | B | Separar nombre técnico y denominación documental literal. | F1–F2, F7 |
| P17 | A | Convenciones regionales explícitas por fuente/perfil y cronología vigente. | F0, F6 |
| P18 | B | Comprobar cantidades/importes por separado, sin sustituir los importes fuente. | F6, F8 |
| P19 | B | Datos sin atributo fijo pueden asociarse expresamente a adicionales existentes. | F6–F7 |
| P20 | A | Fuente común en obra/cliente y presupuestos propios; no duplicar valores en `contracts.data`. | F7–F8 |
| P21 | A | Completar ausentes o corregir captura con justificación; no tramitar convenios. | F7–F8 |
| P22 | B | Confirmaciones independientes con resultados claros por destino. | F7–F9 |
| P23 | A | Control optimista por destino, incluido cliente; sin fusión automática por campo. | F2, F7–F8 |
| P24 | A | Clave y fingerprint por petición/contexto; reintentar no vuelve a aplicar cambios. | F3, F7–F8 |
| P25 | B | Trabajos persistidos y ejecución local limitada con consulta de estado. | F3 |
| P26 | B | Borradores de revisión en servidor, con reanudación y control de revisión. | F3, F6–F9 |
| P27 | B | Ventana separada de extracción, no agrandar el asistente actual. | F9 |
| P28 | A | Reutilizar permisos por destino, sin nuevos permisos ni roles. | F1–F10 |
| P29 | A | Procesos limitados dentro del despliegue actual; un subproceso no equivale a sandbox. | F0, F3–F5 |
| P30 | A | Referenciar el adjunto existente; no copiarlo por análisis. | F1, F3 |
| P31 | A | Operación durable en fases, publicación verificable, auditoría y recuperación idempotente. | F1–F3, F7–F11 |
| P32 | A | Entregar datos/colecciones confirmados mediante contratos propios; sin generación. | F8, F10 |
| P33 | A + validación manual de B | Corpus anotado, métricas por campo/fila y prueba manual con archivos reales autorizados. | F0, F10–F11 |

- **Precedencia:** contrato para número, contratista, fechas y montos contractuales; Excel seleccionado para conceptos, unidades, cantidades y precios. Otras claves y contradicciones permanecen sin elección automática.
- **Valores:** `CONTROL_V1` usa producto exacto, redondeo `HALF_UP` a escala 2 por concepto y suma de controles; conservar total declarado y suma exacta. Diferencias requieren justificación de 10–2000 caracteres; no suponer moneda/IVA.
- **Correcciones y retención:** confirmar solo cambios seleccionados; corrección de captura requiere justificación y no equivale a convenio. Sin purga automática de fuentes, revisiones o auditoría en v1. No trasladar datos reales al corpus sin autorización específica.
- **Aislamiento:** no usar `privileged`, `SYS_ADMIN`, socket Docker ni un servicio nuevo como solución implícita. El perfil experimental solo es válido junto con su filtro interior, no es una configuración aprobada para producción.

## 4. Diseño y cambios previstos

- **Flujo:** JavaFX → API `/api/v1` → servicios de extracción/presupuesto → puertos y persistencia; worker local lee copias de adjuntos y devuelve fragmentos, sin acceso directo del lector a DB o credenciales.
- **API presente:** contexto, perfiles, trabajos, candidatos/sugerencias, revisión, confirmaciones, cancelación, cierre e intentos en `ExtractionController`; consultas y principal de presupuestos en `BudgetController`. Las rutas reales prevalecen sobre las orientativas del plan histórico.
- **Persistencia:** V15 crea denominaciones y revisión de cliente; V16 trabajos/fuentes/revisiones/confirmaciones/perfiles; V17 presupuestos y revisiones; V18 historial de intentos. Flyway es dueño del esquema; no se comprobó ninguna instancia.
- **Concurrencia y recuperación:** claves de operación, ETag, revisiones por destino, evidencia inmutable y confirmación independiente; un reanálisis crea un trabajo vinculado. Verificar atomicidad y recuperación de extremo a extremo antes de declarar cierre.
- **Rutas principales:** `brica-server/src/main/java/com/brica/{extraction,budgets}/`, `brica-server/src/main/python/brica_extraction.py`, migraciones y pruebas del servidor; `brica-client/src/main/java/com/brica/client/extraction/` y pruebas del cliente.

### Políticas técnicas v1 y límites a verificar

Son límites de diseño seleccionados en F0, no una garantía de capacidad o cobertura completa. Contrastar cada valor con código, configuración y pruebas antes de operar.

| Recurso | Política v1 |
|---|---|
| Fuente/trabajo | 10 MiB por archivo; hasta 4 fuentes y 40 MiB por trabajo. |
| PDF/OCR | 40 páginas; 200 DPI y 12 millones de píxeles por página/región; hasta 10 páginas o 20 regiones OCR, `spa+eng`, 20 s por región y 120 s por intento. |
| Excel/OOXML | 50 hojas, 20 000 filas, 200 000 celdas, 256 columnas; 512 entradas ZIP, 20 MiB por entrada y total, razón mínima 0,01 tras 100 KiB. |
| DOCX/resultado | 20 000 bloques, 200 tablas, 16 384 caracteres por contenido; protocolo 1 MiB; HTTP hasta 100 filas/256 KiB. |
| Metadatos y decimales | Metadatos versionados hasta 16 KiB/profundidad 4; valores propuestos hasta 38 dígitos y 18 decimales, preservando literal si exceden el rango. |
| Worker | Uno por nodo; heap 256 MiB, espacio virtual 2 GiB, CPU 60 s, archivo 16 MiB, 128 descriptores, pared 120 s. Frontera agregada: memoria ≤1 GiB, ≤64 tareas, scratch ≤128 MiB. |
| Recuperación | Lease 30 s, heartbeat 10 s; hasta 2 reintentos automáticos de lectura tras pérdida del ejecutor, no de confirmaciones. Scratch abandonado: propuesta de 24 h bajo raíz administrada y lease inactivo; no activar purgas históricas. |

El banco F0 probó solo parte de estos límites. El procesador actual usa 125 s de plazo exterior; comprobar coherencia de ambos plazos y límites antes de afirmar cumplimiento. Las convenciones regionales deben ser explícitas; no evaluar fórmulas y tratar la caché XLS como no verificada.

## 5. Fases y criterios de salida

Los estados indican presencia de código observada, no aceptación ni prueba ejecutada en esta actualización. El orden histórico fue F0 → F1/F2/F3 → F4/F5 → F6 → F7/F8 → F9 → F10 → F11; la integración observada no demuestra que se haya cumplido cada puerta previa.

| Fase | Implementación / áreas | Salida y dependencia | Estado observado |
|---|---|---|---|
| F0 · Contrato y seguridad | Alcance, corpus sintético, límites, lectores y aislamiento; `experiments/`. | Evaluar licencias, corpus y frontera efectiva por worker antes de operación. | Experimentos históricos parciales; aislamiento del destino y evaluación ampliada pendientes. |
| F1 · Modelo | Contratos, referencias, fuentes, trabajos, auditoría y migraciones; V16–V18. | Integridad en instalación nueva y actualización con datos de prueba; depende del diseño F0. | Esquema y pruebas de migración presentes; no ejecutadas aquí. |
| F2 · Destinos | Denominaciones separadas, claves nuevas, ETag de cliente/obra y comandos selectivos; V15. | Cambios concurrentes rechazados; claves históricas intactas. | Código y migración presentes; regresión integral pendiente. |
| F3 · Ejecución | Adjuntos verificados, trabajos, leases, intentos, cancelación y sandbox; `extraction/.../worker`, Python. | Recuperación/idempotencia y límites agregados comprobados; depende de F0–F2. | Componentes presentes; cgroup delegado y prueba del destino pendientes. |
| F4 · PDF/OCR | Lectura digital, posiciones, OCR local y diagnóstico parcial; `SourceReader`. | PDF digital/escaneado/mixto anotados, regiones y fallos comprobados. | Código presente; calidad, mezcla y casos hostiles aún por verificar integralmente. |
| F5 · Excel/DOCX | Hojas, celdas, cachés sin evaluación y contenido Word estructural; lector y mapeo. | XLS/XLSX/DOCX y fronteras de seguridad verificadas; depende de F3. | Lectores y pruebas presentes; cobertura de cachés XLS y corpus ampliado pendientes. |
| F6 · Revisión | Candidatos, precedencia, mapeo/perfiles y borrador persistente con ETag. | Alternativas/evidencia se conservan, sin cambios de negocio; depende de F2–F5. | Servicios y UI presentes; contrastar cobertura completa con criterios EX. |
| F7 · Obra/cliente | Confirmación selectiva, permisos separados, corrección y auditoría. | ETag, reintento y atomicidad por destino; depende de F2/F3/F6. | Servicios y pruebas focalizadas presentes; permisos reales y recorrido integral pendientes. |
| F8 · Presupuestos | Bloques, conceptos, controles decimales, revisiones inmutables y principal explícita. | Alternativas separadas, historia consultable e idempotencia; depende de F1/F5/F6. | Modelo, API y pruebas presentes; validación integral pendiente. |
| F9 · Escritorio/API | Ventana desde obra/adjunto, carga, revisión, mapeo, reanudación y confirmación. | Flujo asíncrono, errores y permisos en Windows; depende de F3–F8. | Código presente; aceptación visual pendiente. |
| F10 · Regresión/aceptación | Pruebas de lectores, migración, concurrencia, seguridad, corpus y recorrido manual. | EX-01–EX-14, suite y evidencia anotada; sin datos reales no autorizados. | Pruebas presentes; no ejecutadas en esta edición, aceptación pendiente. |
| F11 · Operación | Configuración, despliegue, respaldos, recuperación y retención. | Destino identificado, aislamiento y respaldo probados, autorización específica. | Pendiente; no se autoriza desplegar ni tocar volúmenes. |

## 6. Verificación y trazabilidad

Los identificadores EX siguientes son los criterios históricos del plan, preservados sin cambiar su significado:

| Criterio | Resultado verificable | Referencias | Fases |
|---|---|---|---|
| EX-01 | PDF digital/escaneado/mixto produce candidatos y evidencia, sin escritura de negocio. | CA-01–CA-04, MD-001–MD-002; P07–P09 | F3–F4, F10 |
| EX-02 | XLS/XLSX muestra hojas, permite rangos/columnas, distingue conceptos/resúmenes y conserva resultados de fórmula sin evaluarla. | CA-06–CA-10, MD-003; P10–P12 | F5, F9–F10 |
| EX-03 | DOCX fuente se lee sin requerir marcadores; su original y analizador de plantillas quedan intactos. | P02-C, P09-A | F5, F10 |
| EX-04 | Mapeo asistido y precedencia conservan alternativas; unknowns solo se asocian a adicionales existentes compatibles. | CA-03/CA-11–CA-13; P04–P06/P19 | F6, F9–F10 |
| EX-05 | Denominación literal y nombre técnico coexisten; claves/plantillas antiguas no cambian significado. | P16-B y SPEC001 | F2, F7, F10 |
| EX-06 | Fechas y decimales se interpretan con configuración explícita; diferencias de cálculo no sobrescriben importes fuente. | CA-08/CA-13; P17–P18 | F6, F8, F10 |
| EX-07 | Cada destino confirma solo lo aceptado, con permiso, ETag, auditoría y justificación cuando corresponde. | CA-11–CA-14, MD-004; P20–P23/P28 | F7–F10 |
| EX-08 | Confirmar obra no confirma cliente/presupuestos; cancelar no revierte bloques ya aplicados. | P22-B/P26-B | F7–F10 |
| EX-09 | Presupuestos conservan partes/alternativas, revisiones inmutables, orden y principal explícita; conceptos consultables paginados. | CA-09/CA-15, MD-005; P12–P14/P32 | F8–F10 |
| EX-10 | Reintento idéntico devuelve el resultado original; reanálisis y nueva intención no duplican confirmaciones ni destruyen decisiones previas. | P24-A/P31-A | F3, F6–F8, F10 |
| EX-11 | Cerrar cliente/reiniciar servidor conserva revisión y recupera trabajos sin aplicar datos automáticamente. | P25-B/P26-B | F3, F9–F11 |
| EX-12 | Fuente eliminada lógicamente conserva evidencia protegida; pertenencia y límites impiden accesos/lecturas indebidos. | CA-05, MD-014; P29–P31 | F1, F3, F10–F11 |
| EX-13 | Suite anotada y recorrido manual demuestran límites conocidos y calidad acordada, sin promesa universal. | P03-B/P33-A+B | F0, F10–F11 |
| EX-14 | No se producen DOCX/PDF de salida, no se habilita Generar ni se alteran versiones/paquetes como efecto de extraer. | P32-A e instrucción de alcance | Todas |

| Comprobación | Comando / entorno | Criterio |
|---|---|---|
| Regresión | `./mvnw test` y `./mvnw verify`; Docker/Testcontainers y Python según README. | Sin fallos ni omisiones injustificadas; registrar resultados reales. |
| Banco sintético | `python3 -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v`; ejecución completa según `experiments/README.md`. | Resultados y límites por formato registrados; no sustituye pruebas de aplicación. |
| Diff y enlaces | `git diff --check` y comprobación de enlaces afectados. | Sin errores introducidos. |
| Manual | Flujo de [spec.md](spec.md), permisos y concurrencia; Windows y datos de prueba. | Evidencia por criterio, actor y entorno; muestras reales solo con permiso por archivo. |

## 7. Riesgos, bloqueos y próximos pasos

- No se ha demostrado en esta revisión una frontera agregada independiente por worker en el despliegue; falta comprobar también dependencias nativas, corpus hostil, OCR degradado/mixto y cachés XLS.
- No consultar ni modificar bases, volúmenes o documentos reales sin autorización. Código y resultados experimentales históricos no implican despliegue ni aceptación.
- Siguiente paso sujeto a autorización: preparar verificación técnica focalizada y después recorrido manual; extraer herramientas útiles de `experiments/` en una tarea independiente antes de decidir su destino.
