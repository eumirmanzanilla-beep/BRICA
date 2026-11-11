# ============================================================
# Etapa 1: construcción
# ============================================================

FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

# La versión de Maven procede del mismo Wrapper usado en desarrollo.
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copiamos primero los POM para aprovechar la caché de Docker.
COPY pom.xml .
COPY brica-server/pom.xml brica-server/pom.xml
COPY brica-client/pom.xml brica-client/pom.xml

# Descarga dependencias antes de copiar el código fuente.
RUN ./mvnw \
    --no-transfer-progress \
    -pl brica-server \
    -am \
    dependency:go-offline


# Ahora copiamos únicamente el código del servidor.
COPY brica-server/src brica-server/src

# Compilamos el servidor.
RUN ./mvnw \
    --no-transfer-progress \
    -pl brica-server \
    -am \
    clean package \
    -DskipTests


# ============================================================
# Etapa 2: ejecución
# ============================================================

FROM eclipse-temurin:25-jre-noble

WORKDIR /app

# El procesamiento documental es un proceso local, sin servicio ni acceso a la base.
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv \
    && apt-get clean
COPY brica-server/src/main/python/requirements.txt /app/documents-python/requirements.txt
RUN python3 -m venv /opt/brica-document-python \
    && /opt/brica-document-python/bin/pip install --no-cache-dir -r /app/documents-python/requirements.txt \
    && mkdir -p /app/var/documents \
    && chown -R 10001:10001 /app/var/documents
COPY brica-server/src/main/python/brica_docx.py /app/documents-python/brica_docx.py
ENV BRICA_DOCUMENT_PYTHON=/opt/brica-document-python/bin/python
ENV BRICA_DOCUMENT_SCRIPT=/app/documents-python/brica_docx.py
ENV BRICA_DOCUMENT_STORAGE=/app/var/documents

COPY --from=build \
    /workspace/brica-server/target/brica-server-0.1.0-SNAPSHOT.jar \
    app.jar

# Evitamos ejecutar la aplicación como root.
USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
