package com.registraduria.scrutiny_service.events.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.ScrutinyApprovedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScrutinyEventProducer {

    private final KafkaTemplate<String, Object>
            kafkaTemplate;

    public void publishApproved(
            ScrutinyApprovedEvent event
    ) {

        kafkaTemplate.send(
                "scrutiny.approved",
                event
        );
    }
}