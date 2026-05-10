package com.registraduria.scrutiny_service.command.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateE14Request(

    @NotBlank
    String mesaCode,

    @NotBlank
    String municipality,

    @NotBlank
    String pdfHash
    
) {

}