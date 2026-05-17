package com.registraduria.scrutiny_service.events.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.VvpatMismatchEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VvpatEventProducer {

    private final KafkaTemplate<String, Object>
            kafkaTemplate;

    public void publishMismatch(
            VvpatMismatchEvent event
    ) {

        kafkaTemplate.send(
                "vvpat.mismatch",
                event
        );
    }
}