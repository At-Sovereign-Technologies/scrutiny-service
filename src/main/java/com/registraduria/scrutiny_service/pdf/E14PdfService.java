package com.registraduria.scrutiny_service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Path;

@Service
public class E14PdfService {

    public String generatePdf(
            MesaRecord mesa
    ) {

        try {

            String fileName =
                    "generated-pdfs/e14_" +
                    mesa.getMesaCode() +
                    ".pdf";

            Document document =
                    new Document();

            PdfWriter.getInstance(

                    document,

                    new FileOutputStream(fileName)
            );

            document.open();

            document.add(
                    new Paragraph(
                            "ACTA E14"
                    )
            );

            document.add(
                    new Paragraph(
                            "Mesa: " +
                            mesa.getMesaCode()
                    )
            );

            document.add(
                    new Paragraph(
                            "Votos validos: " +
                            mesa.getValidVotes()
                    )
            );

            document.add(
                    new Paragraph(
                            "Votos blancos: " +
                            mesa.getBlankVotes()
                    )
            );

            document.add(
                    new Paragraph(
                            "Votos nulos: " +
                            mesa.getNullVotes()
                    )
            );

            document.add(
                    new Paragraph(
                            "No marcados: " +
                            mesa.getUnmarkedVotes()
                    )
            );

            document.close();

            return fileName;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating PDF."
            );
        }
    }
}