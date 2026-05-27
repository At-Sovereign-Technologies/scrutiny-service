package com.registraduria.scrutiny_service.official.dto;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.official.enums.ActaStatus;

/**
 * Representacion de lectura del Acta E-26. {@code hashVerified} refleja la
 * reverificacion del SHA-256 frente al contenido almacenado (CA-3): si es
 * false, el acta fue manipulada en almacenamiento.
 */
public record ActaE26Response(

        Long id,

        String actaNumber,

        String sourceScrutinyCode,

        ActaStatus status,

        String electoralMethod,

        String contentHash,

        boolean hashVerified,

        String pdfPath,

        String xmlPath,

        String permalink,

        LocalDateTime generatedAt,

        E26ActaContent content
) {
}
