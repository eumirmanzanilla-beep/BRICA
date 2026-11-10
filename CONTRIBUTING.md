# Contribuir a BRICA

BRICA centraliza obras, clientes, contratos y documentación de una constructora. El servidor Spring Boot concentra las reglas del negocio y el cliente JavaFX consume su API. La obra es el punto de partida para contratos, presupuestos y documentación.

## Preparar el entorno

Instala Java 25 y habilita Docker Desktop para tu distribución de WSL. Usa el Maven Wrapper del repositorio. Trabaja desde la raíz en WSL y usa PowerShell para validar la interfaz JavaFX en Windows.

1. Configura `.env` con base en `.env.example` para Docker Compose.
2. Si ejecutas el backend con Maven, exporta las variables `DB_*` que indique la configuración del servidor. Maven no carga `.env` automáticamente.
3. Inicia PostgreSQL y el backend desde WSL.

   ```bash
   docker compose up -d postgres
   ./mvnw -pl brica-server spring-boot:run
   ```

4. Para probar la interfaz, abre PowerShell en la raíz de tu copia de BRICA y ejecuta:

   ```powershell
   .\mvnw.cmd -pl brica-client javafx:run
   ```

Si usas copias separadas del repositorio en Windows y WSL, verifica que ambas incluyan los cambios que vas a probar.

## De issue a rama

1. **Define el issue.** Usa la plantilla del repositorio. Titúlalo `[Área] Verbo + resultado verificable`, por ejemplo `[Obras] Impedir la edición de obras eliminadas`. Incluye el problema, el alcance, los criterios de aceptación y, para errores, los pasos de reproducción.
2. **Divide el trabajo antes de editar.** Propón qué entregará cada commit y qué prueba lo verificará. Si el issue reúne resultados que se pueden revisar y entregar por separado, crea issues hijos o plantea PR separados con un alcance claro. Revisa la especificación aplicable y consulta [AGENTS.md](AGENTS.md) para el flujo y las autorizaciones del agente.
3. **Abre una rama por PR desde `main` actualizado.** Revisa `git status` y conserva el trabajo local ajeno al issue. Nombra la rama `tipo/N-area-resumen`, con el número de issue y un resumen en inglés en `kebab-case`. Usa `feat`, `fix`, `refactor`, `docs`, `chore` o `test` según corresponda.

   ```bash
   git switch main
   git pull --ff-only origin main
   git switch -c fix/42-works-reject-deleted-updates
   ```

Si el pull indica que las ramas divergieron, revisa el historial antes de elegir cómo integrarlas.

## Implementar y verificar

- **Servidor.** Organiza el código por funcionalidad. Conserva la dirección `api → application → domain` y deja las implementaciones de los puertos en infraestructura. Valida las reglas de negocio en el servidor.
- **Cliente.** Accede a los datos mediante la API. Si cambias una vista, comprueba el controlador, sus referencias FXML y el flujo en JavaFX. Mantén las llamadas de red fuera del hilo de la interfaz.
- **Base de datos.** Agrega una migración Flyway para los cambios de esquema. Conserva las migraciones ya aplicadas. Usa datos de prueba y mantén fuera de Git las credenciales, `.env` y los documentos reales de la empresa.
- **Pruebas.** Cubre el comportamiento que cambia y sus errores relevantes. Ejecuta `./mvnw test` antes del commit cuando cambies código. Las pruebas de integración con Testcontainers requieren Docker activo.

Antes de abrir un PR con cambios de código, ejecuta desde WSL:

```bash
./mvnw clean verify
git diff --check main...HEAD
```

El segundo comando revisa espacios en los commits de la rama. Si cambias JavaFX, ejecuta también el cliente en Windows y recorre la pantalla afectada. Para un cambio solo de documentación, revisa el Markdown, los enlaces y el diff.

## Commits atómicos

- **Un propósito por commit.** Haz cambios pequeños y significativos que se puedan entender, validar y revertir como unidad. Agrupa el código, las pruebas y los ajustes indispensables para que ese paso compile y pase sus verificaciones. Si el diff reúne dos motivos de cambio, divídelo. Si dividirlo deja un estado roto, conserva juntos los archivos necesarios.
- **Registra cada avance al terminarlo.** Implementa un paso, verifica su resultado y revisa `git diff --staged` antes de confirmar. Agrega solo los archivos o fragmentos de ese paso. Cuando los commits estén autorizados, crea cada uno antes de iniciar el siguiente. Si falta autorización, presenta el avance y su mensaje propuesto.
- **Usa inglés y el formato `tipo(ámbito): descripción`.** Usa un ámbito del proyecto, como `works`, `desktop` o `repo`, y empieza la descripción con un verbo en imperativo, como `reject`, `add` o `extract`. Agrega `Refs #N` en el cuerpo para enlazar el commit al issue. Reserva el cierre del issue para el PR.
- **Mantén el historial legible.** Conserva los commits significativos durante la revisión. Evita mezclar reformateos, refactors independientes y arreglos ajenos al alcance. Si la política del repositorio permite elegir el método de integración, usa uno que preserve esos commits.

## Pull requests acotados

Cada PR hacia `main` entrega **un resultado coherente**. Incluye varios commits atómicos cuando el alcance tenga varios pasos significativos. Un ajuste puntual puede tener un solo commit. Si el diff crece hasta abarcar resultados que se pueden entregar por separado, divide el PR antes de seguir. Cada parte debe quedar en un estado verificable.

Titula el PR en inglés con `tipo(ámbito): resultado`. El título resume el resultado de todo el PR, aunque sus commits tengan otros tipos o ámbitos. Usa la plantilla del repositorio e incluye:

- **Alcance general.** Qué resultado entrega y qué criterio de aceptación cubre.
- **Descripción.** Qué cambió, por qué y cómo se relacionan los pasos si el orden importa.
- **Issue relacionado.** Escribe `Closes #N` si el PR completa el issue. Si entrega solo una parte, escribe `Refs #N` y explica qué queda pendiente.
- **Validación e impacto.** Anota comandos, resultados y comprobaciones pendientes. Si aplica, adjunta una captura con datos de prueba o explica cambios de API, migraciones y configuración.

Antes de publicar el PR, revisa el historial con `git log --oneline main..HEAD` y el alcance con `git diff --stat main...HEAD`. Confirma que cada commit aporta al resultado declarado y que la descripción coincide con lo entregado. Haz commits, push y apertura de PR según las autorizaciones de [AGENTS.md](AGENTS.md). Resuelve los comentarios de revisión y confirma las verificaciones aplicables antes de integrar.

## Ejemplo de trazabilidad

| Elemento | Ejemplo |
|---|---|
| Issue ficticio | `#42 [Obras] Impedir la edición de obras eliminadas` |
| Rama | `fix/42-works-reject-deleted-updates` |
| Commit 1 | `fix(works): reject updates to deleted works` · Incluye la prueba del servidor y `Refs #42` en el cuerpo. |
| Commit 2 | `fix(desktop): explain why deleted works cannot be edited` · Incluye la comprobación del mensaje en JavaFX y `Refs #42` en el cuerpo. |
| PR | `fix(works): prevent editing deleted works` · Describe el rechazo en la API y el mensaje en el cliente. Incluye `Closes #42` si cumple todos los criterios. |
