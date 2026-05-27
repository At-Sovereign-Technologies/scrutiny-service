package com.registraduria.scrutiny_service.official.dto;

/**
 * Resultado final por lista/partido segun el metodo electoral aplicado (CA-2).
 */
public record PartyResult(

        String party,

        long votes,

        double percentage,

        int seats
) {
}
