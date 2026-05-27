package com.registraduria.scrutiny_service.official.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para cerrar el escrutinio general (handoff desde SR-M4) y dejarlo
 * en estado CERRADO, listo para la aprobacion de los magistrados.
 */
public record CloseGeneralScrutinyRequest(

        @NotBlank
        String scrutinyCode,

        /** Numero de aprobaciones de magistrados requeridas (quorum). */
        @NotNull
        @Min(1)
        Integer requiredQuorum,

        /** Numero de curules/dignidades a asignar. */
        @NotNull
        @Min(1)
        Integer seats,

        /** Metodo electoral; por defecto DHONDT si se omite. */
        String electoralMethod
) {
}
