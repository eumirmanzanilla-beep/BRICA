"""Construye fixtures DOCX deterministas, sin dependencias ni datos de producción."""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
from xml.etree import ElementTree as ET
from io import BytesIO
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[3]
OUTPUT = ROOT / 'brica-server/src/test/resources/documents'
W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
R = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'


def paragraph(text):
    return f'<w:p><w:r><w:t>{escape(text)}</w:t></w:r></w:p>'


def package(body, header_footer=False):
    types = ('<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
             '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
             '<Default Extension="xml" ContentType="application/xml"/>'
             '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>')
    parts = {
        '_rels/.rels': '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
                       f'<Relationship Id="rId1" Type="{R}/officeDocument" Target="word/document.xml"/></Relationships>',
    }
    section = '<w:sectPr>'
    if header_footer:
        for name, tag in [('header', 'hdr'), ('footer', 'ftr')]:
            types += f'<Override PartName="/word/{name}1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.{name}+xml"/>'
            marker = '{{NOMBRE_CLIENTE}}' if name == 'header' else '{{NOMBRE_OBRA}}'
            parts[f'word/{name}1.xml'] = f'<w:{tag} xmlns:w="{W}">{paragraph(marker)}</w:{tag}>'
            section += f'<w:{name}Reference w:type="default" r:id="{name}"/>'
        parts['word/_rels/document.xml.rels'] = (
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            f'<Relationship Id="header" Type="{R}/header" Target="header1.xml"/>'
            f'<Relationship Id="footer" Type="{R}/footer" Target="footer1.xml"/></Relationships>')
    section += '<w:pgSz w:w="12240" w:h="15840"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>'
    parts['word/document.xml'] = f'<w:document xmlns:w="{W}" xmlns:r="{R}"><w:body>{body}{section}</w:body></w:document>'
    parts['[Content_Types].xml'] = types + '</Types>'
    output = BytesIO()
    with ZipFile(output, 'w') as archive:
        for name, xml in sorted(parts.items()):
            ET.fromstring(xml)
            info = ZipInfo(name, (2026, 11, 9, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            archive.writestr(info, xml.encode('utf-8'))
    with ZipFile(BytesIO(output.getvalue())) as archive:
        assert archive.testzip() is None
    return output.getvalue()


body = paragraph('Acta de inicio — prototipo técnico BRICA')
body += paragraph('Documento demostrativo pendiente de revisión; no utilizar como formato definitivo.')
body += '<w:p><w:r><w:t>Obra: </w:t></w:r><w:r><w:t>{{NOMBRE_</w:t></w:r><w:r><w:rPr><w:b/></w:rPr><w:t>OBRA}}</w:t></w:r></w:p>'
body += '<w:tbl><w:tblPr/><w:tblGrid><w:gridCol w:w="4500"/><w:gridCol w:w="4500"/></w:tblGrid><w:tr><w:tc>'
body += paragraph('Cliente asociado') + '</w:tc><w:tc>' + paragraph('{{NOMBRE_CLIENTE}}') + '</w:tc></w:tr></w:tbl>'
fixtures = {
    'acta-inicio-prototipo.docx': package(body, True),
    'documento-estatico.docx': package(paragraph('Documento estático de prueba, sin sustituciones.')),
    'marcador-desconocido.docx': package(paragraph('{{CAMPO_NO_REGISTRADO}}')),
    'marcador-entre-parrafos.docx': package(paragraph('{{NOMBRE_') + paragraph('OBRA}}')),
}
OUTPUT.mkdir(parents=True, exist_ok=True)
for name, data in fixtures.items():
    path = OUTPUT / name
    if path.exists() and path.read_bytes() != data:
        raise SystemExit(f'No se sobrescribe un archivo diferente: {path}')
    path.write_bytes(data)
print(f'{len(fixtures)} DOCX de prueba reproducibles en {OUTPUT}; no se registró nada en la base.')
