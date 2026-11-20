# SPEC002 — Ficha técnica F0, versión 1

**Fecha:** 2026-11-17. **Estado:** decisiones de diseño adoptadas bajo delegación del responsable; validación técnica incompleta. No habilita integración de lectores, migraciones ni despliegue mientras el aislamiento del destino no esté resuelto.

## 1. Lectores y evaluación

- Candidatos exactos: PDFBox **3.0.8**, POI/POI OOXML **6.0.0**, Tesseract **5.5.0**, Leptonica **1.86.0**. Los dos primeros siguen fuera de los POM de BRICA; OCR se descarga/desempaqueta exclusivamente bajo `/tmp/opencode`, sin instalación de paquetes del host.
- Ampliación autorizada de contenedor: la imagen experimental basada en `noble` usa **Tesseract 5.3.4**, **Leptonica 1.82.0**, **Bubblewrap 0.9.0** y **libseccomp 2.5.5** de esa distribución. No copia el runtime `resolute` del host y no cambia el candidato Java. Son versiones evaluadas, no una incorporación al Dockerfile/POM de producción ni evaluación completa de seguridad nativa.
- Las pruebas ejecutables están en [experiments/](experiments/README.md). Son experimentos, no adaptadores de producción, endpoints ni procesos registrados en la aplicación.
- PDFBox/POI se publican bajo Apache-2.0. El inventario de los JARs registra licencias directas del POM y hashes de LICENSE/NOTICE; herencia, avisos de componentes incorporados y obligaciones de distribución deben revisarse antes de empaquetar. CurvesAPI declara BSD; SparseBitSet declara Apache-2.0.
- Se leyeron los archivos `copyright` de los paquetes desempaquetados de Tesseract, sus modelos y Leptonica: Tesseract/modelos declaran Apache-2.0 (el paquete documenta también un script auxiliar MIT); Leptonica incluye permiso BSD de dos cláusulas. Esto no cierra la revisión del resto de bibliotecas nativas ni su vulnerabilidad en el destino `noble`.
- OSV se consulta por coordenada/versión de cada dependencia transitiva del prototipo. Una respuesta sin alertas significa **sin coincidencias informadas en esa consulta**, no ausencia de vulnerabilidades. No sustituye comprobar la resolución efectiva con Spring Boot ni auditar el runtime nativo de OCR.
- No incorporar un proveedor de Log4j distinto del logging del servidor para silenciar el prototipo: primero verificar mediación con el BOM vigente. La advertencia del prototipo queda registrada como pendiente.

## 2. Límites seleccionados y evidencia exigida

Son **políticas iniciales conservadoras**, no capacidad de procesamiento demostrada. El servidor deberá validarlas antes de encolar y el worker durante la lectura; superar un límite devuelve diagnóstico recuperable y nunca confirmación.

| Recurso | Política v1 | Evidencia actual |
|---|---|---|
| Fuente | 10 MiB por archivo; hasta 4 fuentes y 40 MiB referenciados por trabajo | Techo por archivo coincide con almacenamiento actual; trabajo aún no implementado. |
| PDF | Hasta 40 páginas; revisar imágenes incluso en páginas con texto digital | Fixture de 41 páginas rechazado por guardia experimental; falta corpus de 40 páginas. |
| Rasterizado | 200 DPI, hasta 12 millones de píxeles por página/región antes de reservar imagen; máximo 10 páginas o 20 regiones OCR por intento | Guardia por tamaño de página y dos páginas OCR del corpus; límites acumulados/regiones aún no implementados. |
| OCR | `spa+eng`, una ejecución nativa a la vez; 20 s por página/región y 120 s por intento completo | Tesseract local se prueba dentro del sandbox; el experimento impone 120 s al conjunto, no el timeout individual. |
| Excel | Hasta 50 hojas, 20 000 filas físicas acumuladas, 200 000 celdas almacenadas y 256 columnas por hoja | Límites elegidos, pendientes de implementación y pruebas de frontera. No iterar hasta el último índice vacío sin contadores. |
| OOXML | 512 entradas; 20 MiB por entrada y en suma; razón comprimido/expandido mínima 0,01 tras 100 KiB | POI prueba el rechazo de una entrada muy compresible. Suma, rutas, duplicados y todas las fronteras pendientes. |
| DOCX | 20 000 bloques párrafo/celda y 200 tablas, además de límites ZIP/texto | Caso mínimo probado; fronteras pendientes. No inventar página Word. |
| Texto/resultado | 16 384 caracteres por descripción/contenido de celda, 1 MiB de protocolo combinado stdout/stderr; paginación HTTP posterior ≤100 filas y ≤256 KiB | Corte de salida probado; truncamiento nunca silencioso. Descripciones que exceden límite conservan fuente y exigen segmentación/revisión. |
| Metadatos flexibles | Esquema versionado, 16 KiB por registro, profundidad máxima 4; sin rutas, código ni expresiones evaluables | Decisión para F1, todavía no existe JSONB de extracción. |
| Worker | Uno por nodo; heap Java 256 MiB; espacio virtual por proceso 2 GiB, CPU 60 s, archivo 16 MiB, 128 descriptores, sin core; 120 s de pared | Límites por proceso probados parcialmente en host; **no** limitan memoria, procesos o disco agregados del árbol. |
| Frontera OS requerida | Sin red ni secretos, fuentes de solo lectura; procesos hijos terminados al cancelar; memoria agregada ≤1 GiB, ≤64 tareas, scratch ≤128 MiB | Probado en host e imagen experimental con dos filtros seccomp; quota tmpfs y límites cgroup comprobados con ENOSPC/EAGAIN/OOM. El cgroup limita **todo el contenedor experimental**, no un worker delegado dentro del backend. Prohibido declarar listo para producción. |

