package com.registraduria.scrutiny_service.events.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.DisputeCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DisputeEventConsumer {

    @KafkaListener(

            topics = "dispute.created",

            groupId = "scrutiny-group"
    )
    public void consume(
            DisputeCreatedEvent event
    ) {

        log.info(

                """
                NEW DISPUTE EVENT RECEIVED

                mesaCode: {}
                witness: {}
                reason: {}
                """,

                event.mesaCode(),

                event.witnessName(),

                event.reason()
        );
    }
}