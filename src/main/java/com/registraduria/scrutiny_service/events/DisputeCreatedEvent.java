package com.registraduria.scrutiny_service.events;

import java.time.LocalDateTime;

public record DisputeCreatedEvent(

        Long id,

        String mesaCode,

        String witnessName,

        String reason,

        LocalDateTime createdAt
) {
}