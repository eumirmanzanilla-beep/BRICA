# Fixtures DOCX — prototipos técnicos

**Estado:** fixtures técnicos utilizados por el analizador docxtpl y sus pruebas, no formatos operativos aprobados. E1 mantiene pendiente construir conjuntamente una nueva plantilla sobre otro documento; el acta demostrativa no se adopta como formato de negocio. Recorrido: [prueba manual](../manual-test.md).

## Propuesta inicial

- Documento de prueba: acta de inicio demostrativa; no es la futura plantilla de negocio.
- `{{NOMBRE_OBRA}}` → atributo aprobado `WORK_NAME`.
- `{{NOMBRE_CLIENTE}}` → atributo aprobado `CLIENT_NAME`, siempre desde el cliente asociado a la obra.
- Propuesta de obligatoriedad para la primera revisión: ambos campos obligatorios. **Pendiente de aprobación:** el script no registra esa obligatoriedad ni crea campos/plantillas en la base.
- Sin fechas, importes, firmas ficticias ni datos de una empresa real.

El prototipo contiene cuerpo, tabla, encabezado y pie, marcadores repetidos y uno dividido en runs dentro del mismo párrafo. Se incluyen tres fixtures separados: documento estático, clave desconocida y marcador dividido entre párrafos (estos dos últimos deben bloquearse en F3).

## Reproducir los archivos de prueba

Desde la raíz:

```bash
python3 specs/001/fixtures/build_fixtures.py
```

Se usa únicamente la biblioteca estándar de Python para construir paquetes OOXML de prueba, no para implementar un analizador o motor. Salida: `brica-server/src/test/resources/documents/`. El script verifica ZIP/XML y no sobrescribe archivos diferentes ya existentes.

## Pendiente de revisión con el responsable

1. Elegir otro documento para la nueva plantilla conjunta y acordar su presentación. Abrir los fixtures en Word solo como revisión técnica.
2. Revisar nombre de cada campo, sintaxis y origen. Los atributos del Diccionario no son automáticamente campos publicados ni marcadores.
3. Confirmar obligatoriedad para esta versión; otras plantillas pueden tener distinta clasificación.
4. Acordar qué información adicional hace falta y añadirla mediante el flujo de su entidad de origen.

Las pruebas Python verifican además compatibilidad mediante renderizado efímero de una copia normalizada, con escape XML. No hay motor/API de generación ni salida definitiva. Los fixtures no acreditan aceptación visual ni documental de producción.
