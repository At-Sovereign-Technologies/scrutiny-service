package com.registraduria.scrutiny_service.official.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.official.dto.ActaE26Response;
import com.registraduria.scrutiny_service.official.dto.E26ActaContent;
import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.exception.ResourceNotFoundException;
import com.registraduria.scrutiny_service.official.repository.ActaE26Repository;

import lombok.RequiredArgsConstructor;

/**
 * Lectura del Acta E-26. CA-3: en cada consulta se reverifica el SHA-256 frente
 * al contenido almacenado; si no coincide, la lectura falla por integridad.
 */
@Service
@RequiredArgsConstructor
public class ActaQueryService {

    private final ActaE26Repository repository;
    private final ActaIntegrityService integrityService;
    private final ObjectMapper objectMapper;

    @Value("${scrutiny.acta.base-url}")
    private String baseUrl;

    @Transactional(readOnly = true)
    public ActaE26Response getById(Long id) {

        ActaE26 acta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Acta no encontrada: " + id));

        // CA-3: reverificacion de integridad en cada lectura.
        integrityService.verifyOrThrow(acta);

        return toResponse(acta, true);
    }

    /** Verificacion explicita de integridad sin lanzar (para auditoria). */
    @Transactional(readOnly = true)
    public boolean verifyIntegrity(Long id) {
        ActaE26 acta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Acta no encontrada: " + id));
        return integrityService.verify(acta);
    }

    /** Descarga el PDF/A-3 del acta; reverifica integridad antes de servir (CA-3). */
    @Transactional(readOnly = true)
    public ActaDownload downloadPdf(Long id) {
        return readFile(loadVerified(id).getPdfPath());
    }

    /** Descarga el XML firmado del acta; reverifica integridad antes de servir (CA-3). */
    @Transactional(readOnly = true)
    public ActaDownload downloadXml(Long id) {
        return readFile(loadVerified(id).getXmlPath());
    }

    private ActaE26 loadVerified(Long id) {
        ActaE26 acta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Acta no encontrada: " + id));
        integrityService.verifyOrThrow(acta);
        return acta;
    }

    private ActaDownload readFile(String storedPath) {
        // La ruta proviene de la BD (la escribio el propio servicio al generar el
        // acta), no de entrada del usuario, por lo que no hay riesgo de traversal.
        Path path = Path.of(storedPath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException(
                    "Archivo del acta no disponible: " + path.getFileName());
        }
        try {
            return new ActaDownload(
                    path.getFileName().toString(),
                    Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error leyendo el archivo del acta.", e);
        }
    }

    private ActaE26Response toResponse(ActaE26 acta, boolean hashVerified) {

        E26ActaContent content;
        try {
            content = objectMapper.readValue(
                    acta.getContentJson(), E26ActaContent.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error deserializando el contenido del acta.", e);
        }

        return new ActaE26Response(
                acta.getId(),
                acta.getActaNumber(),
                acta.getSourceScrutinyCode(),
                acta.getStatus(),
                acta.getElectoralMethod(),
                acta.getContentHash(),
                hashVerified,
                acta.getPdfPath(),
                acta.getXmlPath(),
                baseUrl + "/" + acta.getId(),
                acta.getGeneratedAt(),
                content
        );
    }
}
