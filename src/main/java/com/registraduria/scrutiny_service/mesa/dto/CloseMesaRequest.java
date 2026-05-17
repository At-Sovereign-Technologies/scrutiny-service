package com.registraduria.scrutiny_service.mesa.dto;

public record CloseMesaRequest(

        Integer validVotes,

        Integer blankVotes,

        Integer nullVotes,

        Integer unmarkedVotes
) {
}