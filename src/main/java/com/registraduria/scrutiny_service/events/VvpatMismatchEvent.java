package com.registraduria.scrutiny_service.events;

public record VvpatMismatchEvent(

        String mesaCode,

        Integer digitalVotes,

        Integer physicalVotes,

        Integer attempt
) {
}