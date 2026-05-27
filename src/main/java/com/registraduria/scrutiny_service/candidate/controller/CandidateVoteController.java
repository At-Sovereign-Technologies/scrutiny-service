package com.registraduria.scrutiny_service.candidate.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.dto.CandidateVoteResponse;
import com.registraduria.scrutiny_service.candidate.service.CandidateVoteQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate-votes")
@RequiredArgsConstructor
public class CandidateVoteController {

    private final CandidateVoteQueryService queryService;

    @GetMapping("/by-mesa/{mesaCode}")
    public List<CandidateVoteResponse> getByMesa(

            @PathVariable String mesaCode
    ) {

        return queryService.getByMesa(mesaCode);
    }

    @GetMapping("/aggregate")
    public List<CandidateAggregateSummary> aggregateAll() {

        return queryService.aggregateAll();
    }

    @GetMapping("/aggregate/by-mesa/{mesaCode}")
    public List<CandidateAggregateSummary> aggregateByMesa(

            @PathVariable String mesaCode
    ) {

        return queryService.aggregateByMesa(mesaCode);
    }
}
