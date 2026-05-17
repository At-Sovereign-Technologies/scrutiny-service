package com.registraduria.scrutiny_service.events;

import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

public record ScrutinyApprovedEvent(

        ScrutinyLevel level,

        String delegateName,

        String scrutinyHash
) {
}