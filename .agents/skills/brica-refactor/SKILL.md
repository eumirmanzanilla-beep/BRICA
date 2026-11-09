---
name: brica-refactor
description: "Ejecuta refactors que preservan el comportamiento en BRICA. Úsala al mover clases, dividir controladores JavaFX, extraer diálogos o reorganizar Clientes, Obras u otros módulos."
---

## Delimitar el cambio

- Lee AGENTS.md y el plan aprobado, si existe. Revisa la rama, el estado de Git y el código actual. Verifica las rutas en el checkout antes de usar notas de un refactor anterior.
- Identifica el flujo que debe conservarse y sus consumidores. En Clientes y Obras, revisa las operaciones, la paginación, los estados de la pantalla y el manejo de errores que correspondan. Si existe control optimista mediante ETag e If-Match, conserva el tratamiento del conflicto.
- Si la tarea requiere cambiar el comportamiento, identifica ese cambio como un requisito separado y aplica el flujo de aprobación de AGENTS.md.

## Ejecutar en pasos comprobables

1. Localiza la clase y sus usos en Java, pruebas, FXML y carga de recursos. Distingue código compartido de código propio de una funcionalidad.
2. Extrae o mueve una responsabilidad a la vez. Ajusta paquetes, imports y consumidores en el mismo paso. Al mover un controlador, comprueba fx:controller, fx:id, manejadores y rutas de FXMLLoader. Si existe module-info.java, comprueba las declaraciones opens y exports afectadas.
3. Mantén el flujo asíncrono y las actualizaciones de interfaz en el hilo JavaFX. Conserva el contrato REST, las validaciones y los estados de carga o error que ya espera el usuario.
4. Revisa el diff y el estado de Git. Confirma que cada clase nueva forma parte del cambio y que las rutas antiguas ya no se utilizan. Ejecuta las comprobaciones Maven pertinentes del módulo afectado.
5. Si cambió la presentación, deja un caso concreto para abrir y recorrer la pantalla en Windows. Registra por separado la compilación, las pruebas automatizadas y la prueba de apertura en JavaFX.
