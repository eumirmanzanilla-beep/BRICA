---
description: Revisa cambios de BRICA y reporta defectos verificables
agent: plan
---

Alcance solicitado: $ARGUMENTS

Haz una revisión de solo lectura. Lee AGENTS.md y consulta MEMORY.md si
existe. Usa la especificación o el issue indicado para validar el
comportamiento esperado.

1. Identifica el cambio y su base antes de revisar:
   - Sin alcance explícito, revisa los cambios locales preparados y sin
     preparar, junto con los archivos nuevos pertinentes.
   - Si solo se indicó un módulo, aplica ese mismo alcance local al módulo
     y a los contratos relacionados.
   - Para un commit, compara con su padre. Si es un merge y el padre
     pertinente resulta ambiguo, pide que se precise.
   - Para una rama o PR, compara desde el ancestro común con la rama base
     indicada o registrada en el PR. Si falta la base o la referencia,
     señala qué dato necesitas para revisar ese cambio.

2. Busca defectos introducidos por el cambio, regresiones y criterios de
   aceptación incumplidos. Si cambia una API, revisa ambos lados del
   contrato. Si cambia JavaFX, comprueba las referencias de fx:controller,
   fx:id y los manejadores afectados.

3. Presenta primero los hallazgos por gravedad. Para cada uno, indica
   archivo y línea, condición que lo activa, efecto observable y una
   corrección sugerida. Separa los defectos de las dudas pendientes.

4. Indica qué comparaste y qué quedó sin verificar. Si no hay cambios en
   el alcance, indícalo. Si hay cambios pero no detectas defectos, señala
   que no encontraste hallazgos en la revisión realizada.   