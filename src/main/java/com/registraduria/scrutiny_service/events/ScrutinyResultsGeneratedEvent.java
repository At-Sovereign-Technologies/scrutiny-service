package com.registraduria.scrutiny_service.events;

import java.util.List;

import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

public record ScrutinyResultsGeneratedEvent(

        ScrutinyLevel scrutinyLevel,

        List<CandidateVoteData> candidateResults
) {
}
