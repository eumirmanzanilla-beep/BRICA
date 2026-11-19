# Experimentos ejecutables F0 (no producción)

Solo datos sintéticos. No lanzar estos programas contra un archivo de negocio,
no importar sus clases en BRICA y no añadir este directorio como módulo Maven.
Se necesita Linux con namespaces de usuario/red habilitados, Bubblewrap, Python
estándar y JDK 25. El ejecutor **falla** si no puede aislar; no tiene fallback.

**Ampliación autorizada:** [container/](container/README.md) evalúa una imagen
aislada `noble` con un perfil seccomp específico, segundo filtro para el lector y
límites cgroup/tmpfs. No modifica el despliegue ni elimina las limitaciones del
launcher host que se describen aquí.

## Archivos

- `F0Probe.java`: genera PDF digital, escaneado/mixto, cifrado/dañado y de límites;
  XLSX con hojas/filas ocultas, fusiones y cuatro estados de caché; XLS binario;
  DOCX sin marcadores y entrada ZIP muy comprimible. Otro modo los lee y verifica
  resultados bajo sandbox. No evalúa fórmulas ni ejecuta macros.
- `expected.json`: anotaciones escritas por separado, no calculadas de la salida
  del lector. Los asserts del experimento concretan esas expectativas; aún no
  contiene un mapeador/presupuesto o el corpus completo S01–S10.
- `evaluate.py`: compila/genera desde código controlado, copia JARs a una raíz
  externa nueva y ejecuta comprobaciones de aislamiento/límites y lectores/OCR.
  Monta fuentes/JARs/clases/runtime en solo lectura y scratch efímero; limpia el
  entorno. No monta `/home`, repositorio, `.env`, sockets, DB o volúmenes.
- `audit_dependencies.py`: inventario/hash de JARs, licencias directas POM,
  LICENSE/NOTICE y consulta online OSV por coordenada exacta. No se ejecuta dentro
  del sandbox: la consulta requiere red y solo envía coordenadas/versiones públicas.
- `policy_examples_test.py`: tres ejemplos ejecutables de la política decimal
  elegida, no pruebas de una implementación Java de negocio.

## Preparación reproducible

Crear un POM **fuera del repositorio** con dos dependencias:
`org.apache.pdfbox:pdfbox:3.0.8` y `org.apache.poi:poi-ooxml:6.0.0`. No heredar el
POM BRICA en esta evaluación. Resolver con el Maven Wrapper existente, por ejemplo:

```sh
./mvnw -f /tmp/opencode/brica-spec002-f0-poc/pom.xml \
  dependency:build-classpath \
  -Dmdep.outputFile=/tmp/opencode/brica-spec002-f0-poc/classpath --no-transfer-progress
```

El POM aislado de la primera evaluación sigue en esa ruta; para otro entorno usar
un `<project>` Maven mínimo con esas dos dependencias y registrar el grafo nuevo.
Los archivos de salida se crean fuera de Git y no se sobrescriben.

Para OCR se evaluaron paquetes Ubuntu `resolute`, descargados con `apt-get download`
y desempaquetados con `dpkg-deb -x`, **sin `sudo`, `apt install` ni instalación en
el host**. No mezclar estos binarios con la imagen `noble` del servidor.

| Paquete desempaquetado | Versión evaluada |
|---|---|
| tesseract-ocr, libtesseract5 | 5.5.0-1build1 |
| libleptonica6 | 1.86.0-1 |
| tesseract-ocr-eng, tesseract-ocr-spa, tesseract-ocr-osd | 1:4.1.0-2build1 |
| libarchive13t64 | 3.8.5-1ubuntu2.3 |
| libgomp1 | 16-20260322-1ubuntu1 |
| libgif7 | 5.2.2-1ubuntu3.2 |
| libtiff6 | 4.7.0-3ubuntu5 |
| libwebp7, libwebpmux3 | 1.5.0-0.1build1 |
| libopenjp2-7 | 2.5.4-1ubuntu0.1 |
| liblerc4 | 4.0.0+ds-5ubuntu2 |
| libjbig0 | 2.1-6.1ubuntu3 |
| libdeflate0 | 1.23-2ubuntu1 |

`ldd` identifica dependencias adicionales proporcionadas por el host; esto no es
un runtime hermético ni una lista completa para redistribuir. El informe guarda
`tesseract --version` con bibliotecas detectadas. Modelos bajo
`usr/share/tesseract-ocr/5/tessdata`; ejecutable bajo `usr/bin/tesseract`.
Si cambian distribución o versiones, repetir evaluación y revisión de licencias.

## Ejecución

Desde la raíz de BRICA, eligiendo un `--work` externo **que no exista**:

```sh
python3 specs/002/experiments/evaluate.py \
  --classpath /tmp/opencode/brica-spec002-f0-poc/classpath \
  --ocr-root /tmp/opencode/brica-spec002-f0-ocr/root \
  --work /tmp/opencode/brica-spec002-f0-new-run

python3 specs/002/experiments/audit_dependencies.py \
  --classpath /tmp/opencode/brica-spec002-f0-poc/classpath \
  --output /tmp/opencode/brica-spec002-f0-new-audit.json

python3 -B -m unittest discover -s specs/002/experiments -p '*_test.py' -v
```

`manifest.json` registra tamaños/SHA-256 de las fuentes generadas antes de leerlas;
`report.json` registra tiempos de pared, salida diagnóstica acotada, códigos y
resultado de cada comprobación. Verifica hashes tras lectura. Los PDF/Office
pueden llevar timestamps/IDs: se verifica integridad por ejecución, no igualdad
binaria entre generaciones. El pico RSS acumulado procede de `RUSAGE_CHILDREN`,
incluye procesos anteriores (javac/generador) y **no** mide RAM máxima del árbol de
JVM+OCR ni sirve para dimensionar producción. La CPU de nietos dentro del PID
namespace tampoco queda atribuida necesariamente al launcher exterior.

## Limitaciones deliberadas

- Bubblewrap prueba aislamiento de filesystem/red en **el host**; no demuestra
  compatibilidad dentro del contenedor del servidor. No modificar Compose para
  dar privilegios ni montar Docker socket. Ver bloqueo en la ficha técnica F0.
- RLIMIT_AS/CPU/FSIZE/NOFILE son por proceso. Sin cgroup/cuota de tmpfs no hay
  presupuesto agregado de memoria, tareas o disco. El proceso Java acotado y el
  timeout son complementos, no prueba de sandbox suficiente para producción.
- `preexec_fn` solo se usa en este launcher Python monohilo, nunca desde el
  backend Java. Matar Bubblewrap elimina el PID namespace y los hijos internos;
  la prueba con hijo dormido comprueba que el timeout no queda esperando sus pipes.
- OCR limpio con cuatro tokens exactos no prueba tablas complejas, rotación,
  tolerancia al ruido, mezcla/desduplicación automática ni mapeo a campos.
- La auditoría Maven no incluye resolución del BOM Spring ni seguridad/licencias
  completas de todos los componentes nativos. Un resultado OSV vacío no certifica
  seguridad. Los avisos de logging permanecen visibles para no ocultarlos.
- No se ejecuta el backend, la suite Maven ni PostgreSQL desde estos experimentos;
  no hay migraciones, API, UI, confirmaciones o generación documental.
