---
name: brica-postgres
description: "Aplica el flujo de PostgreSQL, JPA y Flyway de BRICA. Úsala al cambiar o revisar el esquema, entidades, consultas, restricciones o migraciones, y al investigar discrepancias entre la base local y el código."
---

## Identificar la base y la versión del esquema

- Lee AGENTS.md, las migraciones vigentes, la entidad o consulta afectada y, si corresponde, el Diccionario de Datos. Revisa compose.yml y application.yml para distinguir la configuración del contenedor de la usada por la aplicación.
- Si tienes DBHub, úsalo para inspección de solo lectura. Confirma el destino con SELECT version(), current_database(), current_user y contrástalo con la instancia de desarrollo de BRICA. En WSL, una conexión a localhost puede llegar al PostgreSQL local en lugar del contenedor. Ante una discrepancia, resuelve primero el destino.
- Si DBHub no está disponible, reconstruye el esquema esperado a partir de Flyway y JPA. Distingue lo inferido del código de lo comprobado contra una instancia. La skill supabase-postgres-best-practices, si está disponible, sirve de referencia para SQL, índices y restricciones.

## Cambiar el esquema

1. Localiza el último estado de Flyway y sigue la convención del repositorio. Para evolucionar el esquema, agrega una migración versionada en la ubicación configurada y conserva las migraciones ya aplicadas.
2. Revisa el efecto en filas existentes, valores nulos, duplicados, claves foráneas y registros con borrado lógico cuando apliquen. Si se requiere rellenar datos, define el origen del valor antes de endurecer la restricción.
3. Ajusta entidades, consultas, mapeos y pruebas que dependan del esquema. Mantén las reglas de negocio en la capa indicada por el proyecto. Justifica los índices y restricciones por una consulta o una regla concreta.
4. Verifica el arranque de una base limpia con Flyway y, cuando el cambio afecte datos existentes, la actualización desde el esquema anterior con datos de prueba representativos. Usa Testcontainers o una base desechable conforme a las pruebas del proyecto. Reporta por separado cada escenario ejecutado.
