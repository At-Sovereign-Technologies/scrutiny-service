package com.registraduria.scrutiny_service.official.dto;

/**
 * Candidato electo con sus votos, porcentaje y la curul asignada (CA-2).
 */
public record ElectedCandidate(

        String candidateId,

        String candidateName,

        String party,

        long votes,

        double percentage,

        /** Numero de curul/dignidad asignada (1..seats). */
        int seatNumber
) {
}
