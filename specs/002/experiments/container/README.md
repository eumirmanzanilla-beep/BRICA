# Imagen y perfil experimentales F0 — Linux amd64

**Solo evaluación autorizada.** No cambia Compose, el Dockerfile del servidor,
sus permisos, cuentas, base ni volúmenes. No inicia el backend ni añade un
servicio. Docker se usa desde el harness del host; su socket **no** se monta en
ningún contenedor. La imagen experimental queda disponible localmente; los
contenedores creados por el runner se eliminan individualmente al terminar.

## Modelo de aislamiento que se evalúa

1. Imagen separada basada en la imagen local del servidor (`noble`, JRE 25), con
   Bubblewrap/OCR/libseccomp de esa distribución. Root se usa solo durante el
   build para instalar paquetes; ejecución con **UID 10001**, rootfs de lectura,
   `--cap-drop ALL`, `no-new-privileges`, red Docker `none` y tres binds de lectura
   con fixtures/JARs/clases sintéticos.
2. Perfil exterior **deny-by-default**, derivado del perfil Moby fijado por
   commit/SHA-256. Añade únicamente `clone` con todos los namespaces + SIGCHLD,
   `unshare(NEWUSER)`, y `mount/umount2/pivot_root` para montar el sandbox. No
   `privileged`, `SYS_ADMIN`, `seccomp=unconfined`, `apparmor=unconfined` ni cambios
   de política global. El cambio permite operaciones concretas de preparación;
   **no es seguro aplicarlo sin el siguiente filtro ni es perfil de producción**.
3. Filtro BPF interior obligatorio, exportado con libseccomp y aplicado por
   Bubblewrap **después** de preparar namespaces/montajes: deniega namespaces,
   montajes, ptrace, BPF, acceso a memoria ajena y sockets distintos de AF_UNIX.
   `clone` normal sigue permitido para threads/JVM/OCR; `clone3` devuelve ENOSYS
   para fallback. Los descendientes heredan el filtro. La allowlist Docker sigue
   vigente; el segundo filtro solo la restringe. Ante error, falla cerrado.
4. Root del sandbox de lectura, sin `/app`, marcadores privados del contenedor,
   Docker socket o cgroup; entorno saneado. `/tmp` propio de **128 MiB**, resto de
   destinos de lectura. No se usa `--dev /dev`, que crearía otro tmpfs escribible;
   solo se montan null/random/urandom. El filtro impide crear tmpfs alternativos.
5. Cgroup Docker: 1 GiB sin swap, 64 tareas, 1 CPU; stress de memoria en otro
   contenedor a **256 MiB** para no consumir 1 GiB en el host. Estos límites se
   verifican dentro del contenedor y se comprueban eventos reales de PID/OOM.
   **Son límites del contenedor experimental completo**, no de un worker
   delegado dentro del backend real.

El contexto de build contiene únicamente archivos de experimento nombrados.
**No ejecutar `docker build` con la raíz BRICA como contexto**: `docs/templates/`
puede contener documentos reales no autorizados.

## Reproducción

Requisitos: Docker con cgroup v2, Linux amd64 y un banco sintético F0 anterior
correcto (p. ej. `/tmp/opencode/brica-spec002-f0-run-06`). No aceptar documentos
reales como inputs del experimento. Elegir rutas externas nuevas para no pisar
evidencias. Desde la raíz del proyecto:

```sh
python3 specs/002/experiments/container/prepare_context.py \
  --output /tmp/opencode/brica-spec002-f0-container-new/context

docker build --tag brica-spec002-f0-sandbox:experimental \
  /tmp/opencode/brica-spec002-f0-container-new/context
```

Descargar el perfil público upstream a un archivo externo nuevo, sin ejecutarlo.
URL exacta y hash permitidos:

