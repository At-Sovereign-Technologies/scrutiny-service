package com.registraduria.scrutiny_service.candidate.dto;

public record CandidateAggregateSummary(

        String candidateId,

        String candidateName,

        String party,

        Long totalVotes
) {
}
