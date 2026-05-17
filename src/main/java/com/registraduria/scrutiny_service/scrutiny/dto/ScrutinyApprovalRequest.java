package com.registraduria.scrutiny_service.scrutiny.dto;

import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyDecision;
import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScrutinyApprovalRequest(

        @NotNull
        ScrutinyLevel level,

        @NotNull
        ScrutinyDecision decision,

        @NotBlank
        String delegateName,

        String alerts
) {
}