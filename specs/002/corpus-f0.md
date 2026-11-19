# F0 — Corpus sintético propuesto para SPEC002

**Estado actualizado:** corpus parcial generado y leído mediante [experiments/](experiments/README.md); expectativas independientes en `experiments/expected.json` y evidencia en [Review.md](Review.md). Los datos son ficticios. No se utiliza contenido de documentos reales. La tabla conserva el diseño de aceptación completo, no afirma que cada caso ya esté cubierto.

La aprobación para usar ejemplos construidos permite preparar estos casos; los criterios numéricos de precisión y los límites efectivos se acordarán antes de la aceptación. El contenido esperado se conservará separado del resultado del extractor; no se construirá automáticamente a partir de lo que el lector devuelva.

| ID | Archivo construido propuesto | Anotación esperada | Fallo o límite que cubre |
|---|---|---|---|
| S01 | PDF digital de dos páginas con texto y tabla | Página 1: denominación `Proyecto de Prueba Ñ`; página 2: importe `1234.56` y fila de concepto `0012`, `pieza`, cantidad `2`, precio `5.25`, importe `10.50`. | Etiqueta, valor, coordenadas aproximadas y orden de tabla; no convertir `0012` en número. |
| S02 | PDF de página escaneada, texto ficticio impreso | Página 1: `Contrato PRUEBA_01` y fecha `16/07/2025`; imagen y transcripción anotadas por separado. | OCR y localización de página; reconocer como propuesta, nunca confirmar automáticamente. |
| S03 | PDF mixto con encabezado digital e imagen de tabla en la misma página | Página 1 contiene texto digital y una fila en imagen, ambos anotados con procedencia. | No omitir OCR por detectar solo encabezado textual; no duplicar resultados al combinar. |
| S04 | PDF cifrado y PDF truncado construidos | Ningún dato confirmado; diagnóstico distinguible de fallo técnico. | Rechazo seguro; cancelar o sustituir la fuente. |
| S05 | XLSX con dos hojas de conceptos y una de resumen | Agrupar explícitamente hojas `ParteA`/`ParteB`; `Resumen` no aporta conceptos. Mantener orden y `0012`. | Selección de hojas, partidas, celdas fusionadas, importes y desduplicación controlada por usuario. |
| S06 | XLSX con alternativas y fórmulas | Dos hojas independientes; fórmula registrada como evidencia y valor almacenado por separado cuando exista. | No evaluar fórmulas ni convertir un resultado ausente en cero. El generador del fixture debe documentar cómo produjo una caché verificable. |
| S07 | XLS binario de prueba | Hoja con encabezados, clave textual `0007`, importe y celda numérica con formato visible. | Soporte XLS real, no un XLSX renombrado; preservar evidencia de tipo y formato. |
| S08 | DOCX fuente sin marcadores, con párrafo y tabla | Denominación literal con espacios/acentos y fila identificada por parte/tabla/fila/celda. | Lectura de fuente independiente del analizador de plantillas; no inventar página Word. |
| S09 | Documento con dos importes y fuentes contradictorias | Conservar ambas propuestas y su evidencia; indicar qué regla de precedencia aprobada aplica, sin confirmar ninguna. | No confundir anticipo con monto contratado; conflicto no se resuelve por orden de llegada. |
| S10 | Archivos construidos con fechas/decimales ambiguos | `03/04/2026` y `1,234` conservados literalmente; sin interpretación hasta existir convención aprobada por fuente. | No adivinar mes/día ni separador de miles/decimales. |

## Controles de preparación

1. Crear ejemplos en una ubicación temporal privada o como fixtures sintéticos del proyecto solo después de elegir herramientas adecuadas. No guardar allí contraseñas, datos de clientes o fragmentos de archivos de negocio.
2. Registrar herramienta/versión, pasos de creación, tamaños y SHA-256 en una ejecución reproducible; no descargar documentos externos para usarlos como corpus sin autorización y revisión de licencia.
3. Medir por separado extracción de texto/OCR, asociación al campo y confirmación humana. Reportar aciertos, omisiones y falsos positivos por campo/fila; no inventar una cifra de aceptación global.
4. Complementar con pruebas de límite de memoria/tiempo, macros y vínculos externos, así como cambios concurrentes, pertenencia y permisos antes de cualquier despliegue.
5. La aceptación manual con muestras reales permanece pendiente de autorización explícita, identificación inequívoca de archivos y expectativa anotada por el responsable. No modificar originales ni versionarlos.

## Cobertura parcial ejecutada

- S01: dos páginas, nombre/texto y coordenadas de glifos; no se ha implementado reconstrucción de tabla ni mapeo a campos.
- S02/S03: un PDF rasterizado y otro con encabezado digital más imagen; OCR local `spa+eng` verifica cuatro tokens por página y cajas. No hay algoritmo automático de segmentación/desduplicación: el experimento selecciona esas páginas conocidas explícitamente.
- S04: PDF cifrado y truncado con errores distinguibles; guardias adicionales de 41 páginas y página sobredimensionada antes de rasterizar.
- S05/S06: un XLSX combinado con cuatro hojas, fila/hoja oculta y fusión; cachés `10.50`, ausente, `99.00` deliberadamente obsoleta y cero explícito. Se inyectan directamente en `<v>` sin evaluador. No representa aún la agrupación por usuario o mapeo.
- S07/S08: XLS binario con código literal y formato decimal; DOCX con párrafo/tabla sin marcadores y precio vacío. Faltan casos extendidos de fechas, encabezados/pies y continuaciones.
- ZIP muy compresible, timeout con hijo, salida desmedida y límites por proceso: pruebas adicionales de infraestructura, no equivalentes a toda la aceptación hostil.
- S09/S10 siguen sin integración al mapeo; los tres ejemplos decimales ejecutables de política no sustituyen esos casos.
