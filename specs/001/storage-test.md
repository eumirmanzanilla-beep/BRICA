# D-06 — prueba aislada de almacenamiento del servidor

## Alcance

Se autoriza probar **archivos construidos**, no seleccionar el almacenamiento definitivo de la empresa. El adaptador puede reemplazarse sin cambiar plantillas, paquetes o resolución de negocio.

```text
JavaFX → API → caso de uso → DocumentFileStore → ruta del servidor
                      └── PostgreSQL: metadatos y estado de registro
```

Python solo analiza los bytes que recibe; no administra almacenamiento ni consulta PostgreSQL.

## Configuración de prueba

- `BRICA_DOCUMENT_STORAGE`: raíz de archivos; para Maven se recomienda una ruta absoluta bajo `brica-server/var/documents`, ignorada por Git.
- Docker: raíz `/app/var/documents`, usuario no privilegiado `10001`, volumen de desarrollo `brica_document_test_files`. La imagen crea la carpeta con sus permisos antes de cambiar de usuario.
- Metadatos: nombre original (solo presentación), ruta relativa generada, MIME DOCX, tamaño, SHA-256 y fecha. Los nombres del usuario no se usan para construir rutas.
- Ruta generada: `registrations/<confirmation-uuid>.docx`. No se exponen rutas físicas en la API.
- Escritura en archivo temporal, sincronización del contenido y publicación por movimiento atómico dentro de la misma carpeta. Si el sistema no admite publicación atómica, falla de forma segura; no degrada a una copia parcial.
- Lectura: sin symlinks ni traversal, límite de bytes y comprobación de tamaño/hash antes de uso. No se sustituye un archivo perdido por uno vacío.
- No hay borrado automático, retención, limpieza de huérfanos ni migración de rutas. No borrar manualmente archivos de una base usada para otras pruebas.

Los límites iniciales del analizador son 1 MiB de carga, 20 MiB de contenido ZIP, 512 entradas, 20 segundos y dos procesos locales simultáneos. Son controles conservadores para esta prueba, no capacidad operativa definitiva.

## Confirmación y recuperación

1. El servidor vuelve a analizar y compara la huella revisada. Un cambio devuelve 409 y el análisis actualizado, sin publicar una versión.
2. Con revisión y clasificación válidas, guarda durablemente una operación `PENDING` con sus insumos y hash de solicitud.
3. Publica el archivo inmutable. Si la escritura falla de manera conocida, marca la operación `FAILED`.
4. Bajo bloqueo por plantilla, verifica el archivo, guarda metadatos, versión y asociaciones, y cambia a `COMPLETED` en una sola transacción.
5. El número de versión se asigna al completar bajo ese bloqueo. No se usa un ID de archivo como regla de «última versión».
6. Un reenvío con el mismo UUID y solicitud devuelve el estado/resultado original; con otro contenido devuelve conflicto. Una intención nueva usa otro UUID.
7. La recuperación es **administrativa explícita**, no una tarea programada: `POST /api/v1/document-registrations/{id}/recover`. La consulta es `GET` a la misma ruta sin `/recover`.
8. Si queda evidencia verificable, completa sin duplicar; si falta archivo o integridad, queda `FAILED` y explica cómo reintentar. Un fallo transaccional durante el completado conserva `PENDING` y permite consultar/recuperar tras corregir la causa.

Solo las versiones completadas se consultan como versiones registradas. Las pendientes/fallidas nunca sustituyen la última versión válida. Una plantilla cuya última versión tiene el archivo perdido/corrupto deja de ofrecerse para nuevas selecciones; no cae silenciosamente a una revisión anterior.

## Cómo comprobar sin tocar datos de desarrollo

Preparar Python según [manual-test.md](manual-test.md). Desde la raíz:

```bash
./mvnw -pl brica-server -Dtest=ServerDocumentFileStoreTest,DocumentConfigurationIntegrationTest,ReferenceFieldMigrationIntegrationTest test
```

Estas pruebas usan PostgreSQL 18 desechable y una raíz propia bajo `/tmp/opencode/`, no la raíz configurada para desarrollo. Verifican lectura desde un adaptador nuevo, duplicados, traversal/symlinks, hash corrupto, escritura fallida, interrupción tras publicar, rollback de metadatos, reenvíos y confirmaciones concurrentes. No simulan un respaldo ni un corte de energía del servidor final.

Para el recorrido manual normal, seguir la guía de versiones/paquetes y reiniciar únicamente el backend que se esté probando para comprobar conservación. No detener PostgreSQL, corromper archivos o cambiar permisos de rutas compartidas para forzar errores: los fallos destructivos se inyectan en pruebas desechables.

## Lo que no se acepta con esta prueba

Servidor/ruta de producción, permisos del sistema operativo definitivo, volumen final, respaldo conjunto archivo+base, restauración, retención, cuotas, alta disponibilidad, fallos de energía, migración entre almacenamientos y limpieza segura de huérfanos. Deben tratarse en una entrega independiente. No ejecutar `docker compose down -v` como parte de esta guía.