### Bloqueo del destino, no excepción de seguridad

El host permite `bwrap --unshare-all`. La imagen actual `brica-dev-brica-server:latest` no incluye Bubblewrap/Tesseract y una prueba efímera con `--network none --read-only --cap-drop ALL --security-opt no-new-privileges` devuelve `EPERM` al intentar `unshare(USER|NET)` desde Python/libc. Es evidencia de esa imagen/configuración, no de todas las configuraciones Linux.

Se repitió sin `--read-only`, `--cap-drop` ni perfil de seguridad adicional (manteniendo `--network none`, sin volúmenes/credenciales): con el UID 10001 de la imagen también devolvió `EPERM`. No se ha atribuido la causa a una regla concreta de seccomp/AppArmor; falta investigarla antes de proponer un perfil mínimo.

No montar el socket Docker dentro de BRICA, no otorgar `privileged` ni `SYS_ADMIN`, ni rebajar seccomp globalmente para resolverlo. No usar el aislamiento de **todo el backend** como sustituto: el backend necesita DB y almacenamiento, el lector no debe heredarlos. Antes de F1/F3 se requiere acordar y probar un mecanismo compatible con el despliegue actual y la prohibición de servicios nuevos. Hasta entonces se detiene **la integración**; continúan solo experimentos sintéticos independientes.

### Actualización autorizada: namespaces viables; delegación aún pendiente

El responsable autorizó evaluar una imagen y perfil específicos, sin alterar el despliegue. [container/README.md](experiments/container/README.md) documenta el banco reproducible:

- Traza del perfil default: `clone(NEWNS|NEWCGROUP|NEWUTS|NEWIPC|NEWUSER|NEWPID|NEWNET|SIGCHLD)` devuelve EPERM. La prueba con perfil acotado permite ese clone, `unshare(NEWUSER)` y los tres syscalls de montaje necesarios. Conserva la allowlist Moby fijada por commit/hash, UID 10001, `cap-drop ALL` y `no-new-privileges`; no desactiva seccomp/AppArmor ni añade capacidades del host.
- Un segundo filtro seccomp obligatorio se aplica **tras** montar el sandbox. Deniega namespaces, montajes y sockets de red al payload y sus hijos, manteniendo fork/threads normales y AF_UNIX. Root y fuentes de lectura, entorno sin marcadores/credenciales, `/tmp` acotado a 128 MiB y sin tmpfs adicionales escribibles. Si falla el filtro, no se ejecuta el lector.
- Se prueba ENOSPC al llenar 128 MiB en varios archivos; EAGAIN al alcanzar 64 tareas del cgroup y evento OOM de un hijo en contenedor de stress a 256 MiB. El contenedor normal usa 1 GiB sin swap y cuota de CPU de 1 núcleo (configuración efectiva verificada, no benchmark de CPU).
- Los 28 asserts del lector/OCR pasan también con paquetes nativos `noble` dentro del sandbox. Los hashes de los 11 fixtures se conservan.
- Esto **resuelve el bloqueo básico de namespaces para esa imagen/perfil**, no demuestra límite agregado independiente de un worker dentro del backend. `/sys/fs/cgroup` no es escribible/delegado al UID 10001. El experimento de memoria puede marcar el contenedor `OOMKilled=true` aunque su launcher salga con 0 al detectar/recolectar al hijo; no se presenta como protección del backend ante OOM de un lector.

