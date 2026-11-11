"""Restricted BRICA DOCX analysis. No database, network, user code or persistent files."""
import base64
import io
import json
import re
import sys
import zipfile
from pathlib import PurePosixPath

from docxtpl import DocxTemplate
from lxml import etree

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
KEY = re.compile(r"\{\{([A-Z0-9_]{1,50})\}\}")
PART = re.compile(r"word/(document|header[0-9]+|footer[0-9]+)\.xml\Z")


def xml(content):
    if b"<!DOCTYPE" in content.upper() or b"<!ENTITY" in content.upper():
        raise ValueError("XML con declaraciones externas no admitidas.")
    return etree.fromstring(content, etree.XMLParser(resolve_entities=False, no_network=True))


def prepare(content, max_entries=512, max_expanded=20 * 1024 * 1024):
    """Return a normalized private copy and a report; original bytes stay untouched.

    Aliases make even canonical keys starting with digits safe Jinja identifiers.
    Only placeholder text is coalesced, preserving unrelated text/runs and styles.
    """
    occurrences, diagnostics, aliases, modified = [], [], {}, {}

    def error(code, location, message):
        diagnostics.append(dict(code=code, location=location, message=message))

    with zipfile.ZipFile(io.BytesIO(content)) as archive:
        entries = archive.infolist()
        names = [e.filename for e in entries]
        if len(entries) > max_entries or len(set(names)) != len(names):
            raise ValueError("Demasiadas entradas o nombres ZIP duplicados.")
        if sum(e.file_size for e in entries) > max_expanded:
            raise ValueError("El DOCX descomprimido supera el límite permitido.")
        if any(e.flag_bits & 1 or e.compress_type not in (0, 8) for e in entries):
            raise ValueError("Contenedor cifrado o compresión no admitida.")
        if not {"[Content_Types].xml", "_rels/.rels", "word/document.xml"}.issubset(names):
            raise ValueError("El archivo no es un DOCX completo.")
        for e in entries:
            path = PurePosixPath(e.filename)
            if path.is_absolute() or ".." in path.parts or "\\" in e.filename:
                raise ValueError("Ruta ZIP no admitida.")
            if "vbaproject" in e.filename.lower() or e.filename.lower().endswith(".bin"):
                raise ValueError("Contenido ejecutable no admitido.")
            if not e.filename.endswith((".xml", ".rels")):
                continue
            data = archive.read(e)
            root = xml(data)
            if any(any(token in value for token in ("{{", "}}", "{%", "{#"))
                   for node in root.iter() for value in node.attrib.values()):
                error("UNSUPPORTED_REGION", e.filename, "Marcador en un atributo XML no admitido.")
            if e.filename == "[Content_Types].xml" and b"macroEnabled" in data:
                raise ValueError("No se admiten documentos con macros.")
            if e.filename.endswith(".rels"):
                for rel in root:
                    target = rel.get("Target", "")
                    if rel.get("TargetMode", "").lower() == "external" or ":" in target or "\\" in target:
                        raise ValueError("Relaciones externas no admitidas.")
                    # Relative parent references are valid OOXML; no ZIP entries are extracted.
                continue
            if not PART.fullmatch(e.filename):
                text = "".join(root.itertext())
                if any(token in text for token in ("{{", "}}", "{%", "{#")):
                    error("UNSUPPORTED_REGION", e.filename, "Hay marcadores en una parte no admitida.")
                continue
            for node in root.iter():
                if (node.tag != W + "t" or next(node.iterancestors(W + "p"), None) is None) and any(
                        token in (node.text or "") for token in ("{{", "}}", "{%", "{#")):
                    error("UNSUPPORTED_REGION", e.filename, "Marcador en texto o instrucción fuera del soporte.")
            for number, paragraph in enumerate(root.iter(W + "p"), 1):
                location = f"{e.filename}:p{number}"
                nodes = [n for n in paragraph.iter(W + "t")
                         if next(n.iterancestors(W + "p"), None) is paragraph]
                text = "".join(n.text or "" for n in nodes)
                if not any(t in text for t in ("{", "}")):
                    continue
                # Text boxes, drawing text, revisions and field instructions have no rendering contract.
                forbidden = {W + "txbxContent", W + "del", W + "ins", W + "sdt"}
                if any(a.tag in forbidden for a in paragraph.iterancestors()) or any(
                        a.tag in forbidden for node in nodes for a in node.iterancestors() if a is not paragraph):
                    error("UNSUPPORTED_REGION", location, "Marcador en una región no admitida.")
                    continue
                matches = list(KEY.finditer(text))
                remainder = KEY.sub("", text)
                if any(t in remainder for t in ("{{", "}}", "{%", "%}", "{#", "#}")):
                    error("INVALID_MARKER", location, "Marcador mal formado o dividido entre párrafos.")
                spans, offset = [], 0
                for node in nodes:
                    size = len(node.text or "")
                    spans.append((node, offset, offset + size))
                    offset += size
                for match in reversed(matches):
                    key = match.group(1)
                    alias = next((a for a, k in aliases.items() if k == key), None)
                    if alias is None:
                        alias = f"BRICA_FIELD_{len(aliases)}"
                        aliases[alias] = key
                    occurrences.append(dict(key=key, marker=match.group(), location=location))
                    overlapping = [(n, start, end) for n, start, end in spans
                                   if start < match.end() and end > match.start()]
                    for index, (node, start, end) in enumerate(overlapping):
                        before = (node.text or "")[:max(0, match.start() - start)]
                        after = (node.text or "")[min(end, match.end()) - start:]
                        node.text = before + ("{{ " + alias + " }}" if index == 0 else "") + after
                # Field instructions and non-w:t text must not hide a placeholder.
                for node in paragraph.iter():
                    if node.tag in (W + "instrText", W + "delText") and any(
                            t in (node.text or "") for t in ("{{", "}}", "{%", "{#")):
                        error("UNSUPPORTED_REGION", location, "Marcador en instrucción o texto eliminado.")
            modified[e.filename] = etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)
        normalized = io.BytesIO()
        with zipfile.ZipFile(normalized, "w", zipfile.ZIP_DEFLATED) as output:
            for entry in entries:
                output.writestr(entry.filename, modified.get(entry.filename, archive.read(entry)))
    if not diagnostics:
        template = DocxTemplate(io.BytesIO(normalized.getvalue()))
        detected = template.get_undeclared_template_variables()
        if detected != set(aliases):
            error("UNSUPPORTED_REGION", "document", "El procesador no reconoce todos los marcadores.")
    occurrences.sort(key=lambda o: (o["location"], o["key"]))
    return normalized.getvalue(), aliases, dict(occurrences=occurrences, diagnostics=diagnostics,
                                              staticDocument=not occurrences and not diagnostics)


def main():
    try:
        # Bound JSON and base64 before decoding; Java enforces its own smaller upload limit.
        raw = sys.stdin.buffer.read(16 * 1024 * 1024 + 1)
        if len(raw) > 16 * 1024 * 1024:
            raise ValueError("Carga demasiado grande.")
        request = json.loads(raw)
        content = base64.b64decode(request["content"], validate=True)
        _, _, report = prepare(content, request["maxEntries"], request["maxExpandedBytes"])
        print(json.dumps(report, ensure_ascii=True))
    except Exception:
        # Neither parser traces nor internal paths belong in the protocol.
        print(json.dumps(dict(occurrences=[], staticDocument=False, diagnostics=[dict(
            code="INVALID_DOCX", location="file", message="DOCX inválido, inseguro o no procesable.")])))


if __name__ == "__main__":
    main()
