# Experimental F0 image only. Never used by compose.yml or the application build.
ARG BASE_IMAGE=brica-dev-brica-server:latest
FROM ${BASE_IMAGE}

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bubblewrap strace libseccomp2 tesseract-ocr tesseract-ocr-spa tesseract-ocr-eng \
    && mkdir -p /experiment /synthetic-private \
    && printf 'synthetic marker, not a credential\n' > /synthetic-private/marker

COPY specs/002/experiments/evaluate.py /experiment/evaluate.py
COPY specs/002/experiments/container/ /experiment/container/

USER 10001:10001
WORKDIR /tmp
ENTRYPOINT ["python3", "-B", "/experiment/container/probe.py"]
