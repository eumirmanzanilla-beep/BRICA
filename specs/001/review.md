# Seguimiento de implementación — Spec 001

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
**Inicio:** 2026-11-09 · **Última actualización:** 2026-11-20
**Rama observada:** `actualization`
**Spec:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

> Estado factual: implementación verificada técnicamente; aceptación manual pendiente.

## 1. Estado general

**Estado:** verificado técnicamente según evidencia histórica; aceptación funcional pendiente. La evidencia no se ha repetido para el árbol actual.

| Área | Estado | Pendiente |
|---|---|---|
| Catálogo, permisos y resolución | Verificado técnicamente | Aceptación manual de permisos. |
| Análisis DOCX, versiones y archivos | Verificado con casos de prueba | Despliegue, respaldo y retención definitivos. |
| Paquetes y selección | Implementado y verificado | Historial de generaciones, fuera de alcance. |
| Escritorio | Probado en Linux/WSL | Revisión visual en Windows/Word. |
| CA-35, CA-43–CA-48 | Implementados; aceptación pendiente | Recorrido y evidencia del responsable. |

## 2. Autorizaciones y decisiones

- **D-02:** sin operación/etapa; paquetes asociados a cliente u obra.
- **D-04:** analizador DOCX con `docxtpl` en proceso local del servidor.
- **D-06:** almacenamiento probado con archivos construidos; no define despliegue final.
- **D-12:** el tipo documental identifica alternativas.
- **2026-11-15:** aprobadas las reglas vigentes de nombres, fechas, ámbitos, versiones y adjuntos; ver [spec.md](spec.md).
- El registro disponible no identifica al aprobador de cada decisión; no se infiere.

## 3. Bitácora cronológica

### 2026-11-20 — Adaptación a plantillas

- **Autorización:** reescritura documental aprobada por el usuario; sin cambios funcionales autorizados.
- **Rama y cambios:** `actualization`; reorganización de `spec.md`, `plan.md` y `review.md`, preservando el resto del trabajo local.
- **Alcance:** fases detalladas como reconstrucción, no como nuevos hitos históricos. No se ejecutaron pruebas Maven ni aceptación manual para esta edición.
- **Pendiente:** recorrido Windows/Word y comprobación de permisos con datos de prueba.

### 2026-11-20 — Síntesis documental

- Se acortaron `spec.md`, `plan.md` y `review.md`; se mantuvieron los criterios CA/CD y las reglas vigentes.
- Se eliminó `manual-test.md` por solicitud. Los escenarios esenciales quedaron resumidos en la condición de aceptación de la spec.
- No se modificó código ni se ejecutaron pruebas Maven. `git diff --check` pasó y se revisaron los enlaces.

### Hitos de implementación

- **2026-11-15:** `verify` pasó con 183 pruebas de servidor y 22 de cliente; smoke JavaFX y pruebas de ciclo de vida también pasaron en Linux/WSL. Aceptación Windows/Word pendiente.
- **2026-11-13:** V11–V13 y flujos de obra/adjuntos; `verify` pasó con 178 pruebas de servidor y 21 de cliente.
- **2026-11-10:** analizador DOCX, versiones recuperables y paquetes; `clean verify` pasó con 150 pruebas de servidor y 18 de cliente.
- **2026-11-09:** catálogo y seguridad iniciales. Se corrigieron fallos detectados en la primera ejecución de pruebas antes de repetirlas.

## 4. Verificación acumulada

- Último `./mvnw verify` registrado: **2026-11-15**, 183 servidor + 22 cliente, sin fallos, errores ni pruebas omitidas.
- `git diff --check`: registrado como correcto en la síntesis documental anterior del 2026-11-20.
- No se repitió Maven en esta actualización. Los resultados históricos no equivalen a aceptación funcional.

## 5. Cierre de aceptación

- **Pendiente:** recorrido en Windows/Word, comprobación de permisos y dos verificaciones independientes de conservación de datos para CA-35.
- **Fuera de alcance:** extracción, generación/exportación, historial de generaciones y despliegue/retención definitivos.
- Registrar actor, entorno, escenario, resultado y evidencia cuando se realice la aceptación.
- **Responsable y fecha de aceptación:** pendientes; no hay confirmación registrada.
