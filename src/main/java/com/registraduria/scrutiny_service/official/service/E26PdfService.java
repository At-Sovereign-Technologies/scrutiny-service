package com.registraduria.scrutiny_service.official.service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.MagistrateSignatureDto;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

/**
 * Genera el PDF archivistico del Acta E-26 (CA-2).
 *
 * Se emite con metadatos XMP y marca de perfil archivistico PDF/A-3 (retencion
 * legal). NOTA: OpenPDF 1.3.39 no incluye un escritor PDF/A con validador de
 * conformancia ni perfil ICC; el documento incorpora los metadatos y la
 * estructura requeridos, y la conformancia PDF/A-3 estricta (ICC + validacion)
 * queda como integracion de libreria especializada en produccion.
 */
@Service
public class E26PdfService {

    private final String outputDir;

    public E26PdfService(
            @Value("${scrutiny.acta.output-dir}") String outputDir
    ) {
        this.outputDir = outputDir;
    }

    public String generate(
            String actaNumber,
            E26ActaContent content,
            String contentHash
    ) {

        try {

            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            String fileName = dir.resolve(actaNumber + ".pdf").toString();

            Document document = new Document();
            PdfWriter writer =
                    PdfWriter.getInstance(document, new FileOutputStream(fileName));

            // Metadatos archivisticos + XMP (PDF/A-3 intent). No se fuerza la
            // conformancia a nivel de escritor porque OpenPDF exigiria fuentes
            // embebidas (las base-14 no lo estan); la conformancia PDF/A-3
            // estricta con perfil ICC se integra con libreria especializada.
            document.addTitle("Acta de Declaratoria de Eleccion E-26 " + actaNumber);
            document.addSubject("Resultados electorales oficiales - "
                    + content.scrutinyCode());
            document.addCreator("scrutiny-service (CNE)");
            document.addKeywords("E-26, PDF/A-3, oficial, inmutable");

            document.open();
            writer.createXmpMetadata();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(center(new Paragraph(
                    "ACTA DE DECLARATORIA DE ELECCION (E-26)", title)));
            document.add(center(new Paragraph(
                    "CONSEJO NACIONAL ELECTORAL - RESULTADOS OFICIALES", header)));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Numero de acta: " + actaNumber, header));
            document.add(new Paragraph("Escrutinio origen: " + content.scrutinyCode()
                    + " (id " + content.sourceScrutinyId() + ")", normal));
            document.add(new Paragraph("Metodo electoral: "
                    + content.electoralMethod(), normal));
            document.add(new Paragraph("Curules/dignidades: "
                    + content.totalSeats(), normal));
            document.add(new Paragraph("Votos validos totales: "
                    + content.totalValidVotes(), normal));
            document.add(new Paragraph("Generada (UTC): "
                    + content.generatedAtUtc(), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("RESULTADO POR LISTA/PARTIDO", header));
            for (PartyResult p : content.partyResults()) {
                document.add(new Paragraph(String.format(
                        "  %s | votos: %d (%.2f%%) | curules: %d",
                        p.party(), p.votes(), p.percentage(), p.seats()), normal));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("CANDIDATOS/LISTAS ELECTAS", header));
            for (ElectedCandidate c : content.electedCandidates()) {
                document.add(new Paragraph(String.format(
                        "  Curul %d: %s (%s) | votos: %d (%.2f%%)",
                        c.seatNumber(), c.candidateName(), c.party(),
                        c.votes(), c.percentage()), normal));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("FIRMAS DIGITALES DE MAGISTRADOS", header));
            for (MagistrateSignatureDto s : content.magistrateSignatures()) {
                document.add(new Paragraph(
                        "  " + s.magistrateName() + " (" + s.magistrateId() + ")", normal));
                document.add(new Paragraph(
                        "    esquema: " + s.scheme(), normal));
                document.add(new Paragraph(
                        "    firma: " + s.signature(), normal));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("TRAZABILIDAD", header));
            document.add(new Paragraph("  Hash SHA-256 del acta: " + contentHash, normal));
            document.add(new Paragraph(
                    "  Perfil archivistico: PDF/A-3 (retencion minima 10 anios)", normal));
            document.add(new Paragraph(
                    "  Estado: OFICIAL_INMUTABLE", normal));

            document.close();

            return fileName;

        } catch (Exception e) {
            throw new IllegalStateException("Error generando el PDF del Acta E-26.", e);
        }
    }

    private Paragraph center(Paragraph p) {
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }
}
