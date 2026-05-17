package com.registraduria.scrutiny_service.mesa.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;
import com.registraduria.scrutiny_service.mesa.dto.CloseMesaRequest;
import com.registraduria.scrutiny_service.mesa.dto.CreateMesaRequest;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.pdf.E14PdfService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaCommandService {

    private final MesaRepository mesaRepository;

    private final E14CommandRepository e14Repository;

    private final E14PdfService pdfService;

    public MesaRecord closeMesa(

            Long id,

            CloseMesaRequest request
    ) {

        MesaRecord mesa = mesaRepository.findById(id)
                .orElseThrow();

        if (mesa.getStatus() == MesaStatus.CLOSED) {

            throw new RuntimeException(
                    "Mesa already closed."
            );
        }

        mesa.setValidVotes(
                request.validVotes()
        );

        mesa.setBlankVotes(
                request.blankVotes()
        );

        mesa.setNullVotes(
                request.nullVotes()
        );

        mesa.setUnmarkedVotes(
                request.unmarkedVotes()
        );

        mesa.setStatus(
                MesaStatus.CLOSED
        );

        mesaRepository.save(mesa);

        String pdfPath =
                pdfService.generatePdf(mesa);

        String hash =
                generateFileHash(pdfPath);

        E14Record e14 = E14Record.builder()
                .mesaCode(mesa.getMesaCode())
                .municipality("Bogota")
                .pdfHash(hash)
                .status(E14Status.DRAFT)
                .build();

        e14Repository.save(e14);

        return mesa;
    }

    private String generateFileHash(
            String path
    ) {

        try {

            byte[] fileBytes =
                    Files.readAllBytes(
                            Path.of(path)
                    );

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(fileBytes);

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format("%02x", b)
                );
            }

            return hex.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating file hash."
            );
        }
    }

    public MesaRecord createMesa(
            CreateMesaRequest request
    ) {

        MesaRecord mesa = MesaRecord.builder()
                .mesaCode(request.mesaCode())
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .build();

        return mesaRepository.save(mesa);
    }
}