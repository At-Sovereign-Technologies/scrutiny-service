package com.registraduria.scrutiny_service.events;

/**
 * Evento de dominio emitido al generarse el Acta E-26 oficial (CA-4).
 * Notifica a otros microservicios (portal, results-service, etc.) que los
 * resultados oficiales ya estan publicados.
 *
 * Topic Kafka: {@code official.results.published}.
 */
public record OfficialResultsPublishedEvent(

        String scrutinyCode,

        String actaNumber,

        String actaHash,

        String actaPermalink,

        String electoralMethod,

        String generatedAtUtc
) {
}
