# Seguimiento de implementación — Spec 001

**Inicio:** 2026-11-09. **Rama:** `main`. **Plan:** [plan.md](plan.md). **Spec:** [spec.md](spec.md).

Este archivo es el registro de avance, no una aprobación automática del módulo. Cerrar cada tarea únicamente cuando exista evidencia, indicando límites y pendientes. No confundir reglas internas probadas con un criterio aceptado de extremo a extremo.

## 1. Alcance autorizado y estado

El responsable solicitó iniciar la implementación, mantener este registro y consultar decisiones no triviales. Tras revisar las fuentes, aprobó explícitamente:

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

**Estado general:** bloque F2 de nombres y base de seguridad de F1 verificados automáticamente. **53 tareas: 18 cerradas y 35 parciales, pendientes, bloqueadas o diferidas.** La actualización de decisiones no cierra tareas de implementación. Hay API de catálogo/valores protegida, acceso JavaFX y administración local implementados; F1-03/05 conservan pendientes manuales o documentales. Última suite existente: `test`, **130 servidor + 14 cliente = 144**, sin fallos/errores/omitidas, tras el arreglo independiente del login. El último `clean verify` registrado sigue siendo el anterior de **130 + 6**, con ambos módulos empaquetados. **F0/F1 completas siguen abiertas; F3 no iniciada; almacenamiento definitivo diferido.** No se ejecutó Maven durante esta actualización exclusivamente documental.

## 2. Fuentes y línea base

- Leídos `AGENTS.md`, constitución, comando `/feature`, memoria, Spec 001 y plan actual. Se respeta la indicación local de trabajar únicamente sobre la Spec 001.
- ERS, DAS y Diccionario ahora contienen información. La nueva solicitud sustituye la exclusión temporal del DAS/Diccionario utilizada al redactar el plan. Extraído y leído el texto de los tres DOCX; no modificados ERS/DAS.
- ERS: RF-03/05 y RNF-02/04 sustentan lectura desde fuente común; CU-03/RF-11/12/15, paquetes; CU-08/RF-13/14, versiones y análisis; RES-03, no alterar datos de obras al modificar plantillas; RF-26, roles.
- DAS: presentación → aplicación → dominio, infraestructura por puertos; autorización en aplicación; usuario registrado, contraseña hasheada y un rol con permisos; archivos físicos en servidor y metadatos/ruta relativa/hash en PostgreSQL.
- Diccionario: clave y nombre visible de campo máximo 50, tipo 30, origen 100, ruta 150. Su descripción de PAQUETE_DOCUMENTAL y atributo `operacion_etapa` son referencias heredadas incompatibles con la nueva D-02; no definir un catálogo para conservarlas. Tipos/identidad documental (D-12), identidad compuesta de versión y ARCHIVO siguen pendientes según fase. El DOCX no se modifica en esta actualización.
- Entorno: WSL/Linux, Java 25.0.4.1, Wrapper Maven 3.8.7, Docker Desktop disponible, PostgreSQL 18.6.
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
| F0-03 | Ejecutar baseline antes de cambios funcionales. | **Cerrada:** 43 pruebas exitosas. |
| F0-04 | Resolver decisiones necesarias para el primer bloque. | **Cerrada:** cinco respuestas explícitas de sección 1. |
| F0-05 | Alinear Diccionario y spec para marcador de 54. | **Cerrada:** una celda cambiada; un byte diferente en `word/document.xml`, resto de entradas ZIP idénticas, XML y ZIP válidos. |
| F0-06 | Cerrar decisiones necesarias y registrar las diferidas según fase. | **Parcial:** D-02/D-07 resueltas; mínimo D-03/D-10 mantenido; D-05 implementada; D-06 diferida y D-09 fuera del flujo contractual. D-04 y D-12 requieren análisis/acuerdo; no se cierra F0 por aprobar la actualización documental. |
| F0-07 | Preparar DOCX representativos y fixtures de análisis/seguridad/estático. | **Parcial:** conservar los cuatro fixtures técnicos; no adoptar el acta como primera plantilla real. Falta elegir otro documento y construir juntos contenido/campos/obligatoriedad. Presupuesto disponible para extracción, aún no identificado/analizado en el repositorio; no es plantilla de salida. |

