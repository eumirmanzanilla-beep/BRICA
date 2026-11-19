# F0 — Alineación de propuestas DP-01–DP-38

**Estado:** propuestas aportadas por el responsable, en evaluación frente a las decisiones P01–P33. Las propuestas nuevas no se convierten por sí solas en criterios implementados ni en autorización de inspeccionar archivos reales. Las decisiones P ya aprobadas se conservan salvo indicación explícita posterior.

## Coincidencias y aplicación prevista

| DP | Tratamiento en SPEC002 | Trazabilidad |
|---|---|---|
| 01–02 | Separar extracción, revisión/confirmación y persistencia. Analizar no modifica obra/cliente; generación queda en otra SPEC. | P01, P20, P22; F6–F8 |
| 03–04 | Mantener PDF digital/escaneado/mixto con OCR local y XLS/XLSX/DOCX fuente. CSV/XLSM fuera. Selección de tecnología y límites aún en F0. | P02, P08–P09; F0, F4–F5 |
| 05–06 | Aislamiento efectivo y evaluación POI; no ejecutar macros, vínculos ni contenido activo. La autorización de dependencias está condicionada a licencia, versiones y prueba aislada. | P09, P29; F0/F3/F5 |
| 07–09 | Perfiles versionados opcionales que sugieren estructura; lectura genérica y mapeo manual siguen disponibles. Una coincidencia no confirma datos; no exige precios. | P03, P05, P10; F5–F6/F9 |
| 10 | Conservar P12-B: usuario agrupa explícitamente partes, resúmenes y alternativas; sugerencias no fusionan automáticamente. | P12; F5/F8/F9 |
| 11–15 | Comparación de encabezados con normalización solo para sugerir; conservar originales. Clasificación sugerida concepto/partida/subtotal/impuesto/total/nota/dudosa, sin descartar desconocidos. Mantener condiciones comerciales separadas y revisión de continuaciones. | P05, P10, P15; F5–F6/F9 |
| 16–21 | Conceptos con identidad y orden, códigos repetidos, cantidades/unidades con su texto original, conceptos sueltos o en primer nivel de partidas. Precio/importe ausente no significa cero. Subpartidas no se asumen. | P12–P15, P18; F1/F5/F8 |
| 22–23, 25–26 | Conservar fórmula y valor almacenado, sin evaluarla; cálculos decimales separados y sin sobrescribir lo declarado. No suponer IVA. Importe en letras es una necesidad futura, no una función autorizada en esta fase. | P11/P18; F5–F6/F8 |
| 24 | Conservar precisión; escala y momento de redondeo de comprobación siguen pendientes de criterios con ejemplos. No confundir cálculo con presentación `AmountPresentation`. | P18; ficha F0 |
| 27 | Propuesta: discrepancia interpretable aceptable con justificación; valores inválidos bloquean. Pendiente concretar campos/casos que impiden una revisión coherente y cómo registrar justificación. | P18/P21; F0/F8 |
| 28–29 | Revisar datos, partidas, condiciones y advertencias; conservar valor extraído, corrección, valor confirmado y evidencia mínima. No confundir clasificación de candidatos con confirmación. | P01/P15/P22; F6–F9 |
| 30 | Modelo híbrido P13-C con identidad/revisiones y relaciones explícitas, conceptos tipados y metadatos acotados. Nuevas migraciones, no edición de V14. | P13/P31; F1/F8 |
| 31 | Propuesta visible: borrador/confirmado/cancelado. Separar estado técnico del trabajo y resultado de cada confirmación; no perder el estado parcialmente confirmado. Detalle de transiciones pendiente. | P22/P25/P26; F0/F3/F7–F9 |
| 32 | Conservar P14-B: revisiones confirmadas inmutables y elección explícita de principal. No vincular una generación en esta SPEC. | P14/P32; F8 |
| 33–36 | Pendientes para futura generación: bloques, validación de salida, fallos parciales, vista previa y conversión. No modificar sintaxis de plantillas ni habilitar Generar. | P32; fuera del alcance de SPEC002 |
| 37 | Operación de extracción/importación durable, publicación verificable, confirmación y recuperación idempotente; no confirmar una revisión con fuente inaccesible. La recuperación de resultados generados es futura. | P24–P25/P30–P31; F3/F7/F11 |
| 38 | Mantener RF-24 como vínculo provisional de exportación futura y registrar RF-28 como discrepancia del ERS; no inventar correspondencia. | P32; documentación futura |

## Corpus y autorización — DP-08

- Se autorizó previamente únicamente el uso de **datos sintéticos**. La mención de los tres archivos como referencia **no concede autorización de inspección**.
- No se ha identificado de forma inequívoca la ruta de la cotización AR-SO ni del catálogo sin precios dentro del estado local. Antes de analizarlos, pedir rutas exactas y autorización de lectura por archivo; mantener originales sin modificar y resultados sensibles fuera de Git.
- Los fixtures automatizados serán sintéticos/anónimos. Reproducir una estructura no autoriza copiar contenido real o publicar archivos proporcionados. No asumir tres perfiles: evaluar si cada perfil mejora un caso frente a mapeo genérico.

## Pendientes concretos antes de cerrar F0

**Actualización de esta lista:** el responsable delegó las decisiones de diseño y aprobó ejecutar el bloque F0. La [ficha técnica v1](ficha-tecnica-f0.md) adopta políticas concretas para redondeo, justificación, borradores, perfiles/mapeo y retención. Las filas anteriores registran su estado histórico al alinear las propuestas; no se sustituyen por afirmaciones de implementación. La integración permanece bloqueada por aislamiento del destino y evaluación incompleta; las muestras reales siguen sin autorización.

1. Fijar límites medibles de OCR y lectores, selección/versiones/licencias/vulnerabilidades, aislamiento y comportamiento ante fallos. La prueba aislada mínima no demuestra OCR ni seguridad.
2. Definir escalas y momento de redondeo de cálculos de control mediante ejemplos; no modificar valores declarados.
3. Precisar qué discrepancias admiten confirmación con justificación y cuáles bloquean por incoherencia, incluidos importes y revisiones de presupuesto.
4. Definir transiciones de borrador/cancelación y representación del estado parcial entre bloques confirmados. No confundir con estados técnicos de trabajo.
5. Acordar alcance de mapeo manual para PDF/DOCX: selección de región/tabla y asociación de columnas cuando existan, sin asumir un visor o biblioteca aún no aprobados.
6. Identificar las tres muestras reales y pedir autorización específica por archivo antes de la validación manual. Ninguna aceptación de salida se declara cumplida sin evidencia.
