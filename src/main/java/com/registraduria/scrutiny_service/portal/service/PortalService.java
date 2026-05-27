package com.registraduria.scrutiny_service.portal.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.registraduria.scrutiny_service.official.exception.ResourceNotFoundException;
import com.registraduria.scrutiny_service.portal.dto.PortalResultsResponse;
import com.registraduria.scrutiny_service.portal.entity.PortalPublication;
import com.registraduria.scrutiny_service.portal.repository.PortalPublicationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Gestiona el estado de publicacion del portal publico (CA-4).
 *
 * Antes de oficializar muestra la etiqueta PRELIMINAR; al generarse el acta se
 * elimina toda etiqueta "preliminar" y se muestra el badge verde, accesible
 * WCAG AA, "RESULTADOS OFICIALES" junto al enlace permanente al Acta E-26.
 */
@Service
@RequiredArgsConstructor
public class PortalService {

    public static final String BADGE_PRELIMINARY = "PRELIMINAR";
    public static final String BADGE_OFFICIAL = "RESULTADOS OFICIALES";

    // Verde con contraste suficiente para texto blanco (WCAG AA).
    private static final String OFFICIAL_GREEN = "#1a7f37";
    private static final String PRELIMINARY_AMBER = "#9a6700";

    private final PortalPublicationRepository repository;

    /** Crea/deja el portal en estado preliminar (sin resultados oficiales). */
    @Transactional
    public PortalPublication markPreliminary(String scrutinyCode) {

        PortalPublication pub = repository.findByScrutinyCode(scrutinyCode)
                .orElseGet(() -> PortalPublication.builder()
                        .scrutinyCode(scrutinyCode)
                        .build());

        pub.setOfficial(false);
        pub.setBadgeText(BADGE_PRELIMINARY);
        pub.setActaPermalink(null);
        pub.setUpdatedAt(nowUtc());

        return repository.save(pub);
    }

    /**
     * Publica resultados oficiales: elimina la etiqueta "preliminar", muestra el
     * badge "RESULTADOS OFICIALES" y publica el enlace permanente al acta.
     */
    @Transactional
    public PortalPublication publishOfficial(String scrutinyCode, String actaPermalink) {

        PortalPublication pub = repository.findByScrutinyCode(scrutinyCode)
                .orElseGet(() -> PortalPublication.builder()
                        .scrutinyCode(scrutinyCode)
                        .build());

        pub.setOfficial(true);
        pub.setBadgeText(BADGE_OFFICIAL);
        pub.setActaPermalink(actaPermalink);
        pub.setUpdatedAt(nowUtc());

        return repository.save(pub);
    }

    @Transactional(readOnly = true)
    public PortalResultsResponse getResults(String scrutinyCode) {

        PortalPublication pub = repository.findByScrutinyCode(scrutinyCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portal sin publicacion para escrutinio: " + scrutinyCode));

        return toResponse(pub);
    }

    public PortalResultsResponse toResponse(PortalPublication pub) {

        boolean official = pub.isOfficial();

        return new PortalResultsResponse(
                pub.getScrutinyCode(),
                official,
                pub.getBadgeText(),
                official ? "results-official" : "results-preliminary",
                official ? OFFICIAL_GREEN : PRELIMINARY_AMBER,
                official
                        ? "Resultados oficiales publicados"
                        : "Resultados preliminares, sujetos a escrutinio",
                "AA",
                official, // al ser oficial, ya no queda ninguna etiqueta "preliminar"
                pub.getActaPermalink(),
                pub.getUpdatedAt()
        );
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