### F1 — Permisos y contratos base (parcial, validación manual pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F1-01 | Elegir mecanismo de autenticación, usuarios/roles y credenciales. | **Cerrada:** A1/B1/C1/D1 y parámetros confirmados; V6, contraseña/BCrypt y servicios de aprovisionamiento probados. Migración desde V4 conserva negocio y no crea cuentas por defecto. El responsable reportó crear dos cuentas por consola; su recorrido completo sigue pendiente en F1-03/sección 11. |
| F1-02 | Implementar autorización por operación en aplicación y protección HTTP. | **Cerrada para casos de uso existentes:** permisos vigentes en Clientes/Obras/Contratos/campos/valores; HTTP real verifica 401/403, revocación/desactivación, CSRF y rechazo de llamadas de aplicación no autorizadas. HTTPS exigido salvo opción explícita de desarrollo. Las operaciones futuras deberán incorporar su permiso y sus pruebas. |
| F1-03 | Acordar impacto en API existente, sesión/CSRF y autenticación del escritorio. | **Parcial:** 14 pruebas del cliente tras arreglo independiente, incluyendo cookie tardía y diagnóstico seguro de errores. Prueba cruzada con cliente HTTP real, Spring y PostgreSQL desechable correcta; no equivale a validación visual. Responsable usa WSL y reportó crear cuentas; no confirmó aún acceso correcto por interfaz. Recorrido completo de consola/JavaFX pendiente. |
| F1-04 | Definir y publicar DTO/rutas/errores autorizados. | **Cerrada para contratos base:** autenticación, catálogo y valores publicados y probados, con `ProblemDetail` coherente en filtros/API. No se cierran los contratos futuros de versiones/paquetes de F3–F5. |
| F1-05 | Verificar usuarios sin permiso, consulta, administración y ambos. | **Parcial:** matriz real PostgreSQL/HTTP correcta para campos/valores y rechazo de cambios no autorizados. Pendiente ampliar a versiones/paquetes cuando existan y completar recorrido manual; no equivale a CA-17/42 completos. |

### F2 — Esquema, catálogo y resolución (parcial)

| ID | Tarea | Estado / evidencia |
|---|---|---|
| F2-01 | Crear V5 para catálogo, conservando V1–V4. | **Cerrada:** tabla `reference_fields`, restricciones, unicidad y revisión optimista; Flyway e Hibernate validan. |
| F2-02 | Validar migración limpia y upgrade V4 con datos de negocio. | **Cerrada:** PostgreSQL 18; comparación de filas completas de cliente/obra/contrato, segunda migración sin cambios. |
| F2-03 | Implementar clave canónica y marcador exacto 50/54. | **Cerrada:** 33 pruebas de `ReferenceKey`, límites y sintaxis. No es aún analizador DOCX. |
| F2-04 | Lista aprobada de nombres; derivar tipo, origen y ruta sin evaluación libre. | **Cerrada:** dominio y DB rechazan origen/tipo/ruta ajenos a la lista. |
| F2-05 | Registrar campo y rechazar duplicados, incluidos concurrentes. | **Cerrada:** servicio, adaptador y restricción única; carrera real produce un éxito y un duplicado. |
| F2-06 | Listar con paginación y renombrar sin cambiar semántica. | **Cerrada:** validación de nombre, orden estable, conservación de definición y rechazo de entidad obsoleta mediante `@Version`. |
| F2-07 | Resolver valores de dos obras desde su cliente asociado, sin escrituras. | **Cerrada:** proyección de una consulta, recarga tras corrección y comparación de filas de negocio completas. |
| F2-08 | Distinguir presente/faltante/inválido de fallo de lectura. | **Cerrada para texto:** dominio y adaptador probados; fallo de origen se propaga como error, no faltante. Otros tipos pendientes F2-12. |
| F2-09 | Excluir obras y clientes eliminados de nuevas resoluciones. | **Cerrada:** filtros de ambos y conservación de contratos/filas probados. Archivos generados históricos se verificarán en F4/F7. |
| F2-10 | Proteger semántica de campos vinculados a versiones reales. | **Pendiente:** F4; sin edición semántica en este bloque. CA-05 no aceptado todavía. |
| F2-11 | Corregir fecha desde Obras manteniendo precondiciones. | **Diferida:** D-09 no incorpora cambios contractuales por convenio ni autoriza correcciones ordinarias de fechas. No ampliar `general-information`; CA-04 permanece pendiente. |
| F2-12 | Resolver fechas/importes y acordar formato común. | **Diferida:** D-01/03/10 mantienen solo nombres para el preliminar y los formatos propuestos para la futura ampliación. Sin cálculo/redondeo inventados ni atributos habilitados anticipadamente. |
| F2-13 | Verificar suite completa, empaquetado y diff. | **Cerrada para este bloque:** `test` y `clean verify` correctos, 114 pruebas y ambos módulos empaquetados; diff sin errores. |

