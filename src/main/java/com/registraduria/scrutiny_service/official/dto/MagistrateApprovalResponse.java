package com.registraduria.scrutiny_service.official.dto;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.official.enums.GeneralScrutinyStatus;

/**
 * Resultado de registrar la aprobacion de un magistrado, indicando si con esta
 * aprobacion se alcanzo el quorum y se genero el Acta E-26.
 */
public record MagistrateApprovalResponse(

        Long approvalId,

        String scrutinyCode,

        String magistrateId,

        String magistrateName,

        LocalDateTime approvedAtUtc,

        long approvalsCount,

        int requiredQuorum,

        boolean quorumReached,

        GeneralScrutinyStatus scrutinyStatus,

        /** Numero del acta si se genero con esta aprobacion; null en caso contrario. */
        String actaNumber
) {
}
