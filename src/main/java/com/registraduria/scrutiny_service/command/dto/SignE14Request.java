package com.registraduria.scrutiny_service.command.dto;

import jakarta.validation.constraints.NotBlank;

public record SignE14Request(

        @NotBlank
        String digitalSignature
) {
}