# Guía de contribución — BRICA

## 1. Principios generales

Los cambios deben ser pequeños, trazables y coherentes
con la arquitectura del proyecto.

Los cambios destinados a main deben proponerse mediante
Pull Requests.

No se realizarán force pushes sobre main.

## 2. Convención de ramas

Formato:

tipo/numero-issue-descripcion

El número de Issue puede omitirse cuando no exista
un Issue relacionado.

Tipos permitidos:

- feat: funcionalidad nueva.
- fix: corrección de errores.
- refactor: mejora interna sin cambio funcional.
- docs: documentación.
- chore: mantenimiento.
- test: pruebas.

Ejemplos:

feat/19-desktop-contracts
fix/works-etag-handling
refactor/works-controller
docs/architecture-update
chore/repository-cleanup

Utilizar minúsculas y guiones para separar palabras.

## 3. Mensajes de commit

Formato:

tipo(alcance): descripción breve

Ejemplos:

feat(desktop): add work registration form
fix(works): handle stale ETag response
refactor(client): extract dialog components
docs(architecture): update domain naming ADR
chore(repo): remove obsolete project files
test(clients): add API response parsing tests

El alcance es opcional.

No utilizar mensajes ambiguos como:

update
changes
fix stuff
final version

## 4. Issues

Todo trabajo significativo debe tener un Issue que describa:

- Objetivo o defecto.
- Alcance.
- Criterios de aceptación.
- Dependencias relevantes.

Las tareas pequeñas y exclusivamente administrativas
pueden documentarse directamente en el PR.

## 5. Pull Requests

El título debe seguir Conventional Commits.

El PR debe explicar:

- Qué cambia.
- Por qué es necesario.
- Qué Issue aborda.
- Qué validaciones se realizaron.
- Qué limitaciones quedan pendientes.

Se recomienda un PR por funcionalidad o cambio técnico
coherente.

No mezclar refactorizaciones extensas con nuevas
funcionalidades salvo necesidad justificada.

## 6. Integración

La rama main representa el código integrado.

Antes de fusionar un PR se debe revisar manualmente
su alcance, diferencias y efectos.

Las pruebas y validaciones realizadas deben declararse
sin atribuir resultados que no se hayan obtenido.

La protección automática de main y la integración
continua quedan pendientes de implementación.

## 7. Arquitectura

El cliente JavaFX se comunica con el backend mediante
la API REST.

El cliente no accede directamente a PostgreSQL.

Las reglas de negocio y persistencia pertenecen
al backend.

Se deben respetar las decisiones arquitectónicas
registradas en docs/adr.

## 8. Seguridad

No incorporar al repositorio:

- Contraseñas.
- Tokens.
- Archivos .env con credenciales.
- Datos privados de clientes.
- Archivos temporales o generados.

Los cambios de esquema de PostgreSQL deben gestionarse
mediante migraciones Flyway.

## 9. Documentación

Actualizar README y documentos relacionados cuando
un cambio afecte arquitectura, configuración,
funcionalidades o procedimientos de ejecución.