No introducir un servicio/contenedor ejecutado dinámicamente por el servidor ni un socket Docker como consecuencia implícita de estas pruebas. Antes de integrar, definir/evaluar delegación de cgroup o mecanismo local equivalente con autorización específica de despliegue. El perfil exterior permite operaciones de preparación: no usarlo sin el filtro interior ni adoptarlo globalmente. Evaluación únicamente Linux amd64, no compatibilidad universal.

## 3. Valores, precisión y discrepancias — DP-17/22–27

- Conservar literal original, tipo, formato visible y número almacenado. XLSX usa el decimal textual de `<v>`; un getter `double` no es el registro de precisión. En XLS documentar la representación binaria disponible, sin prometer recuperar dígitos perdidos.
- Una fórmula no se evalúa, aunque no haya caché. En XLSX comprobar presencia de `<v>` **antes** de consultar getters numéricos: el experimento verifica que la ausencia puede exponerse como `0.0`. Caché ausente, cero explícito, error y valor declarado desactualizado son estados distintos. La interpretación de cachés faltantes en XLS sigue pendiente de investigación.
- Abrir los libros explícitamente en solo lectura (`OPCPackage` con `PackageAccess.READ` y fábrica XLS con `readOnly=true`), además del montaje OS de solo lectura. La prueba detectó un intento de escritura al cerrar un XLSX abierto en el modo predeterminado; no se permite eliminar la protección OS para evitar ese error.
- Valores tipados de propuesta: hasta 38 dígitos significativos y 18 decimales; conservar el literal como evidencia si excede ese rango y bloquear su aplicación numérica hasta corregirlo explícitamente, sin redondearlo para hacerlo caber. El diseño SQL final queda para F1.
- `BigDecimal` en producción, operaciones decimales exactas. Política de control `CONTROL_V1`: multiplicar cantidad × precio sin redondeo intermedio, redondear **cada importe de control de concepto a escala 2 con HALF_UP**, sumar esos importes redondeados. Calcular y mostrar también la suma exacta sin redondear como evidencia secundaria; no mezclar los dos resultados.
- Ejemplo: tres renglones de `1 × 0.335` producen `0.34` cada uno y control total `1.02`; suma exacta `1.005` y redondeo final `1.01`. Si el documento declara `1.01`, se conserva ese valor y se señala diferencia `-0.01` respecto al control elegido. No corregir el documento ni decir que el presupuesto está equivocado por usar otra política.
- La escala 2 es de **comprobación**, no precisión oficial de origen ni inferencia de moneda. Mostrar la regla, persistir su versión con la revisión y no afirmar MXN/IVA si no se declararon. No reutilizar el redondeo de presentación de SPEC001.
- No hay tolerancia que oculte discrepancias: mostrar diferencia decimal exacta y, aparte, su presentación. Si la diferencia no es cero, exigir aceptación explícita y justificación de 10–2000 caracteres para confirmar ese bloque. Diferencias entre declarado y calculado, caché potencialmente obsoleta y cantidades negativas interpretables pueden aceptarse con justificación; jamás se sustituyen valores fuente.
- Bloquear campos seleccionados no interpretables, números fuera de rango, relaciones/orden incoherentes, fuente ausente, revisión obsoleta, falta de permiso y violación de reglas vigentes del agregado. El usuario puede excluir un campo/fila dudoso y dejarlo en evidencia para confirmar el resto de un bloque coherente.
- Un concepto confirmado requiere descripción no vacía; clave, partida, unidad, cantidad, precio e importe pueden no estar registrados. Identidad de fila y orden obligatorios; ausencia no es cero. Sin medición/precio no se calcula importe de control. No sumar filas marcadas como resumen/alternativa/notas a las partes.
- Impuestos: conservar tipo, base, tasa e importe declarados por separado. Solo comprobarlos si los operandos y su relación están explícitos; no añadir IVA, moneda, descuento o una fórmula fiscal por defecto.

## 4. Estados, confirmación y recuperación — DP-31/32/37

