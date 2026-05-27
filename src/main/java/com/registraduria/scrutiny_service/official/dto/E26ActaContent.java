package com.registraduria.scrutiny_service.official.dto;

import java.util.List;

/**
 * Contenido canonico del Acta E-26 (CA-2). Se serializa de forma determinista a
 * JSON y sobre esa serializacion se calcula el SHA-256 de integridad (CA-3).
 *
 * Importante para la reverificacion del hash: este record NO incluye el propio
 * hash ni rutas de archivo; representa unicamente el contenido firmado/sellado.
 */
public record E26ActaContent(

        String scrutinyCode,

        Long sourceScrutinyId,

        String electoralMethod,

        long totalValidVotes,

        int totalSeats,

        List<PartyResult> partyResults,

        List<ElectedCandidate> electedCandidates,

        List<MagistrateSignatureDto> magistrateSignatures,

        /** Timestamp de generacion en UTC (ISO-8601). */
        String generatedAtUtc
) {
}
