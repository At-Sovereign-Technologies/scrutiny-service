package com.registraduria.scrutiny_service.portal.dto;

import java.time.LocalDateTime;

/**
 * Estado publico del portal para un escrutinio (CA-4).
 *
 * Antes de oficializar: official=false, badge "PRELIMINAR".
 * Tras generar el acta: official=true, badge "RESULTADOS OFICIALES" (verde,
 * accesible WCAG AA) y enlace permanente al Acta E-26 publicado.
 */
public record PortalResultsResponse(

        String scrutinyCode,

        boolean official,

        /** Texto del badge mostrado al publico. */
        String badgeText,

        /** Clase CSS del badge (sin la palabra "preliminar" cuando es oficial). */
        String badgeCssClass,

        /** Color del badge; verde con contraste suficiente para WCAG AA. */
        String badgeColorHex,

        /** Etiqueta accesible para lectores de pantalla. */
        String ariaLabel,

        /** Nivel de conformidad de accesibilidad declarado. */
        String wcagLevel,

        /** true cuando ya no queda ninguna etiqueta "preliminar" visible. */
        boolean preliminaryLabelsRemoved,

        /** Enlace permanente al Acta E-26 (null mientras sea preliminar). */
        String actaPermalink,

        LocalDateTime updatedAt
) {
}
