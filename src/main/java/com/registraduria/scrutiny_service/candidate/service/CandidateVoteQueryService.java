package com.registraduria.scrutiny_service.candidate.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.dto.CandidateVoteResponse;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateVoteQueryService {

    private final CandidateVoteRepository repository;

    public List<CandidateVoteResponse> getByMesa(String mesaCode) {

        return repository.findByMesaCode(mesaCode)
                .stream()
                .map(r -> new CandidateVoteResponse(
                        r.getId(),
                        r.getMesaCode(),
                        r.getCandidateId(),
                        r.getCandidateName(),
                        r.getParty(),
                        r.getVotes()
                ))
                .toList();
    }

    public List<CandidateAggregateSummary> aggregateAll() {
        return repository.aggregateAllCandidates();
    }

    public List<CandidateAggregateSummary> aggregateByMesa(String mesaCode) {
        return repository.aggregateByMesa(mesaCode);
    }
}