- Estado técnico: `QUEUED → RUNNING → COMPLETED | PARTIAL | FAILED | CANCELLED`; reanálisis crea otro intento. `COMPLETED` solo significa lectura terminada. Lease de 30 s, heartbeat de 10 s, hasta 2 intentos automáticos de **lectura** tras pérdida del ejecutor; errores de formato/límites no se reintentan automáticamente. Token de intento impide publicación tardía.
- Estado visible de revisión: `DRAFT`, `CONFIRMED`, `CANCELLED`. Se mantiene `DRAFT` mientras exista un bloque pendiente; mostrar separadamente **parcialmente confirmado** y los resultados por obra, cliente y presupuesto. `CONFIRMED` requiere cerrar explícitamente el conjunto seleccionado y que todos sus bloques estén confirmados; no se deduce de haber leído la fuente.
- En `DRAFT` se edita solo lo pendiente con ETag. Confirmar un bloque congela su snapshot; nuevas correcciones requieren otra operación/revisión. `CANCELLED` impide nuevas confirmaciones y conserva lo previamente confirmado, fuentes y auditoría. Estados terminales no vuelven a borrador; crear otro borrador es intención nueva.
- Ninguna recuperación vuelve a ejecutar confirmaciones de negocio. Confirmación y auditoría/resultados idempotentes son atómicos por destino en DB; cotejar fuente publicada/hash antes de confirmar. Una fuente inaccesible bloquea la confirmación y se informa; no fabricar un archivo vacío ni eliminar referencias para ocultar el fallo.
- Revisiones de presupuesto confirmadas inmutables; principal nullable y modificable solo por acción explícita con revisión del agregado. Un presupuesto nuevo no se vuelve principal automáticamente.
- Borradores/auditoría/resultados/fuentes: sin vencimiento o purga automática en v1; cierre/cancelación no borra evidencia. Scratch privado por intento se elimina al terminar/cancelar; temporales abandonados se proponen para limpieza tras 24 h **solo** dentro de raíz administrada, tras verificar lease inactivo. No activar tareas de limpieza de datos históricos en este avance.

## 5. Mapeo, precedencia y correcciones

- Mapeo manual mínimo: elegir hoja y rango rectangular o tabla estructural DOCX; asignar clave/descripción/unidad/cantidad/precio/importe, excluir filas y definir un nivel de partidas. PDF: fragmentos/página y tablas/regiones identificadas; si no hay estructura fiable, captura manual con cita de página/fragmento. Visor gráfico/segmentación libre aún no incorporados.
- Perfiles declarativos privados del contexto **obra**, inmutables por versión, con las mismas autorizaciones de edición que el borrador; no compartidos automáticamente al cliente. Perfil solo guarda asignaciones de campos/encabezados y convención regional, no valores reales, credenciales, rutas o código. Conservar versión aplicada; cambios no alteran intentos/revisiones previas.
- Precedencia v1: contrato para número contractual, contratista, fechas contractuales, monto contratado, antes de IVA e IVA. Hoja Excel de presupuesto seleccionada para partidas/conceptos/unidades/cantidades/precios. Para los demás campos, sin recomendación automática. Si hay valores contradictorios en la fuente preferida, no elegir ninguno. Prioridad no es confirmación.
- Fecha textual por fuente: convención elegida explícitamente antes de normalizar (p. ej. `dd/MM/uuuu`); no inferirla del locale del servidor. Para números textuales especificar separadores decimal/miles y moneda si se conoce; `1,234` queda ambiguo hasta esa elección. Respetar sistema Excel 1900/1904 y no convertir el día ficticio 1900-02-29 a fecha válida.
- Denominaciones documentales: atributos futuros `WORK_DOCUMENT_NAME`/`CLIENT_DOCUMENT_NAME`, claves nuevas `DENOMINACION_OBRA`/`DENOMINACION_CLIENTE`, hasta 1000 caracteres Unicode, sin controles ni saltos de línea. No cambiar `NOMBRE_OBRA`/`NOMBRE_CLIENTE`, nombres técnicos o plantillas históricas; registros previos quedan nulos.
- Corregir cualquier valor existente seleccionado requiere motivo explícito de corrección de captura y justificación de 10–2000 caracteres, preservando antes/después. Si el responsable indica cambio de condiciones contractuales/convenio, bloquear esa aplicación y conservar propuesta. La máquina no puede deducir jurídicamente del importe si se trata de un convenio; la revisión debe advertirlo y exigir clasificación humana, sin prometer detección automática.

## 6. Aceptación y pendientes

- Expectativas sintéticas anotadas en `experiments/expected.json` antes de ejecutar el lector. Para esos casos se exige coincidencia exacta en texto/código/fecha/importe anotados y presencia de coordenadas/cajas válidas; se mide por campo, **no** una precisión OCR universal.
- La prueba de páginas escaneada/mixta es de texto impreso limpio y sin rotación. Faltan escaneo degradado/rotado, detección de regiones/mezcla sin duplicados, tablas complejas, macros/vínculos, límites acumulados, fallo/cancelación/reinicio en la aplicación y corpus S09/S10 conectado al mapeo.
- No hay validación manual real ni autorización de leer archivos reales. No confundir pruebas de infraestructura con aceptación F10.
- **F0 permanece abierta:** delegación de recursos agregados al worker sin exponer al backend a OOM/PID exhaustion, licencias/seguridad nativas y del grafo integrado, cachés XLS y corpus ampliado pendientes. El aislamiento básico de namespaces sí está probado en imagen experimental autorizada. F1–F11 no se declaran implementadas.