### F3 — Análisis DOCX y revisión (pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F3-01 | Evaluar/aprobar biblioteca, integración y alcance de procesamiento. | **Bloqueada:** D-04 pasa a evaluación de Python; no hay biblioteca ni arquitectura elegidas. D-06 está diferida: cualquier prueba local debe acotarse aparte, no implementar almacenamiento definitivo por esa vía. |
| F3-02 | Analizar cuerpo, tablas, encabezados/pies y runs con ubicaciones. | **Pendiente:** F3-01 y fixtures. |
| F3-03 | Diagnosticar desconocidos, sintaxis, partes no soportadas y párrafos distintos. | **Pendiente:** no omitir marcadores por extracción plana. |
| F3-04 | Proponer obligatoriedad heredada y exigir clasificar nuevos. | **Pendiente:** configuración por versión/campo. |
| F3-05 | Confirmación explícita de estático y pruebas hostiles. | **Pendiente:** no confundir inválido con estático. |

### F4 — Archivos y versiones (objetivo completo; almacenamiento diferido)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F4-01 | Cerrar mapeo de versión, ARCHIVO y asociaciones. | **Pendiente:** identidad compuesta del Diccionario, marcador 54 ya acordado. |
| F4-02 | Almacenamiento, metadatos, hash y comprobación de integridad. | **Diferida:** D-06, retomar en otra iteración. No elegir rutas/volumen/límites/retención ahora ni declarar aceptado el flujo completo con archivos temporales. |
| F4-03 | Reanalizar y pedir nueva revisión cuando cambie archivo/catálogo. | **Pendiente:** CA-27. |
| F4-04 | Estados pendiente/completado/fallido, recuperación e idempotencia. | **Pendiente:** dueño de recuperación, insumos conservados y política de limpieza. |
| F4-05 | Numeración concurrente y última revisión válida seleccionable. | **Pendiente:** carrera y orden de completado comprobados en DB. |
| F4-06 | Inactivación, eliminación lógica e histórico inmutable. | **Pendiente:** preservar archivos y asociaciones, incluidos negocios eliminados. |
| F4-07 | Verificar fallos por frontera, pérdida de respuesta y reenvíos. | **Pendiente:** sin publicar estados incompletos como válidos. |

### F5 — Paquetes y aplicabilidad (pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F5-01 | Alta por cliente y unicidad exacta de nombres, sin operación/etapa. | **Pendiente:** D-02/D-07 resueltas en diseño; implementación depende de versiones/paquetes. No crear catálogo de etapas ni dar esta tarea por implementada. |
| F5-02 | Borrador persistente, reapertura y descarte seguro. | **Pendiente:** conservar captura y referencias sin borrar plantillas. |
| F5-03 | Confirmación y edición atómica con precondición de revisión. | **Pendiente:** no reasignar cliente tras confirmar; operación/etapa eliminada del modelo previsto. |
| F5-04 | Opciones aplicables por cliente, filtros y orden visual nombre/ID. | **Pendiente:** sin borradores, inactivas o eliminadas; no imponer secuencia ni momento de generación. |
| F5-05 | Selección única por documento y resolución vigente de sus campos. | **Bloqueada:** D-12 y versiones/paquetes. |
| F5-06 | Verificar cambios entre selección/resolución y contrato de procedencia. | **Pendiente:** historial real con el motor, no generaciones ficticias. |

