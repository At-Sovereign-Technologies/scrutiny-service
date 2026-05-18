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

    /**
     * Crear mesa: INICIAL → OPEN
     */
    public MesaRecord createMesa(CreateMesaRequest request) {
        MesaRecord mesa = MesaRecord.builder()
                .mesaCode(request.mesaCode())
                .validVotes(0)
                .blankVotes(0)
                .nullVotes(0)
                .unmarkedVotes(0)
                .status(MesaStatus.OPEN)
                .quarantined(false)
                .build();

        return mesaRepository.save(mesa);
    }

    /**
     * Cerrar mesa: OPEN → CLOSED
     * Validaciones:
     *   - Mesa debe estar en estado OPEN
     *   - Se registran los votos finales
     *   - Se genera acta E14 en estado DRAFT
     */
    public MesaRecord closeMesa(Long id, CloseMesaRequest request) {
        MesaRecord mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa not found: " + id));

        // FSM Validation: Solo OPEN → CLOSED
        if (mesa.getStatus() != MesaStatus.OPEN) {
            throw new IllegalStateException(
                String.format("Invalid state transition: %s → CLOSED. Expected: OPEN", mesa.getStatus())
            );
        }

        mesa.setValidVotes(request.validVotes());
        mesa.setBlankVotes(request.blankVotes());
        mesa.setNullVotes(request.nullVotes());
        mesa.setUnmarkedVotes(request.unmarkedVotes());
        mesa.setStatus(MesaStatus.CLOSED);

        mesaRepository.save(mesa);

        String pdfPath = pdfService.generatePdf(mesa);
        String hash = generateFileHash(pdfPath);

        E14Record e14 = E14Record.builder()
                .mesaCode(mesa.getMesaCode())
                .municipality("Bogota")
                .pdfHash(hash)
                .status(E14Status.DRAFT)
                .build();

        e14Repository.save(e14);

        return mesa;
    }

    /**
     * Verificar mesa: CLOSED → VERIFIED
     * Validaciones:
     *   - Mesa debe estar en estado CLOSED
     *   - Indica que VVPAT ha sido validado exitosamente
     *   - Prepara para sellado de acta
     */
    public MesaRecord verifyMesa(Long id) {
        MesaRecord mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa not found: " + id));

        // FSM Validation: Solo CLOSED → VERIFIED
        if (mesa.getStatus() != MesaStatus.CLOSED) {
            throw new IllegalStateException(
                String.format("Invalid state transition: %s → VERIFIED. Expected: CLOSED", mesa.getStatus())
            );
        }

        mesa.setStatus(MesaStatus.VERIFIED);
        return mesaRepository.save(mesa);
    }

    /**
     * Sellar mesa: VERIFIED → SEALED
     * Validaciones:
     *   - Mesa debe estar en estado VERIFIED
     *   - Transición final e inmutable
     *   - E14 cambia a estado SEALED
     */
    public MesaRecord sealMesa(Long id) {
        MesaRecord mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa not found: " + id));

        // FSM Validation: Solo VERIFIED → SEALED
        if (mesa.getStatus() != MesaStatus.VERIFIED) {
            throw new IllegalStateException(
                String.format("Invalid state transition: %s → SEALED. Expected: VERIFIED", mesa.getStatus())
            );
        }

        mesa.setStatus(MesaStatus.SEALED);
        mesaRepository.save(mesa);

        // Actualizar E14 a SEALED
        E14Record e14 = e14Repository.findByMesaCode(mesa.getMesaCode())
                .orElseThrow(() -> new IllegalArgumentException("E14 not found for mesa: " + mesa.getMesaCode()));
        e14.setStatus(E14Status.SEALED);
        e14Repository.save(e14);

        return mesa;
    }

    /**
     * Poner en cuarentena: * → QUARANTINED
     * Validaciones:
     *   - Puede desde cualquier estado excepto SEALED (irrevocable)
     *   - Registra irregularidad (usa flag quarantined)
     *   - Previene transiciones posteriores
     */
    public MesaRecord quarantineMesa(Long id, String reason) {
        MesaRecord mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa not found: " + id));

        // FSM Validation: No se puede cuarentinar desde SEALED
        if (mesa.getStatus() == MesaStatus.SEALED) {
            throw new IllegalStateException(
                String.format("Cannot quarantine SEALED mesa. Current state: %s", mesa.getStatus())
            );
        }

        mesa.setStatus(MesaStatus.QUARANTINED);
        mesa.setQuarantined(true);
        return mesaRepository.save(mesa);
    }

    private String generateFileHash(String path) {
        try {
            byte[] fileBytes = Files.readAllBytes(Path.of(path));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error generating file hash.", e);
        }
    }
}
