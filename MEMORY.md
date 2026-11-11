# MEMORY.md - Diario de estudio

Memoria entre sesiones (~50 líneas). Contrastar con el código y [seguimiento](specs/001/review.md) antes de actuar.

## Estado actual
- Rama `feat/23-documents-secure-reference-catalog` (issue #23); convenciones en `docs/22-repo-contribution-workflow` (#22). Dos PR borrador autorizados, sin merge.
- Java 25, Maven multimódulo, Spring Boot 4.1.1, JavaFX 25.0.2, PostgreSQL 18/Flyway.
- Spec 001: catálogo de nombres `WORK_NAME`/`CLIENT_NAME` y resolución con API protegida implementados (V5).
- Seguridad V6: sesiones en servidor, CSRF, permisos vigentes, acceso JavaFX y administración local `security-admin` sin web.
- Preliminar hasta F6 implementado: analizador docxtpl, versiones, archivos aislados, paquetes/borradores/selección y pantallas. V7 nueva; V1–V6 conservadas.
- Seguimiento: 38/53 tareas cerradas técnicamente, 15 parciales/en verificación/pendientes/diferidas; ninguna aceptación manual se inventa.
- `clean verify` repetido al organizar commits: 150 servidor + 18 cliente, todo correcto; suite Python incluida. Estados intermedios y evidencia en review sección 16. FXML WSL/Docker correctos; Windows/Word pendientes.
- Prueba cruzada sin UI (cliente real + Spring + PostgreSQL desechable): login/cookie/logout y contraseña incorrecta correctos.
- Responsable usa WSL para cliente y reportó crear cuentas; acceso visual exitoso de esas cuentas y recorrido completo aún no confirmados.

## Decisiones (Y por qué)
- D-02: paquetes por cliente, sin operación/etapa; generar en cualquier orden/momento, conservando permisos/elegibilidad/versiones.
- D-07: nombres de paquete por comparación exacta dentro del cliente, sin recortar ni convertir mayúsculas.
- D-03/D-10: solo nombres. Fechas/importes/moneda/cálculo diferidos; propuestas futuras del responsable preservadas en el plan sin resolver sus diferencias de precisión.
- D-04: docxtpl 0.20.2 y proceso Python local aprobados; JSON/timeout, sin servicio adicional ni acceso a DB. Dependencias fijadas en `brica-server/src/main/python/requirements.txt`.
- D-06: prueba construida aprobada con ruta del servidor y puerto/adaptador aislados. Despliegue/retención/respaldo definitivos pendientes; guía independiente `specs/001/storage-test.md`.
- D-09: convenios/fechas fuera del flujo. CA-04/10 requieren origen opcional real; los nombres actuales son obligatorios. Captura manual mediante entidad de origen, sin overrides documentales.
- F0-07/E1: plantilla operativa todavía no adoptada; acta existente solo fixture técnico. Tres documentos nuevos recibidos se conservan localmente; su adaptación fue cancelada, sin ampliar el flujo.
- D-12 resuelta: tipo documental agrupa alternativas; una selección por tipo. Estados Vigente/Borrador/No disponible válidos y administrables; solo Vigente elegible; eliminada solo lectura.
- Sesión sin caducidad temporal; cookies/CSRF solo en memoria. BCrypt coste 10, mínimo 8 caracteres con mayúscula/número, máximo 72 bytes UTF-8 sin truncar.
- Usuarios exactos, ACTIVE/INACTIVE; roles 1/0. Permisos de lectura/escritura y valores/administración independientes y comprobados en servidor.
- Diccionario define atributos; solo se autorizó previamente cambiar longitud de marcador de 50 a 54 en una celda.

## Aprendizajes y errores a evitar
- `.env` de Compose no se carga automáticamente en Java/Maven. `read -rsp 'Contraseña: ' DB_PASSWORD` necesita nombre de variable; no imprimir secretos.
- Credenciales PostgreSQL no son cuentas BRICA. No crear usuarios repetidos ni resetear contraseñas como diagnóstico sin evidencia.
- Login falló antes de autenticar por HTTPS exigido sobre HTTP local. Se habilitó `BRICA_ALLOW_HTTP=true` solo en `.env` local ignorado y recreó backend, sin cambiar cuentas.
- El cliente requiere su propia opción HTTP en desarrollo: `BRICA_ALLOW_HTTP=true ./mvnw -pl brica-client javafx:run`; mantener HTTPS por defecto.
- Cliente distingue conexión/HTTPS/CSRF/rechazo sin mostrar secretos. Respuestas tardías no deben sobrescribir cookies de otra sesión ni repetir escrituras.
- No confundir una compilación/prueba HTTP con validación visual JavaFX/Word, ni fixtures con plantilla operativa aprobada.
- docxtpl restringe runs; adaptación en copia privada con aliases internos y prueba de renderizado efímero. Original intacto; solo sintaxis `{{CLAVE}}`, sin Jinja libre ni motor de generación.
- Diccionario aún contiene `PAQUETE_DOCUMENTAL.operacion_etapa`; Spec 002 aún menciona compatibilidad por operación. Alinear por separado, no conservar el concepto eliminado en el diseño nuevo.
- Entrega #23: seis commits previos más cinco nuevos por alcance (análisis, versiones/paquetes, runtime, escritorio, documentación), con Refs #23. Solo commits locales; sin push/merge. Configuración personal y tres documentos nuevos fuera de commits.

## Próximos pasos
- Ejecutar `specs/001/manual-test.md` en Windows/Word, registrar permisos, estados, dos evidencias CA-35 y recuperación de borradores. Elegir conjuntamente la plantilla operativa.
- Reanudar la adaptación de documentos locales solo ante otra solicitud; no crear TXT ni implementar extracción como parte de la organización del historial.
- Antes de Maven, instalar requisitos Python en un entorno aislado y exportar `BRICA_DOCUMENT_PYTHON`; las pruebas no se omiten si falta docxtpl.
- Completar verificación manual de acceso/consola; F0/F1 siguen parciales por aceptación, no bloquean el avance técnico expresamente aprobado hasta F6.
- No iniciar F7/motor. Retomar D-06 definitivo y fechas/importes por separado; no cerrar criterios dependientes por posponerlos.