```text
https://raw.githubusercontent.com/moby/profiles/9ee3204f0d0cfa19ca2c6a7b0fcf5a2b353954ae/seccomp/default.json
SHA-256 3320527d4568265ac97a077609b1bfd4cfd0d789eeb9239b7e094b1526675406
```

El generador valida ese hash, rechazo por defecto y arquitectura; no sustituir
el upstream por `main` sin nueva revisión. Guardado como `upstream-seccomp.json`:

```sh
python3 specs/002/experiments/container/create_profile.py \
  --base /tmp/opencode/brica-spec002-f0-container-new/upstream-seccomp.json \
  --output /tmp/opencode/brica-spec002-f0-container-new/setup-seccomp.json

python3 specs/002/experiments/container/run_container.py \
  --image brica-spec002-f0-sandbox:experimental \
  --profile /tmp/opencode/brica-spec002-f0-container-new/setup-seccomp.json \
  --synthetic-run /tmp/opencode/brica-spec002-f0-run-06 \
  --work /tmp/opencode/brica-spec002-f0-container-new/results
```

El runner fija el ID local de imagen al comenzar; verifica restricciones con
`docker inspect` antes de arrancar y valores efectivos de cgroup después.
Verifica manifiesto/hash de fixtures antes de copiarlos y usa únicamente sus
copias sintéticas. Cada modo se ejecuta en un contenedor nuevo; no comparte
fuentes con escritura, temporales ni DB. Si falla/timea, mata y elimina únicamente
el ID que acaba de crear, nunca contenedores seleccionados por nombre/patrón.

Resultados: `summary.json`, configuración inspeccionada y stdout/stderr por modo.
No registrar secretos reales: las variables de prueba son marcadores sintéticos.

## Comprobaciones y criterios

- **normal (9)**: payload arrancado; archivos/entorno/capacidades/filtros;
  namespace/red y sockets; denegación de namespaces/montajes posteriores; fuentes
  de lectura; ENOSPC al acumular 128 MiB en varios archivos; salida excesiva;
  timeout con hijo y 28 aserciones PDF/OCR/XLS/XLSX/DOCX del banco previo.
- **pids (2)**: payload y creación finita de hasta 80 hijos que tropieza con el
  límite de 64 tareas; exige EAGAIN y aumento de `pids.events:max`; termina/recolecta
  los hijos. No es una fork bomb ni una prueba en el servicio persistente.
- **memory (2)**: payload y dos hijos que intentan mantener 160 MiB cada uno
  bajo cgroup de 256 MiB. Exige SIGKILL de un hijo y aumento de
  `memory.events:oom_kill`; launcher debe salir correctamente. Docker puede
  mostrar `OOMKilled=true` con `ExitCode=0`, porque murió un descendiente: se
  conserva ese estado como evidencia esperada, no se oculta como ausencia de OOM.
- No contar los 28 asserts de lectores como 28 pruebas de aplicación ni como
  calidad OCR universal. No atribuir el fallo default al kernel/AppArmor por el
  mensaje genérico de Bubblewrap: la traza y el perfil acotado prueban que seccomp
  exterior bloqueaba el `clone` de preparación en este entorno.

## Pendiente antes de producción

El contenedor actual **no delega un subárbol cgroup escribible** al UID del
backend. Estos experimentos no autorizan Docker socket/servicio nuevo ni prueban
un límite agregado independiente para un worker que comparta contenedor con el
backend. Usar el cgroup de todo el servidor expone al backend a presión/OOM del
lector; no se presenta como solución equivalente. Definir y evaluar delegación o
un mecanismo local compatible requiere una decisión de despliegue posterior.

Tampoco se han completado licencias/vulnerabilidades de todas las dependencias
nativas, el corpus hostil/ampliado, modelos degradados/rotados o la integración de
logging/grafo Maven Spring. La imagen usa repositorios Ubuntu al construir;
versiones efectivas/ID se registran, pero no es un build hermético bit a bit.
**F0 permanece abierta; no se aplican cambios a producción.**
