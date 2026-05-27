package com.registraduria.scrutiny_service.candidate.dto;

public record CandidateVoteInput(

        String candidateId,

        String candidateName,

        String party,

        Integer votes
) {
}
