# Flujo SDD de BRICA

Las especificaciones hacen que cada cambio funcional tenga un resultado comprobable, un plan revisable y un registro de evidencia. En cada carpeta de funcionalidad se mantienen tres documentos relacionados:

```text
specs/NNN-nombre-corto/
├── spec.md    # necesidad, comportamiento y criterios de aceptación
├── plan.md    # diagnóstico, decisiones y secuencia de implementación
└── review.md  # historial factual de cambios, verificaciones y pendientes
```

Las plantillas copiables están en [`templates/`](templates/). Úsalas como punto de partida y ajusta secciones que no apliquen; no llenes campos con suposiciones para completar el formato.

## Flujo de trabajo

### 1. Enmarcar el problema y crear la spec

1. Vincula la propuesta a un issue o petición aprobada. Busca una spec existente antes de crear otra.
2. Revisa `AGENTS.md`, la constitución, documentación de negocio aplicable y el estado real del código/configuración. Los documentos iniciales expresan intención, no existencia de funcionalidad.
3. Crea una carpeta `NNN-nombre-corto` con número no usado y nombre breve. No alteres una spec cerrada o en curso sin conservar su historia.
4. Completa `spec.md` con problema, resultado, alcance, reglas y criterios verificables. Identifica las preguntas abiertas; solicita decisión cuando impacten negocio, persistencia, seguridad o arquitectura.

### 2. Preparar y aprobar el plan

1. Revisa rama, estado Git, módulos, pruebas, migraciones y configuración efectiva. Registra discrepancias entre requisitos, documentación y código.
2. Completa `plan.md`: alternativas/decisiones, límites, rutas previstas, fases, pruebas, migraciones y validación manual. Etiqueta cada decisión como propuesta, aprobada, rechazada o pendiente, e indica quién la confirmó y cuándo.
3. Presenta el plan y espera autorización antes de cambios no triviales. El plan no autoriza por sí mismo añadir dependencias, cambiar arquitectura, modificar datos persistentes, hacer commits/push/PR ni desplegar.

### 3. Implementar por bloques y mantener trazabilidad

Implementa unidades acotadas que puedan verificarse. Contrasta cada una con los criterios de la spec y los pasos del plan. Si cambia el alcance aprobado, pausa y confirma el plan actualizado antes de continuar.

Registra en `review.md` los hechos conforme ocurren: autorización, fecha, rutas creadas/modificadas, decisiones, comandos exactos, resultados, fallos y pendientes. Un fallo intermedio no se borra al registrar su corrección.

### 4. Verificar y revisar

- Distingue pruebas unitarias, integración, empaquetado, análisis estático, comprobaciones FXML, pruebas visuales y aceptación de negocio.
- Registra el resultado real; no afirmes que una tarea está cerrada por tener código o porque compile.
- Marca aceptación manual como pendiente hasta que haya evidencia de la persona/entorno requerido.
- Revisa migraciones sobre instalación nueva y actualización cuando aplique; nunca edites una migración aplicada.
- Revisa el estado/diff para no incluir cambios locales ajenos, secretos ni datos/documentos reales.

### 5. Cierre o continuación

Al cerrar un bloque, resume qué criterios tienen evidencia, qué criterios se aceptaron y qué queda abierto. Mantén `spec.md`, `plan.md` y `review.md` enlazados y coherentes; no reescribas historia para que parezca que una decisión o verificación existía antes de su fecha.

## Convenciones de estado

Usa términos explícitos y consistentes:

- **Propuesto:** alternativa aún no autorizada.
- **Aprobado:** decisión o alcance confirmado por responsable identificado.
- **En curso:** trabajo iniciado, no terminado.
- **Implementado:** código presente; no implica que pruebas o aceptación estén completas.
- **Verificado técnicamente:** comandos/pruebas indicados pasaron en el entorno descrito.
- **Aceptado:** criterio validado por el actor/entorno requerido, con evidencia registrada.
- **Pendiente / bloqueado / diferido:** sin evidencia final, dependiente de otra decisión o fuera del alcance actual, respectivamente.

No uses porcentajes ni contadores de tareas sin explicar qué significan y de dónde salen. Los códigos de requisito y criterio (`REQ-001`, `CA-001`, etc.) deben ser estables dentro de la spec.

## Plantillas

- [Plantilla de `spec.md`](templates/spec-template.md)
- [Plantilla de `plan.md`](templates/plan-template.md)
- [Plantilla de `review.md`](templates/review-template.md)

Las plantillas orientan la escritura; las decisiones reales de BRICA, las reglas de seguridad de [`AGENTS.md`](../AGENTS.md) y las autorizaciones del responsable prevalecen sobre ejemplos o texto marcador.
