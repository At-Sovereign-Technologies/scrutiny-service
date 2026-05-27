package com.registraduria.scrutiny_service.candidate.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.registraduria.scrutiny_service.candidate.dto.CandidateVoteInput;
import com.registraduria.scrutiny_service.candidate.entity.CandidateVoteRecord;
import com.registraduria.scrutiny_service.candidate.repository.CandidateVoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateVoteCommandService {

    private final CandidateVoteRepository repository;

    @Transactional
    public List<CandidateVoteRecord> saveCandidateVotes(

            String mesaCode,

            List<CandidateVoteInput> inputs
    ) {

        List<CandidateVoteRecord> records = inputs.stream()

                .map(input -> CandidateVoteRecord.builder()
                        .mesaCode(mesaCode)
                        .candidateId(input.candidateId())
                        .candidateName(input.candidateName())
                        .party(input.party())
                        .votes(input.votes())
                        .build())

                .toList();

        return repository.saveAll(records);
    }
}
