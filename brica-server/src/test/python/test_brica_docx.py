import io
from pathlib import Path
import sys
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "main/python"))
from brica_docx import prepare
from docxtpl import DocxTemplate

FIXTURES = Path(__file__).resolve().parents[1] / "resources/documents"


def modify(content, part, change):
    out = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(content)) as src, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as dst:
        for name in src.namelist():
            dst.writestr(name, change(src.read(name)) if name == part else src.read(name))
    return out.getvalue()


class DocxTests(unittest.TestCase):
    def setUp(self):
        self.content = (FIXTURES / "acta-inicio-prototipo.docx").read_bytes()

    def test_repeated_split_runs_table_header_footer_and_real_docxtpl_compatibility(self):
        original = bytes(self.content)
        normalized, aliases, report = prepare(original)
        self.assertEqual([], report["diagnostics"])
        self.assertEqual(4, len(report["occurrences"]))
        self.assertEqual({"NOMBRE_OBRA", "NOMBRE_CLIENTE"}, set(aliases.values()))
        self.assertEqual(original, self.content)
        template = DocxTemplate(io.BytesIO(normalized))
        template.render({a: "Obra <A> & Cliente" for a in aliases}, autoescape=True)
        output = io.BytesIO()
        template.save(output)
        with zipfile.ZipFile(output) as archive:
            for name in ["word/document.xml", "word/header1.xml", "word/footer1.xml"]:
                data = archive.read(name)
                self.assertNotIn(b"{{", data)
                self.assertIn(b"&lt;A&gt; &amp;", data)

    def test_static_is_explicit_candidate(self):
        _, _, report = prepare((FIXTURES / "documento-estatico.docx").read_bytes())
        self.assertTrue(report["staticDocument"])

    def test_cross_paragraph_marker_is_invalid_not_static(self):
        _, _, report = prepare((FIXTURES / "marcador-entre-parrafos.docx").read_bytes())
        self.assertFalse(report["staticDocument"])
        self.assertEqual(2, len(report["diagnostics"]))

    def test_exact_50_character_key_and_numeric_keys(self):
        for key in ["A" * 50, "123", "_0"]:
            content = modify(self.content, "word/header1.xml", lambda b: b.replace(b"NOMBRE_CLIENTE", key.encode()))
            _, aliases, report = prepare(content)
            self.assertEqual([], report["diagnostics"])
            self.assertIn(key, aliases.values())

    def test_noncanonical_markers_and_jinja_are_blocked(self):
        for marker in [b"{{lower}}", b"{{ A }}", b"{{" + b"A" * 51 + b"}}", b"{% if x %}",
                       b"{{x.__class__}}", b"{# comment #}", b"{{A|e}}"]:
            with self.subTest(marker=marker):
                content = modify(self.content, "word/header1.xml", lambda b: b.replace(b"{{NOMBRE_CLIENTE}}", marker))
                _, _, report = prepare(content)
                self.assertTrue(report["diagnostics"])
                self.assertFalse(report["staticDocument"])

    def test_text_box_and_field_instructions_are_not_silently_ignored(self):
        for fragment in [b"<w:r><w:instrText>{{HIDDEN}}</w:instrText></w:r>",
                         b'<w:fldSimple w:instr="{{123}}"><w:r><w:t>Literal</w:t></w:r></w:fldSimple>',
                         b"<w:r><w:txbxContent><w:p><w:r><w:t>{{HIDDEN}}</w:t></w:r></w:p></w:txbxContent></w:r>"]:
            content = modify(self.content, "word/header1.xml", lambda b: b.replace(b"<w:r><w:t>{{NOMBRE_CLIENTE}}</w:t></w:r>", fragment))
            _, _, report = prepare(content)
            self.assertTrue(any(d["code"] == "UNSUPPORTED_REGION" for d in report["diagnostics"]))

    def test_notes_are_not_silently_ignored(self):
        out = io.BytesIO()
        with zipfile.ZipFile(io.BytesIO(self.content)) as src, zipfile.ZipFile(out, "w") as dst:
            for n in src.namelist():
                dst.writestr(n, src.read(n))
            dst.writestr("word/comments.xml", '<comments><text>{{HIDDEN}}</text></comments>')
        _, _, report = prepare(out.getvalue())
        self.assertTrue(any(d["code"] == "UNSUPPORTED_REGION" for d in report["diagnostics"]))

    def test_external_relationships_and_xml_entities_are_rejected(self):
        bad = modify(self.content, "word/_rels/document.xml.rels", lambda b: b.replace(b"</Relationships>",
                     b'<Relationship Id="evil" Target="http://localhost/private" TargetMode="External"/></Relationships>'))
        with self.assertRaises(ValueError):
            prepare(bad)
        bad = modify(self.content, "word/header1.xml", lambda b: b'<!DOCTYPE a [<!ENTITY x SYSTEM "file:///etc/passwd">]>' + b)
        with self.assertRaises(ValueError):
            prepare(bad)

    def test_archive_traversal_duplicates_and_limits(self):
        for name in ["../evil.xml", "/evil.xml", "word/document.xml"]:
            out = io.BytesIO()
            with zipfile.ZipFile(io.BytesIO(self.content)) as src, zipfile.ZipFile(out, "w") as dst:
                for n in src.namelist():
                    dst.writestr(n, src.read(n))
                dst.writestr(name, "<evil/>")
            with self.assertRaises(ValueError):
                prepare(out.getvalue())
        with self.assertRaises(ValueError):
            prepare(self.content, max_entries=1)
        with self.assertRaises(ValueError):
            prepare(self.content, max_expanded=100)


if __name__ == "__main__":
    unittest.main()
