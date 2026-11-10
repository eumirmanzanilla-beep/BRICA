# MEMORY.md - Diario de estudio

Memoria entre sesiones (~50 líneas). Contrastar con el código y [seguimiento](specs/001/review.md) antes de actuar.

## Estado actual
- Rama `feat/23-documents-secure-reference-catalog` (issue #23); convenciones en `docs/22-repo-contribution-workflow` (#22). Dos PR borrador autorizados, sin merge.
- Java 25, Maven multimódulo, Spring Boot 4.1.1, JavaFX 25.0.2, PostgreSQL 18/Flyway.
- Spec 001: catálogo de nombres `WORK_NAME`/`CLIENT_NAME` y resolución con API protegida implementados (V5).
- Seguridad V6: sesiones en servidor, CSRF, permisos vigentes, acceso JavaFX y administración local `security-admin` sin web.
- Versiones/paquetes, analizador y pantallas documentales no implementados; F3 no iniciada. 18/53 tareas cerradas; módulo no aceptado completo.
- Verificación 2026-11-10: `test` y `clean verify`, 130 servidor + 14 cliente = 144, sin fallos/errores/omitidas. Estados intermedios también probados por separado.
- Prueba cruzada sin UI (cliente real + Spring + PostgreSQL desechable): login/cookie/logout y contraseña incorrecta correctos.
- Responsable usa WSL para cliente y reportó crear cuentas; acceso visual exitoso de esas cuentas y recorrido completo aún no confirmados.

## Decisiones (Y por qué)
- D-02: paquetes por cliente, sin operación/etapa; generar en cualquier orden/momento, conservando permisos/elegibilidad/versiones.
- D-07: nombres de paquete por comparación exacta dentro del cliente, sin recortar ni convertir mayúsculas.
- D-03/D-10: mínimo inicial de nombres; fechas/importes diferidos. Futuros formatos: `dd/MM/uuuu`, dos decimales con punto sin agrupación/moneda; cálculo/redondeo pendiente.
- D-04: evaluar Python (`python-docx`/`docxtpl`), sin biblioteca, integración ni arquitectura aprobadas. No añadir POI por defecto.
- D-06: rutas/almacenamiento interno/despliegue definitivo diferidos a otra iteración; no implican aceptación del versionado/archivos completos.
- D-09: convenios/cambios contractuales fuera del flujo; no ampliar silenciosamente correcciones de fechas. CA-04 pendiente.
- F0-07/E1: diseñar juntos otra plantilla aún por elegir; acta existente solo fixture técnico. Presupuesto disponible para extracción, no plantilla; archivo aún no identificado/analizado aquí.
- D-12 sigue pendiente: identidad que agrupa plantillas alternativas de un documento; no la resuelve eliminar etapas.
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
- `docxtpl` documenta restricciones de etiquetas entre runs; demostrar compatibilidad con CA-11 antes de elegirlo. No sustituir formato con texto plano.
- Diccionario aún contiene `PAQUETE_DOCUMENTAL.operacion_etapa`; Spec 002 aún menciona compatibilidad por operación. Alinear por separado, no conservar el concepto eliminado en el diseño nuevo.
- Entrega #23: fuentes, catálogo, seguridad, escritorio, fixtures e instrucciones en seis commits. `.env` y `dbhub.toml` personales conservados fuera de commits.

## Próximos pasos
- Elegir con el responsable el documento de salida y construir su plantilla: contenido, campos, obligatoriedad y revisión visual.
- Recibir/identificar el presupuesto para extracción; acordar formato/alcance aparte, sin asumir PDF ni un modelo de contrato.
- Profundizar evaluación Python y resolver D-12; presentar propuesta antes de implementar o añadir dependencias.
- Completar verificación manual de acceso/consola y Windows conforme a AGENTS; F0/F1 siguen parciales antes de F3.
- Retomar D-06 y fechas/importes en sus iteraciones; no cerrar criterios dependientes por posponerlos.
