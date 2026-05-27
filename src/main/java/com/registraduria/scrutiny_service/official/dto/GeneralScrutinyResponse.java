package com.registraduria.scrutiny_service.official.dto;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.official.enums.GeneralScrutinyStatus;

public record GeneralScrutinyResponse(

        Long id,

        String scrutinyCode,

        GeneralScrutinyStatus status,

        String electoralMethod,

        Integer seats,

        Integer requiredQuorum,

        long approvalsCount,

        String stateHash,

        LocalDateTime createdAt,

        LocalDateTime officializedAt
) {
}
