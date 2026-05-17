package com.registraduria.scrutiny_service.events.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.VvpatMismatchEvent;

@Component
public class VvpatMismatchConsumer {

    @KafkaListener(
            topics = "vvpat.mismatch",
            groupId = "scrutiny-group"
    )
    public void consume(
            VvpatMismatchEvent event
    ) {

        System.out.println(
                """
                ================================
                VVPAT MISMATCH DETECTED
                Mesa: %s
                Digital Votes: %d
                Physical Votes: %d
                Attempt: %d
                ================================
                """
                        .formatted(

                                event.mesaCode(),

                                event.digitalVotes(),

                                event.physicalVotes(),

                                event.attempt()
                        )
        );
    }
}