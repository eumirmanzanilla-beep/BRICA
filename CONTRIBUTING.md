# Contribuir a BRICA

Esta guía define cómo llevar un cambio desde su necesidad hasta su revisión en GitHub. Distingue el flujo operativo actual de la automatización propuesta: **en la rama inspeccionada no hay workflows de GitHub Actions ni plantillas de issues o pull requests**. Por tanto, las validaciones descritas se ejecutan manualmente; no hay CI automatizado ni CD/despliegue continuo configurado.

Para las reglas de trabajo asistido, seguridad y cuidado del árbol de trabajo, consulta también [`AGENTS.md`](AGENTS.md). Para principios arquitectónicos, consulta [`docs/constitution.md`](docs/constitution.md).

## 1. Flujo resumido

1. Aclarar el problema y registrar un issue con alcance y criterios de aceptación.
2. Revisar el estado del repositorio, las especificaciones aplicables y la configuración real.
3. Crear una rama de trabajo desde `main` actualizado, conservando cambios locales ajenos.
4. Implementar un resultado acotado, probarlo y documentar limitaciones.
5. Crear commits pequeños y relacionados con el issue, cuando estén autorizados.
6. Abrir un pull request (PR) hacia `main`, describir las pruebas y solicitar revisión.
7. Resolver observaciones y verificar el resultado antes de integrar.
8. Tras integrar, desplegar solo mediante el procedimiento operativo autorizado. No hay despliegue automático configurado.

Los commits, el push, la apertura del PR, la integración y cualquier despliegue requieren autorización del responsable del proyecto; esta guía no concede por sí sola esa autorización.

## 2. Issues: definir el trabajo

Busca primero issues existentes para evitar duplicados. No hay una plantilla de issue versionada actualmente, así que incluye manualmente la información pertinente.

### Título

Usa el formato:

```text
[Área] Verbo + resultado verificable
```

Ejemplos:

- `[Obras] Rechazar cambios en obras eliminadas`
- `[Documentación] Aclarar el origen de los campos de referencia`
- `[Cliente] Mostrar un error comprensible si falla la carga de adjuntos`

### Contenido recomendado

**Para una funcionalidad o mejora:**

```markdown
## Problema o necesidad
¿Qué situación concreta se quiere resolver y para quién?

## Resultado esperado
¿Qué debe poder hacer el usuario o qué propiedad debe cumplir el sistema?

## Alcance
- Incluye:
- No incluye:

## Criterios de aceptación
- [ ] Dado ... cuando ... entonces ...
- [ ] ...

## Referencias
- Spec, requisito, decisión o pantalla aplicable:
- Issue relacionado:
```

**Para un error**, añade pasos reproducibles, resultado actual, resultado esperado, versión/rama o entorno afectado y, si es posible, registros o capturas sin datos sensibles. No incluyas credenciales, datos reales de clientes/obras ni documentos confidenciales.

Los criterios deben ser observables y comprobables. Identifica explícitamente dudas de negocio, migraciones, contratos de API, permisos, compatibilidad, aceptación manual y trabajo fuera de alcance. Consulta ERS/DAS/Diccionario como fuentes de intención y confirma el comportamiento vigente en specs, código y migraciones; esos documentos no prueban que una función ya esté implementada.

## 3. Ramas

Usa una rama por PR, basada en `main` actualizado. Convención:

```text
tipo/N-área-resumen
```

Usa un tipo coherente con el cambio (`feat`, `fix`, `refactor`, `docs`, `chore` o `test`), el número de issue y un resumen corto en inglés `kebab-case`.

```bash
git status --short --branch
git switch main
git pull --ff-only origin main
git switch -c fix/42-works-reject-deleted-updates
```

Antes de cambiar de rama, inspecciona y conserva el trabajo local. No fuerces un `pull` si las ramas divergieron: revisa el historial y acuerda cómo integrarlas. No reutilices una rama de otra entrega como base de un PR nuevo sin confirmar que ese es el propósito.

## 4. Implementar y validar

- Mantén el cambio limitado al resultado aprobado. En backend sigue la estructura funcional existente y valida reglas/autorización en el servidor; JavaFX consume la API y no debe bloquear su hilo de interfaz.
- Para cambios de esquema, agrega una migración Flyway nueva; no edites migraciones ya aplicadas. No elimines datos o volúmenes para hacer pasar verificaciones.
- Añade o actualiza pruebas para el comportamiento y los errores relevantes. Las pruebas de integración usan Testcontainers y requieren Docker.
- Para cambios Java, las verificaciones habituales desde la raíz son:

  ```bash
  ./mvnw test
  ./mvnw verify
  git diff --check
  ```

  En PowerShell usa `./mvnw.cmd` en lugar de `./mvnw`. `verify` puede ejecutar pruebas de integración, que requieren Docker y, para determinadas pruebas documentales, el entorno Python preparado según `README.md`.
- Si se modifica JavaFX, además de compilar, abre y recorre la pantalla afectada en Windows. Comprueba FXML, navegación y estados asíncronos. Si la aceptación requiere Word o un formato operativo, registra esa comprobación por separado.
- Para documentación, revisa el Markdown, enlaces, comandos y consistencia con el estado actual; ejecuta `git diff --check`.
- Registra con honestidad qué se ejecutó, qué resultado tuvo y qué queda pendiente. No sustituyas una comprobación visual por una prueba HTTP o una compilación.

## 5. Commits

Un commit debe representar un propósito coherente, revisable y, cuando sea razonable, verificable por sí mismo. Incluye juntos código, pruebas, migración y documentación que sean necesarios para ese paso. Divide motivos independientes; no separes archivos si eso deja el cambio roto.

Usa inglés y este formato:

```text
tipo(ámbito): verbo en imperativo + resultado
```

