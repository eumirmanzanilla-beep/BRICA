# Plan de implementación — Spec NNN

**Funcionalidad:** título breve
**Fecha:** AAAA-MM-DD
**Estado:** Propuesto | Aprobado | En curso | Bloqueado | Cerrado
**Spec:** `spec.md` · **Seguimiento:** `review.md`
**Autorización / límites:** quién aprobó qué y qué acciones siguen requiriendo autorización.

## 1. Resultado, alcance y fuentes

- Resultado a implementar:
- Incluido / excluido:
- Especificaciones, ADR, código y configuración vigentes consultados:
- Rama/estado Git observado (incluidos cambios locales que se preservan):

## 2. Diagnóstico del repositorio

| Área/ruta | Situación comprobada | Impacto en el plan |
|---|---|---|
| ... | ... | ... |

Señala explícitamente discrepancias con documentos históricos y archivos/consumidores que habrá que revisar.

## 3. Decisiones técnicas y de negocio

| ID | Decisión / alternativa | Pros y riesgos | Estado / aprobador / fecha |
|---|---|---|---|
| D-001 | Propuesta; no implementar hasta aprobación | ... | Propuesto |

No marques una propuesta como aprobada por estar escrita en el plan.

## 4. Diseño y cambios previstos

- Flujo entre módulos/capas:
- Contratos API/UI:
- Entidades, consultas y migraciones Flyway:
- Archivos/rutas previstos (propuesta, confirmar al implementar):
- Seguridad, autorización, privacidad y límites:
- Compatibilidad, idempotencia, concurrencia, rollback/recuperación:

## 5. Fases y criterios de salida

| Fase | Entrega | Archivos/áreas | Pruebas/evidencia | Dependencias / aprobación |
|---|---|---|---|---|
| F0 | Diagnóstico y decisiones | ... | ... | ... |
| F1 | ... | ... | ... | ... |

Define fases pequeñas. No inicies una fase bloqueada por decisión o autorización pendiente.

## 6. Verificación

| Comprobación | Comando/recorrido | Entorno | Criterio de éxito |
|---|---|---|---|
| Pruebas | `./mvnw test` | ... | ... |
| Integración/empaquetado | `./mvnw verify` | Docker/Python si aplica | ... |
| Diff | `git diff --check` | raíz | sin errores |
| Manual | ... | Windows/Word/rol real, si aplica | ... |

## 7. Riesgos, bloqueos y próximos pasos

- Riesgos y mitigación:
- Datos/instancias que no deben tocarse:
- Bloqueos y decisiones requeridas:
- Próximo paso sujeto a autorización:
