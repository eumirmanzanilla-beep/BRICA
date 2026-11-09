# Constitución de BRICA

**Versión** 1.0.0 · **Estado** Propuesta para adopción

BRICA centraliza la gestión de obras y su documentación para la operación interna de una constructora. Esta constitución establece los principios que deben respetar las especificaciones, el código y las contribuciones realizadas con asistencia de IA.

## I. La obra es el eje del negocio

Cada funcionalidad debe resolver una necesidad identificable de la operación. La información de la obra debe poder reutilizarse en los procesos que la consumen, como contratos, presupuestos, estimaciones y documentos de cierre.

Define el comportamiento esperado y sus criterios de aceptación antes de implementar. Para campos, estados, relaciones y reglas de negocio, consulta el ERS, el Diccionario de Datos y las decisiones vigentes. Si una regla queda abierta o dos fuentes discrepan, registra la duda y confirma el comportamiento con el responsable del proyecto antes de codificar esa decisión.

## II. El servidor concentra las reglas y protege los datos

BRICA mantiene un cliente JavaFX y un servidor Spring Boot organizado por funcionalidad. El cliente consume la API REST. El servidor decide la validez de los datos, aplica las reglas de negocio y comprueba la autorización cuando corresponda. Las validaciones de interfaz pueden orientar al usuario y deben corresponder al comportamiento del servidor.

En el backend, la API invoca los casos de uso de aplicación, el dominio concentra las reglas del negocio y la infraestructura implementa los puertos de persistencia e integración. Respeta los límites existentes al añadir una funcionalidad y justifica cualquier cambio arquitectónico o dependencia nueva antes de incorporarlo.

## III. Los cambios preservan la integridad y el historial

Usa Flyway para evolucionar el esquema de PostgreSQL. Conserva las migraciones ya aplicadas y crea una nueva para cada cambio necesario. Considera tanto una base vacía como una base actualizada con datos previos cuando la migración pueda afectarlos.

Preserva el historial mediante borrado lógico en las entidades donde así esté definido. Mantén los controles de concurrencia existentes, como las precondiciones de actualización de Obras. Cuando una operación abarque varias escrituras, define qué debe suceder si falla a mitad del proceso o se reintenta.

Mantén las credenciales fuera del repositorio. Antes de usar una herramienta de inspección de datos, confirma la instancia de desarrollo y utiliza acceso de solo lectura cuando la tarea sea de consulta.

## IV. La generación documental conserva su procedencia

Al implementar el flujo documental, vincula cada generación con su obra, su paquete y la versión concreta de cada plantilla utilizada. Conserva esa versión al reanudar un borrador. Define el origen y el formato de los campos de referencia y confirma el tratamiento de datos ausentes antes de emitir documentos definitivos.

Mantén los archivos físicos en el almacenamiento del servidor y los metadatos de archivo en PostgreSQL, según el diseño aprobado. Verifica el resultado con una plantilla representativa y datos de prueba. Comprueba los valores insertados, los marcadores pendientes y la presentación del documento.

## V. Cada cambio se revisa con evidencia proporcional

Mantén el cambio acotado al propósito aprobado. En un refactor, preserva el comportamiento observable y revisa los consumidores y recursos afectados. Cuando se muevan controladores JavaFX, verifica sus referencias FXML y comprueba la apertura de la pantalla.

Ejecuta las verificaciones de Maven que correspondan al cambio y añade o actualiza pruebas cuando haya un comportamiento nuevo o una regresión concreta que cubrir. Usa WSL para el desarrollo y las comprobaciones de código, y Windows para la validación visual de JavaFX. Registra qué se ejecutó, qué resultado dio y qué sigue pendiente. La compilación y la apertura de una pantalla son comprobaciones distintas.

## VI. El responsable del proyecto dirige los cambios asistidos por IA

En tareas no triviales, revisa el estado del repositorio, presenta un plan y espera la aprobación antes de editar. Si cambia el alcance aprobado, confirma el nuevo alcance antes de seguir. Preserva el trabajo local ajeno a la tarea.

Los comandos y las skills aplican estas reglas dentro de su alcance. Usa /feature para planear e implementar y /review para evaluar el cambio en modo de solo lectura. El uso de herramientas MCP debe limitarse al propósito y acceso autorizados. Ejecuta commits, push, apertura de PR u operaciones destructivas únicamente cuando el responsable del proyecto las haya solicitado.

## Gobierno y enmiendas

- Esta constitución fija los principios. AGENTS.md recoge las instrucciones operativas y los comandos. El ERS y el Diccionario de Datos detallan el comportamiento y los datos. El DAS y los ADR registran el diseño y sus decisiones.
- Cuando una especificación, un plan o un cambio contradiga un principio, señala el conflicto antes de implementarlo. Una excepción requiere aprobación expresa, alcance y motivo registrados en la especificación, el issue o un ADR.
- El responsable del proyecto aprueba la adopción y las enmiendas. Registra la versión, la fecha y el motivo de cada modificación. Revisa AGENTS.md, comandos y skills afectados al incorporar una enmienda.
