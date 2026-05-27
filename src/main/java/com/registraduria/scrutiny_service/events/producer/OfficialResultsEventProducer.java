package com.registraduria.scrutiny_service.events.producer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.OfficialResultsPublishedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Productor del evento de dominio {@code OfficialResultsPublished} (CA-4).
 *
 * Sigue el patron opcional del proyecto: inyecta el {@link KafkaTemplate} via
 * {@link ObjectProvider} y publica solo {@code ifAvailable}, de modo que el
 * servicio arranca aunque la autoconfiguracion de Kafka no este presente.
 */
@Component
@RequiredArgsConstructor
public class OfficialResultsEventProducer {

    public static final String TOPIC = "official.results.published";

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;

    public void publish(OfficialResultsPublishedEvent event) {
        kafkaTemplateProvider.ifAvailable(
                kafkaTemplate -> kafkaTemplate.send(TOPIC, event)
        );
    }
}
