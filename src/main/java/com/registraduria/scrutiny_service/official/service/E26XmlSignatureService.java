package com.registraduria.scrutiny_service.official.service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.MagistrateSignatureDto;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

import lombok.RequiredArgsConstructor;

/**
 * Genera el XML del Acta E-26 con firma digital XML-DSig enveloped (CA-2).
 *
 * Usa la API de firma XML de la JDK (javax.xml.crypto.dsig) con RSA-SHA256 y
 * publica la clave publica en KeyInfo, produciendo un documento verificable e
 * interoperable. Esta firma XML-DSig es la base tecnica de XAdES-BES (que la
 * extiende con propiedades calificadoras y sello de tiempo en produccion).
 */
@Service
@RequiredArgsConstructor
public class E26XmlSignatureService {

    private final ActaSigningKeyProvider keyProvider;

    public String generate(
            String actaNumber,
            E26ActaContent content,
            String contentHash,
            String outputDir
    ) {

        try {

            Document doc = buildDocument(actaNumber, content, contentHash);

            signEnveloped(doc, keyProvider.getKeyPair());

            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            String fileName = dir.resolve(actaNumber + ".xml").toString();

            writeToFile(doc, fileName);

            return fileName;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error generando el XML firmado del Acta E-26.", e);
        }
    }

    private Document buildDocument(
            String actaNumber,
            E26ActaContent content,
            String contentHash
    ) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // requerido por XML-DSig
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("ActaE26");
        root.setAttribute("actaNumber", actaNumber);
        root.setAttribute("status", "OFICIAL_INMUTABLE");
        doc.appendChild(root);

        appendText(doc, root, "scrutinyCode", content.scrutinyCode());
        appendText(doc, root, "sourceScrutinyId",
                String.valueOf(content.sourceScrutinyId()));
        appendText(doc, root, "electoralMethod", content.electoralMethod());
        appendText(doc, root, "totalValidVotes",
                String.valueOf(content.totalValidVotes()));
        appendText(doc, root, "totalSeats", String.valueOf(content.totalSeats()));
        appendText(doc, root, "generatedAtUtc", content.generatedAtUtc());
        appendText(doc, root, "contentHash", contentHash);

        Element parties = doc.createElement("partyResults");
        root.appendChild(parties);
        for (PartyResult p : content.partyResults()) {
            Element pe = doc.createElement("party");
            pe.setAttribute("name", p.party());
            pe.setAttribute("votes", String.valueOf(p.votes()));
            pe.setAttribute("percentage", String.valueOf(p.percentage()));
            pe.setAttribute("seats", String.valueOf(p.seats()));
            parties.appendChild(pe);
        }

        Element elected = doc.createElement("electedCandidates");
        root.appendChild(elected);
        for (ElectedCandidate c : content.electedCandidates()) {
            Element ce = doc.createElement("candidate");
            ce.setAttribute("seatNumber", String.valueOf(c.seatNumber()));
            ce.setAttribute("candidateId", c.candidateId());
            ce.setAttribute("name", c.candidateName());
            ce.setAttribute("party", c.party());
            ce.setAttribute("votes", String.valueOf(c.votes()));
            ce.setAttribute("percentage", String.valueOf(c.percentage()));
            elected.appendChild(ce);
        }

        Element signatures = doc.createElement("magistrateSignatures");
        root.appendChild(signatures);
        for (MagistrateSignatureDto s : content.magistrateSignatures()) {
            Element se = doc.createElement("magistrate");
            se.setAttribute("id", s.magistrateId());
            se.setAttribute("name", s.magistrateName());
            se.setAttribute("scheme", s.scheme());
            se.setAttribute("signedAtUtc", s.signedAtUtc());
            se.setTextContent(s.signature());
            signatures.appendChild(se);
        }

        return doc;
    }

    /** Firma enveloped XML-DSig (RSA-SHA256) sobre todo el documento. */
    private void signEnveloped(Document doc, KeyPair keyPair) throws Exception {

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(
                        fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                null
        );

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(
                        CanonicalizationMethod.INCLUSIVE,
                        (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                Collections.singletonList(ref)
        );

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyValue kv = kif.newKeyValue(keyPair.getPublic());
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

        DOMSignContext dsc =
                new DOMSignContext(keyPair.getPrivate(), doc.getDocumentElement());

        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);
    }

    private void writeToFile(Document doc, String fileName) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            transformer.transform(new DOMSource(doc), new StreamResult(fos));
        }
    }

    private void appendText(Document doc, Element parent, String tag, String value) {
        Element e = doc.createElement(tag);
        e.setTextContent(value);
        parent.appendChild(e);
    }
}
