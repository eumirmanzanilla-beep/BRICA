#!/usr/bin/env python3
"""Start an isolated, loopback-only BRICA review; never reads .env or existing DB_*.

Database credentials and logs stay in a private external directory. Account passwords
are prompted without echo (or passed from the environment) and never written to disk.
No stop/delete command is automated: print exact resources for explicit later cleanup.
"""
import argparse
import getpass
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def run(command, **kwargs):
    return subprocess.run(command, check=True, text=True, capture_output=True, **kwargs).stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=0, help="Puerto local libre para revisión; 0 elige uno nuevo.")
    args = parser.parse_args()
    if args.port != 0 and not 1024 <= args.port <= 65535:
        raise RuntimeError("El puerto de revisión debe estar entre 1024 y 65535.")
    with socket.socket() as sock:
        try:
            sock.bind(("127.0.0.1", args.port))
        except OSError:
            raise RuntimeError("El puerto de revisión está ocupado; no se detendrá ningún proceso existente.") from None
        server_port = sock.getsockname()[1]
    jar = ROOT / "brica-server/target/brica-server-0.1.0-SNAPSHOT.jar"
    if not jar.exists():
        raise RuntimeError("Compila primero con ./mvnw verify. No se arranca el backend de desarrollo.")
    password = os.environ.get("BRICA_REVIEW_PASSWORD") or getpass.getpass("Contraseña para las dos cuentas de revisión: ")
    if len(password) < 8 or not any(c.isupper() for c in password) or not any(c.isdigit() for c in password) or len(password.encode()) > 72:
        raise RuntimeError("La contraseña requiere 8 caracteres, una mayúscula y un número; máximo 72 bytes UTF-8.")
    base = Path("/tmp/opencode")
    if not base.is_dir():
        base = Path(tempfile.gettempdir())
    state_dir = Path(tempfile.mkdtemp(prefix="brica-review-", dir=base))
    state_dir.chmod(0o700)
    token = secrets.token_hex(6)
    database = "brica_review_" + token
    container = "brica-review-" + token
    db_password = secrets.token_urlsafe(32)
    docker_env = {**os.environ, "POSTGRES_PASSWORD": db_password}
    print("Se creará", container, "con PostgreSQL temporal y solo dos cuentas nuevas. No se copia ni modifica desarrollo.", flush=True)
    run(["docker", "run", "-d", "--name", container, "--label", "brica.review=true",
         "--tmpfs", "/var/lib/postgresql:rw", "-p", "127.0.0.1::5432",
         "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_USER=brica_review",
         "-e", "POSTGRES_DB=" + database, "postgres:18-alpine"], env=docker_env)
    state = {"container": container, "database": database}
    state_path = state_dir / "state.json"
    state_path.write_text(json.dumps(state, indent=2))
    state_path.chmod(0o600)
    # Never use compose: this container has no development volumes or networks.
    for _ in range(60):
        ready = subprocess.run(["docker", "exec", container, "pg_isready", "-U", "brica_review", "-d", database], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(1)
    else:
        raise RuntimeError("PostgreSQL no inició. Recursos de revisión: " + str(state_path))
    binding = json.loads(run(["docker", "inspect", "--format", '{{json .NetworkSettings.Ports}}', container]))
    db_port = binding["5432/tcp"][0]["HostPort"]
    env = {k: v for k, v in os.environ.items() if not k.startswith(("DB_", "SPRING_", "SERVER_", "BRICA_", "POSTGRES_", "LOGGING_", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"))}
    env.update(DB_URL=f"jdbc:postgresql://127.0.0.1:{db_port}/{database}", DB_USER="brica_review", DB_PASSWORD=db_password,
               SERVER_ADDRESS="127.0.0.1", SERVER_PORT=str(server_port), BRICA_ALLOW_HTTP="true",
               BRICA_DOCUMENT_PYTHON=os.environ.get("BRICA_DOCUMENT_PYTHON", "python3"),
               BRICA_DOCUMENT_SCRIPT=str(ROOT / "brica-server/src/main/python/brica_docx.py"),
               BRICA_DOCUMENT_STORAGE=str(state_dir / "documents"))
    log_path = state_dir / "server.log"
    with log_path.open("w") as log:
        log_path.chmod(0o600)
        process = subprocess.Popen(["java", "-jar", str(jar)], env=env, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    url = f"http://127.0.0.1:{server_port}"
    state.update(server_pid=process.pid, url=url)
    state_path.write_text(json.dumps(state, indent=2))
    for _ in range(90):
        if process.poll() is not None:
            raise RuntimeError("El servidor terminó. Consulta " + str(log_path))
        try:
            with urllib.request.urlopen(url + "/api/v1/health", timeout=2) as response:
                if response.status == 200:
                    break
        except OSError:
            pass
        time.sleep(1)
    else:
        raise RuntimeError("El servidor no respondió. Consulta " + str(log_path))
    # Use the packaged dependencies and the same password policy/encoder as BRICA.
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.startswith(("BOOT-INF/lib/", "BOOT-INF/classes/")) and not name.endswith("/"):
                relative = Path(name)
                if ".." in relative.parts:
                    raise RuntimeError("Ruta inválida en el JAR local.")
                destination = state_dir / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(archive.read(name))
    classpath = os.pathsep.join([str(state_dir / "BOOT-INF/classes"), str(state_dir / "BOOT-INF/lib/*")])
    provision_env = {**env, "BRICA_REVIEW_DATABASE": database, "BRICA_REVIEW_PASSWORD": password}
    result = run(["java", "--class-path", classpath, str(ROOT / "tools/ReviewAccounts.java")], env=provision_env)
    print(result)
    print("Servidor de revisión:", url)
    print("Estado y registro privados:", state_dir)
    print("Cliente (terminal aparte): BRICA_ALLOW_HTTP=true BRICA_SERVER_URL=" + url + " ./mvnw -pl brica-client javafx:run")
    print("Para detener sin borrar: kill", process.pid, "y docker stop", container)
    print("La BD es temporal: detener el contenedor pierde sus datos. No se elimina ningún volumen de desarrollo.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError) as failure:
        # Do not print environment, credentials or SQL errors with parameters.
        print("No se completó el entorno de revisión:", str(failure) if isinstance(failure, RuntimeError) else "falló una operación; revisa el registro privado.", file=sys.stderr)
        sys.exit(1)
