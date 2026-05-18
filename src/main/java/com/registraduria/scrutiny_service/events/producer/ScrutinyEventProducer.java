package com.registraduria.scrutiny_service.events.producer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.ScrutinyApprovedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScrutinyEventProducer {

    private final ObjectProvider<KafkaTemplate<String, Object>>
            kafkaTemplateProvider;

    public void publishApproved(
            ScrutinyApprovedEvent event
    ) {

        kafkaTemplateProvider.ifAvailable(
                kafkaTemplate -> kafkaTemplate.send(
                        "scrutiny.approved",
                        event
                )
        );
    }
}