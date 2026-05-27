package com.registraduria.scrutiny_service.mesa.dto;

import java.util.List;

import com.registraduria.scrutiny_service.candidate.dto.CandidateVoteInput;

public record CloseMesaRequest(

        Integer validVotes,

        Integer blankVotes,

        Integer nullVotes,

        Integer unmarkedVotes,

        List<CandidateVoteInput> candidateVotes
) {
}