package com.brica.documents.infrastructure;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DocumentFixturesTest {
    @ParameterizedTest
    @ValueSource(strings = {"acta-inicio-prototipo.docx", "documento-estatico.docx",
            "marcador-desconocido.docx", "marcador-entre-parrafos.docx"})
    void shouldKeepTechnicalFixturesAsWellFormedOoxmlPackages(String name) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Set<String> entries = new HashSet<>();
        try (var input = getClass().getResourceAsStream("/documents/" + name)) {
            assertNotNull(input);
            try (var zip = new ZipInputStream(input)) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    entries.add(entry.getName());
                    assertFalse(entry.getName().contains(".."));
                    assertNotNull(factory.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes())));
                }
            }
        }
        assertTrue(entries.containsAll(Set.of("[Content_Types].xml", "_rels/.rels", "word/document.xml")));
        if (name.equals("acta-inicio-prototipo.docx")) {
            assertTrue(entries.containsAll(Set.of("word/header1.xml", "word/footer1.xml", "word/_rels/document.xml.rels")));
        }
    }
}
