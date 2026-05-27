package com.registraduria.scrutiny_service.official.service;

import java.util.List;

import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

/**
 * Resultado de aplicar el metodo electoral: totales, asignacion de curules por
 * lista y candidatos electos (CA-2).
 */
public record SeatAllocation(

        long totalValidVotes,

        List<PartyResult> partyResults,

        List<ElectedCandidate> electedCandidates
) {
}
