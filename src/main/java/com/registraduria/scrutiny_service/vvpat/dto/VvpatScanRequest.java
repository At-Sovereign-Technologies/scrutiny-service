package com.registraduria.scrutiny_service.vvpat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VvpatScanRequest(

        @NotBlank
        String mesaCode,

        @NotBlank
        String juradoId,

        @NotNull
        Integer physicalVotes
) {
}