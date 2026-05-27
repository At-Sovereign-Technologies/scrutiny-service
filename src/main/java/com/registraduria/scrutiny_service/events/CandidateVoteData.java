package com.registraduria.scrutiny_service.events;

public record CandidateVoteData(

        String candidateId,

        String candidateName,

        String party,

        Long totalVotes
) {
}