Ejemplos:

```text
fix(works): reject updates to deleted works
test(documents): cover reference value resolution
docs(repo): describe pull request checks
```

Usa un ámbito reconocible (`works`, `documents`, `desktop`, `server`, `repo`, etc.) y una descripción breve que explique el resultado, no cada archivo. En el cuerpo enlaza el issue:

```text
Refs #42
```

Antes de confirmar, revisa `git diff --staged` y prepara solo los archivos del cambio. No incluyas `.env`, secretos, datos reales, salidas de compilación ni modificaciones incidentales. No reescribas o descartes commits ajenos. Si la autorización para hacer commits no está clara, presenta el mensaje propuesto y espera confirmación.

## 6. Pull requests

No hay una plantilla de PR versionada actualmente. Abre un PR hacia `main` cuando el cambio esté listo para revisión y exista autorización. Mantén un PR por resultado coherente; usa varios commits atómicos si el resultado requiere pasos relacionados.

### Título

Usa inglés y el mismo patrón general:

```text
tipo(ámbito): resultado del PR
```

El título resume el resultado global y no tiene que repetir el tipo de cada commit.

### Descripción

Incluye, como mínimo:

```markdown
## Resumen
Qué resultado entrega y qué problema resuelve.

## Cambios
- Principales cambios y motivo.
- Decisiones relevantes o dependencias entre pasos.

## Issue
Closes #N

## Verificación
- [ ] `./mvnw test` — resultado:
- [ ] `./mvnw verify` — resultado:
- [ ] `git diff --check` — resultado:
- [ ] Validación manual/UI/Word, si aplica:

## Riesgos e impacto
API, migraciones, configuración, seguridad, persistencia, compatibilidad o limitaciones.

## Evidencia y pendientes
Capturas con datos de prueba, decisiones por confirmar y aceptación todavía pendiente.
```

Usa `Closes #N` solo si el PR satisface todos los criterios del issue. Si entrega una parte, usa `Refs #N` y especifica lo que queda. No cierres un issue solo porque el PR esté relacionado.

Antes de publicar, revisa el alcance y el historial respecto de la base del PR:

```bash
git log --oneline origin/main..HEAD
git diff --stat origin/main...HEAD
git diff --check origin/main...HEAD
```

Confirma que no haya secretos ni cambios no relacionados. Atiende los comentarios, vuelve a ejecutar las verificaciones afectadas y comunica lo que falta. No integres el PR hasta contar con las revisiones y aprobaciones requeridas por el responsable del proyecto.

## 7. CI de GitHub: estado actual y criterio futuro

### Estado actual

En esta rama no existe `.github/workflows/` ni configuración de GitHub Actions. Por ello:

- GitHub no ejecuta automáticamente build, pruebas, análisis ni despliegue al abrir o actualizar un PR.
- No hay checks de estado requeridos configurados por el repositorio que puedan considerarse una barrera automática de integración.
- Las verificaciones de la sección 4 son pasos manuales que el autor debe informar en el PR.
- Una marca verde de revisión o la existencia de un PR no significa que la CI haya pasado.

No describas workflows propuestos como si estuvieran activos. Si se configura CI posteriormente, debe documentarse aquí y versionarse en `.github/workflows/`.

### Recomendación para incorporar CI

Al aprobar su implementación, configurar un workflow de GitHub Actions en PRs contra `main` y en pushes a `main`, como mínimo para:

1. Configurar JDK 25 y utilizar Maven Wrapper.
2. Ejecutar `./mvnw verify` en ambos módulos; asegurar que Docker esté disponible para Testcontainers y configurar el entorno Python documental requerido por la suite.
3. Ejecutar `git diff --check` sobre los cambios del PR.
4. Publicar logs/resultados como checks y fallar el workflow si una verificación falla; no exponer secretos en logs.
5. Habilitar protección de `main` y exigir el check exitoso y revisión humana antes de integrar, una vez comprobado que el workflow es estable.

Las pruebas visuales de JavaFX en Windows/Word y las aceptaciones de negocio que no estén automatizadas deben seguir apareciendo como verificaciones manuales en el PR. No inventar un check automatizado para ellas.

## 8. CD y despliegue

No hay CD, publicación de artefactos ni despliegue automático configurados en esta rama. `compose.yml` y `docs/installation-guide.md` describen un entorno local de desarrollo/pruebas; no equivalen a un pipeline de producción. No se debe desplegar automáticamente al cerrar un PR ni presentar Compose local como infraestructura de producción.

Si se aprueba CD en el futuro, diseñarlo como un cambio separado, con destino y responsables definidos, credenciales almacenadas en GitHub Environments/secrets, aprobación para producción, TLS, respaldo/restauración de PostgreSQL y archivos, política de migraciones Flyway, observabilidad y procedimiento de rollback. Probar primero en un entorno de staging equivalente. Los secretos y datos de producción nunca deben entrar al repositorio ni a artefactos públicos.

## 9. Trazabilidad de ejemplo

| Elemento | Ejemplo |
|---|---|
| Issue | `#42 [Obras] Rechazar cambios en obras eliminadas` |
| Rama | `fix/42-works-reject-deleted-updates` |
| Commit | `fix(works): reject updates to deleted works` con `Refs #42` |
| PR | `fix(works): prevent editing deleted works` con pruebas, impacto y `Closes #42` si completa todos los criterios |

## 10. Seguridad y datos

- No subas `.env`, contraseñas, tokens, claves privadas, datos de clientes/obras ni documentos reales.
- Usa `.env.example` solo con valores ficticios y configuraciones no secretas.
- Revisa que el diff no incluya archivos locales, logs, PDFs/DOCX reales ni resultados generados.
- Nunca borres volúmenes, bases, historial o trabajo local para corregir CI sin autorización expresa.
