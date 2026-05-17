package com.registraduria.scrutiny_service.events.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.events.ScrutinyApprovedEvent;
import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

@Component
public class ScrutinyApprovedConsumer {

    @KafkaListener(
            topics = "scrutiny.approved",
            groupId = "scrutiny-group"
    )
    public void consume(
            ScrutinyApprovedEvent event
    ) {

        System.out.println(
                """
                ================================
                SCRUTINY APPROVED
                Level: %s
                Delegate: %s
                Hash: %s
                ================================
                """
                        .formatted(

                                event.level(),

                                event.delegateName(),

                                event.scrutinyHash()
                        )
        );

        if (event.level()
                == ScrutinyLevel.NATIONAL) {

            System.out.println(
                    """
                    ================================
                    E26 ENABLED
                    National scrutiny approved.
                    ================================
                    """
            );
        }
    }
}