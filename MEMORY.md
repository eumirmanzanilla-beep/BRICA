# MEMORY.md - Diario de estudio

> **Contexto de integración:** el estado técnico descrito procede de la rama de trabajo `actualization`; parte del código, rutas y experimentos aún no están integrados en `main`. Contrasta con la rama que estés utilizando.
Memoria entre sesiones (~50 líneas). Contrastar con el código, `AGENTS.md` y [seguimiento](specs/001/review.md) antes de actuar.

## Estado actual (2026-11-19)
- Rama activa: `actualization`, creada desde `feat/23-documents-secure-reference-catalog` para reunir el trabajo local; no hay commit ni push de esta rama.
- Ocho rutas están preparadas (staged): actualización `AGENTS.md`, README nuevos en cliente/servidor, dos cambios Java del cliente, `NameImputRules.java`, `draft.md` y `specs/002/spec.md`.
- `draft.md` además tiene cambios posteriores sin preparar (`MD` en `git status`); preservar esa diferencia al seguir trabajando.
- `git diff --cached --check` detectó espacios finales en cambios de Java/Spec y línea vacía final en `brica-client/README.md`. No se corrigieron. No se ejecutaron pruebas en esta sesión.
- Se leyeron completos `docs/ERS_BRICA.docx`, `docs/DAS_BRICA.docx` y `docs/DiccionarioDatos_BRICA.docx`; también configuración, README, constitución y estructura actuales.
- Los documentos de requisitos/diseño/datos son iniciales: describen capacidades futuras y un modelo lógico, no garantizan funcionalidad ni esquema actual. Contrastar siempre con código, POM, configuración y migraciones.
- Los README de módulo pueden describir estructura objetivo, no inventario exacto. Código cliente actual está en `api`, `config`, `presentation`; backend organizado por función con `api/application/domain/infrastructure`.
- `AGENTS.md` actualizado con jerarquía de fuentes, entorno, comandos, límites de arquitectura, pruebas, Flyway, seguridad y cuidado de cambios locales.
- Stack vigente en POM/config: Java 25, Maven Wrapper, Spring Boot 4.1.1, JavaFX 25.0.2, PostgreSQL 18, Flyway. HTTP desactivado por defecto; solo habilitar para desarrollo local.
- Estado funcional/revisión histórica de Spec 001: verificar siempre `specs/001/review.md` antes de asumir aceptación. Windows/Word y aceptación manual no se dan por completadas sin evidencia.

## Decisiones y conocimiento de dominio (revalidar contra specs vigentes)
- La Obra es el centro; paquetes documentales por cliente, sin operación/etapa en el flujo aprobado previamente. Comparación exacta de nombres donde esté implementada.
- D-03/D-10: solo nombres; fechas/importes/moneda/cálculo quedaron diferidos en las decisiones anteriores.
- D-04: `docxtpl` 0.20.2 y proceso Python local aprobados; dependencias fijadas en `brica-server/src/main/python/requirements.txt`, sin servicio documental adicional ni acceso del proceso a DB.
- D-06: almacenamiento de prueba con ruta del servidor; despliegue, retención y respaldo definitivos pendientes (`specs/001/storage-test.md`).
- D-09: convenios/fechas fuera del flujo aprobado anteriormente; no asumir su implementación por aparecer en ERS/DAS.
- D-12: tipo documental agrupa alternativas; una selección por tipo; solo Vigente elegible; eliminada solo lectura.
- Seguridad previamente aprobada: sesión sin caducidad temporal, cookies/CSRF solo en memoria; BCrypt coste 10; contraseña mínimo 8 caracteres, mayúscula y número, límite 72 bytes UTF-8 sin truncar. Confirmar en código antes de cambios.
- Permisos se autorizan en servidor; niveles jerárquicos no implican por sí mismos permisos. Credenciales PostgreSQL y cuentas BRICA son distintas.
- El Diccionario contiene `PAQUETE_DOCUMENTAL.operacion_etapa` y el modelo puede diferir de migraciones actuales. No alterar migraciones aplicadas ni forzar convergencia sin decisión aprobada.

## Aprendizajes y errores a evitar
- `.env` de Compose no se carga automáticamente en Java/Maven. No imprimir secretos ni versionar `.env`.
- HTTPS es el comportamiento predeterminado; HTTP local requiere `BRICA_ALLOW_HTTP=true` en backend y cliente, solo para desarrollo.
- No confundir prueba/compilación con validación visual JavaFX/Word, ni fixtures con plantilla operativa aprobada.
- En JavaFX, no bloquear el hilo de UI; usar llamadas asíncronas y devolver actualizaciones al hilo JavaFX.
- El cliente consume la API; reglas, autorización y persistencia pertenecen al servidor.
- No borrar volúmenes de PostgreSQL/documentos ni usar `docker compose down -v`; los datos son persistentes.
- Mantener versiones y documentos históricos; usar nueva migración Flyway, no editar una aplicada.
- Cambios locales preexistentes se consideran trabajo del usuario. Revisar `git status` antes de editar o preparar archivos.

## Próximos pasos
- Resolver los espacios finales observados solo si se solicita o antes de una entrega que requiera diff limpio; no modificar en bloque código ajeno sin revisar.
- Revisar `draft.md` en el árbol de trabajo: contiene cambios no preparados además de lo staged.
- Ejecutar pruebas Maven con Docker/Testcontainers cuando corresponda; preparar Python documental en entorno aislado para desarrollo local.
- Continuar aceptación manual Windows/Word y elegir plantilla operativa conjuntamente; registrar evidencias sin afirmar resultados no verificados.
- Antes de iniciar F7/motor documental, revalidar estado, decisiones y autorización vigente en specs/seguimiento; no inferir alcance de ERS/DAS.
