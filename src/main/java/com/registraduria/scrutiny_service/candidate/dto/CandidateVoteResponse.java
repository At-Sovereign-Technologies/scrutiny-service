package com.registraduria.scrutiny_service.candidate.dto;

public record CandidateVoteResponse(

        Long id,

        String mesaCode,

        String candidateId,

        String candidateName,

        String party,

        Integer votes
) {
}
