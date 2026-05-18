package com.registraduria.scrutiny_service.events.producer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.VvpatMismatchEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VvpatEventProducer {

    private final ObjectProvider<KafkaTemplate<String, Object>>
            kafkaTemplateProvider;

    public void publishMismatch(
            VvpatMismatchEvent event
    ) {

        kafkaTemplateProvider.ifAvailable(
                kafkaTemplate -> kafkaTemplate.send(
                        "vvpat.mismatch",
                        event
                )
        );
    }
}