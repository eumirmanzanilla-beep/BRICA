---
description: Planifica, implementa y verifica una funcionalidad de BRICA
agent: build
---

Funcionalidad solicitada: $ARGUMENTS

1. Lee AGENTS.md y las reglas aplicables. Consulta MEMORY.md si existe. Verifica contra el código y las fuentes vigentes cualquier dato de memoria que afecte la implementación.
2. Revisa el estado de Git, el código relacionado y la especificación, issue o criterios de aceptación indicados. Si falta el objetivo o hay una regla de negocio sin confirmar que cambia el comportamiento, pide aclaración.
3. Define el alcance, los criterios de aceptación, los módulos y archivos previstos, el cambio propuesto y cómo se verificará. En una tarea no trivial, presenta el plan y termina el turno antes de editar. Continúa cuando apruebe ese plan. Si el alcance cambia de forma sustancial, vuelve a presentar el plan actualizado.
4. Implementa el cambio aprobado respetando las convenciones existentes y el trabajo local ajeno a la tarea. Si cambia un contrato entre módulos, revisa sus consumidores. Si mueves clases JavaFX, comprueba paquetes, imports, controladores y referencias FXML.
5. Ejecuta en WSL las comprobaciones Maven pertinentes según AGENTS.md. Si cambia la interfaz, indica los pasos para verificarla en Windows. Distingue lo que se ejecutó, su resultado y lo que queda pendiente.
6. Resume el comportamiento implementado, los archivos principales y la evidencia de validación. Haz commits, push o abre un PR solo si te lo pido.