### F6 — Escritorio (pendiente)

| ID | Tarea | Estado / dependencia |
|---|---|---|
| F6-01 | Navegación, clientes HTTP, DTO, vistas y diálogos pequeños. | **Pendiente:** API segura. |
| F6-02 | Red/archivos asíncronos y actualizaciones en hilo JavaFX. | **Pendiente:** no bloquear interfaz. |
| F6-03 | Registro de plantilla desde paquete y retorno sin pérdida. | **Pendiente:** persistencia y estado del editor. |
| F6-04 | Valores/diagnósticos/obligatoriedad/históricos y revisión actualizada. | **Pendiente:** flujos y permisos reales. |
| F6-05 | Cierre/reapertura de borradores, doble clic y validación Windows. | **Pendiente:** compilación no sustituye comprobación visual. |

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

## 5. Cambios de esta entrega

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

**Ningún CA se declara aceptado completamente todavía.** CA-01/02/03/26/40 tienen implementación parcial; ya hay API y seguridad para nombres, pero faltan interfaz documental, análisis o asociaciones reales según el criterio. CA-25 tiene únicamente semántica de texto y fallo de origen. CA-04/41, fechas/importes, diferidos explícitamente. CA-05/10/40 con referencias de versión reales esperan F4. CA-19/20 tienen evidencia HTTP real en el bloque de nombres; CA-17/42 tienen evidencia para campos y valores, no para versiones/paquetes aún inexistentes. F1 mantiene pendiente su recorrido manual. CA-14/21/22/23 en lo relativo a generaciones históricas esperan integración real con el motor.

Los demás criterios CA-06–CA-39 sin implementación en este bloque permanecen pendientes según las tareas y la matriz del plan. No se confunde la prueba de preservación de filas del catálogo con CA-35, que exige dos comprobaciones manuales separadas sobre versiones y paquetes.

El preliminar mantiene fechas/importes y D-06 diferidos. CA-13/16/31/32 y CD-005/006 se actualizan conforme a paquetes por cliente sin etapas, con comparación exacta de nombres y sin secuencia de generación. Se conserva su numeración y se mantienen los 42 criterios; no se declara aceptado ninguno por cambiar su redacción.

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

El responsable priorizó F0/F1: **no avanzar a implementar F3**. Las decisiones D-02 y D-07 ya están resueltas; el mínimo de nombres se mantiene. Próximos pasos: elegir otro documento y diseñar juntos la plantilla, estudiar la viabilidad/conveniencia de Python para D-04 y resolver D-12 con ese formato. El presupuesto para extracción es un insumo separado aún por recibir/identificar. D-06 se retoma en otra iteración; quedan pendientes las verificaciones manuales de F1. No instalar bibliotecas ni cambiar arquitectura por esta actualización.

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

## 12. Decisiones de F0 — estado vigente y propuestas pendientes

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

## 13. Cierre de la actualización documental

- Actualizados `spec.md`, `plan.md` y este seguimiento con el alcance aprobado; memoria resumida en `../../MEMORY.md`.
- Conservados los IDs de 53 tareas y los 42 criterios; 18 tareas siguen cerradas. Decisiones resueltas y trabajo diferido no se presentan como implementación terminada.
- Fuentes discrepantes identificadas y sin modificar: `docs/DiccionarioDatos_BRICA.docx` (`operacion_etapa`) y la precondición de `specs/002/spec.md`. ERS/DAS y guía de fixtures se mantienen como fuentes/evidencia previas, no se reescriben en esta tarea.
- Verificación de esta tarea: espacios, enlaces locales, trazabilidad, longitud de memoria y conservación del contenido de archivos ajenos. Los 144 tests corresponden a la ejecución previa, no se ejecutó Maven por editar documentación. Sin commit, push ni PR.
