package com.registraduria.scrutiny_service.candidate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.candidate.entity.CandidateVoteRecord;

public interface CandidateVoteRepository
        extends JpaRepository<CandidateVoteRecord, Long> {

    List<CandidateVoteRecord> findByMesaCode(String mesaCode);

    @Query("""
            SELECT new com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary(
                c.candidateId,
                c.candidateName,
                c.party,
                SUM(c.votes)
            )
            FROM CandidateVoteRecord c
            GROUP BY c.candidateId, c.candidateName, c.party
            ORDER BY SUM(c.votes) DESC
            """)
    List<CandidateAggregateSummary> aggregateAllCandidates();

    @Query("""
            SELECT new com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary(
                c.candidateId,
                c.candidateName,
                c.party,
                SUM(c.votes)
            )
            FROM CandidateVoteRecord c
            WHERE c.mesaCode = :mesaCode
            GROUP BY c.candidateId, c.candidateName, c.party
            ORDER BY SUM(c.votes) DESC
            """)
    List<CandidateAggregateSummary> aggregateByMesa(String mesaCode);
}
