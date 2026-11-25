# Lectura y revisión de fuentes

Este documento describe el flujo implementado en la rama actual; no constituye
aceptación de seguridad ni una guía de despliegue. La extracción de fuentes es
distinta de analizar una plantilla DOCX y no genera documentos.

## Flujo de usuario

1. En una obra vigente, adjunta el contrato o presupuesto y abre «Extraer y
   revisar». Se admiten PDF, XLS, XLSX y DOCX hasta 10 MiB por archivo.
2. Selecciona entre uno y cuatro adjuntos de esa obra y solicita el análisis.
   Se crea un trabajo persistente y se verifica el contenido original.
3. El servidor procesa copias mediante un lector aislado. Consulta el estado
   y, ante fallos, el diagnóstico y el historial de intentos.
4. En «Evidencia y campos», revisa fragmentos, archivo y ubicación. En DOCX la
   ubicación corresponde a párrafo o tabla, no a página de Word. En Excel se
   presentan hoja y coordenadas; para fórmulas se conservan fórmula y valor
   almacenado cuando existe, sin evaluarlas.
5. Mapea o captura los valores y guarda el borrador. Obra, cliente y cada
   presupuesto se confirman por separado; analizar no modifica esos datos.
   El cliente es compartido por las obras que lo utilizan.

Los estados técnicos son `QUEUED`, `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED`
y `CANCELLED`; son independientes del estado de revisión `DRAFT`, `CONFIRMED`
o `CANCELLED`. Una lectura fallida no equivale a una confirmación.

## Requisitos y fallos

El servidor exige `WORKS_READ` para consultar y `WORKS_WRITE` para crear o
revisar trabajos; consultar o confirmar datos de cliente requiere además sus
permisos correspondientes. Los permisos se comprueban en el servidor.

En Linux el lector requiere un cgroup v2 delegado y los componentes de
aislamiento configurados. La configuración Compose predeterminada no delega
ese cgroup ni define `BRICA_EXTRACTION_CGROUP_ROOT`: levantar el servidor con
Compose, por sí solo, **no habilita** la lectura aislada. No desactives el
aislamiento ni concedas privilegios amplios para sortear este requisito.
Consulta `specs/002/plan.md` y revisa el entorno con el responsable antes de
operar la extracción.

En Windows el lanzador de lectura exige el servidor JAR empaquetado; ejecutar
`spring-boot:run` usa un classpath de desarrollo que el lanzador rechaza.
El diagnóstico del trabajo indica esta situación. El soporte requiere también
que el mecanismo de aislamiento de Windows esté disponible y verificado.

Ante una fuente inválida o un aislamiento no disponible, consulta el diagnóstico
del trabajo; conserva el borrador, sustituye la fuente o captura manualmente.
No se aplica ningún dato automáticamente. Reanalizar crea un trabajo nuevo
vinculado al anterior; cancelar lo pendiente no revierte bloques confirmados.

## Límites y verificación

Los límites implementados incluyen hasta cuatro fuentes por trabajo, 50 hojas,
20 000 filas, 200 000 celdas y 256 columnas; también se verifican límites y
contenido activo de los contenedores Office. Los valores completos y las
limitaciones de OCR y aislamiento se documentan en `specs/002/plan.md`.

Las pruebas de lectores usan archivos sintéticos. Las pruebas que llaman al
lector directamente no demuestran aislamiento del sistema operativo; las
pruebas de integración con PostgreSQL tampoco sustituyen el recorrido visual
JavaFX ni la verificación del entorno aislado de destino. No uses documentos
reales sin autorización individual.
