package com.registraduria.scrutiny_service.official.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.MagistrateSignatureDto;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

/**
 * CA-2: el acta se materializa en PDF (archivistico) y en XML firmado. Aqui se
 * comprueba que el PDF se genera y que la firma XML-DSig del XML es valida
 * (verificable e interoperable).
 */
class E26DocumentGenerationTest {

    private final ActaSigningKeyProvider keyProvider = new ActaSigningKeyProvider();

    private E26ActaContent sampleContent() {
        return new E26ActaContent(
                "NAC-2026", 1L, "DHONDT", 100L, 1,
                List.of(new PartyResult("A", 100L, 100.0, 1)),
                List.of(new ElectedCandidate("A1", "Ana", "A", 100L, 100.0, 1)),
                List.of(new MagistrateSignatureDto(
                        "MAG-1", "Magistrada Uno", "c2ln", "2026-05-27T00:00:00Z", "RSA-SHA256")),
                "2026-05-27T00:00:00Z");
    }

    @Test
    void pdf_is_generated_and_non_empty(@TempDir Path dir) {
        E26PdfService pdf = new E26PdfService(dir.toString());

        String path = pdf.generate("E26-000001", sampleContent(), "deadbeef");

        File file = new File(path);
        assertThat(file).exists();
        assertThat(file.length()).isPositive();
    }

    @Test
    void signed_xml_has_a_valid_xmldsig_signature(@TempDir Path dir) throws Exception {
        E26XmlSignatureService xml = new E26XmlSignatureService(keyProvider);

        String path = xml.generate("E26-000001", sampleContent(), "deadbeef", dir.toString());

        // Reparsear y validar la firma con la clave publica del firmante.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new File(path));

        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertThat(nl.getLength()).isEqualTo(1);

        DOMValidateContext valCtx =
                new DOMValidateContext(keyProvider.getKeyPair().getPublic(), nl.item(0));
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = fac.unmarshalXMLSignature(valCtx);

        assertThat(signature.validate(valCtx)).isTrue();
    }
}
