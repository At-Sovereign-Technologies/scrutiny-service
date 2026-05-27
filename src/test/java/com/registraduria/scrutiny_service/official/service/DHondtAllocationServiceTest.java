package com.registraduria.scrutiny_service.official.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

class DHondtAllocationServiceTest {

    private final DHondtAllocationService service = new DHondtAllocationService();

    @Test
    void allocates_seats_by_dhondt_and_picks_top_candidates_per_party() {
        // Partido A: 105 (A1=60, A2=45); Partido B: 80; Partido C: 30. 3 curules.
        // Cocientes D'Hondt -> A:2, B:1, C:0.
        List<CandidateAggregateSummary> aggregates = List.of(
                new CandidateAggregateSummary("A1", "Ana", "A", 60L),
                new CandidateAggregateSummary("A2", "Aldo", "A", 45L),
                new CandidateAggregateSummary("B1", "Beto", "B", 80L),
                new CandidateAggregateSummary("C1", "Cira", "C", 30L)
        );

        SeatAllocation result = service.allocate(aggregates, 3);

        assertThat(result.totalValidVotes()).isEqualTo(215L);

        assertThat(result.partyResults())
                .extracting(PartyResult::party, PartyResult::votes, PartyResult::seats)
                .containsExactlyInAnyOrder(
                        tuple("A", 105L, 2),
                        tuple("B", 80L, 1),
                        tuple("C", 30L, 0));

        // 3 electos: B1 (80), A1 (60), A2 (45) por votos descendentes.
        assertThat(result.electedCandidates())
                .extracting(ElectedCandidate::candidateId, ElectedCandidate::seatNumber)
                .containsExactly(
                        tuple("B1", 1),
                        tuple("A1", 2),
                        tuple("A2", 3));
    }

    @Test
    void does_not_assign_more_seats_than_party_candidates() {
        // Partido A domina pero solo tiene 1 candidato: solo 1 curul ocupable.
        List<CandidateAggregateSummary> aggregates = List.of(
                new CandidateAggregateSummary("A1", "Ana", "A", 1000L),
                new CandidateAggregateSummary("B1", "Beto", "B", 10L)
        );

        SeatAllocation result = service.allocate(aggregates, 3);

        // A tendria 3 curules por D'Hondt, pero solo hay 1 candidato A.
        assertThat(result.electedCandidates())
                .extracting(ElectedCandidate::candidateId)
                .containsExactly("A1");
    }

    @Test
    void computes_percentages_against_total_valid_votes() {
        List<CandidateAggregateSummary> aggregates = List.of(
                new CandidateAggregateSummary("A1", "Ana", "A", 75L),
                new CandidateAggregateSummary("B1", "Beto", "B", 25L)
        );

        SeatAllocation result = service.allocate(aggregates, 1);

        assertThat(result.totalValidVotes()).isEqualTo(100L);
        assertThat(result.partyResults())
                .filteredOn(p -> p.party().equals("A"))
                .first()
                .extracting(PartyResult::percentage)
                .isEqualTo(75.0);
    }
}
