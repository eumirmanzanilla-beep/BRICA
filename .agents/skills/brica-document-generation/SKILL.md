---
name: brica-document-generation
description: "Implementa y verifica la generación documental dentro de BRICA. Úsala al cambiar paquetes documentales, versiones de plantilla, campos de referencia, previsualización, reanudación de borradores o exportación de documentos de obra."
---

## Fijar el alcance

- Lee AGENTS.md y consulta el ERS, el Diccionario de Datos y el diagrama de secuencia pertinente. Abre una plantilla representativa y verifica qué parte del flujo ya existe en el código.
- Identifica la obra, el paquete aplicable y la versión de cada plantilla. Distingue la plantilla reutilizable, su versión concreta, la ejecución y el documento resultante. Conserva la versión fijada para una generación existente.
- Separa las reglas del negocio, la selección de campos y el renderizado. Reutiliza el motor y el almacenamiento del proyecto. Si hace falta una biblioteca nueva, presenta la decisión conforme a AGENTS.md.

## Implementar y comprobar

1. Para cada marcador afectado, identifica su campo de referencia, fuente, formato y tratamiento cuando falte el dato. Si varias fuentes pueden aportar valores distintos, confirma la precedencia en el requisito antes de implementarla.
2. Revisa qué cambia para la previsualización, el borrador y la salida definitiva según el caso de uso. Si se reanuda una generación, verifica qué datos deben conservarse y que se siga usando su versión fijada de plantilla.
3. Mantén el archivo y sus metadatos vinculados según el modelo vigente. Si la operación puede dejar resultados parciales o repetirse tras un fallo, define qué se conserva y cómo se retoma conforme al flujo existente.
4. Genera una salida con datos de prueba. Contrasta los campos con su fuente, comprueba marcadores sin resolver y abre o renderiza el archivo para revisar tablas, encabezados, saltos y valores extensos que afecte el cambio.
5. Si se modifica la selección de versiones o el manejo de datos faltantes, verifica el escenario correspondiente con una plantilla representativa. Registra qué plantilla y versión se usaron y qué salidas se comprobaron.
