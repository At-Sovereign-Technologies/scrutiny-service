package com.registraduria.scrutiny_service.official.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.candidate.dto.CandidateAggregateSummary;
import com.registraduria.scrutiny_service.official.dto.ElectedCandidate;
import com.registraduria.scrutiny_service.official.dto.PartyResult;

/**
 * Aplica el metodo D'Hondt para asignar curules/dignidades por lista y derivar
 * los candidatos electos (CA-2).
 *
 * D'Hondt: para cada lista se calculan los cocientes votos/1, votos/2, ... ,
 * votos/seats; los {@code seats} cocientes mas altos del conjunto reciben curul.
 * Las curules de cada lista se asignan a sus candidatos mas votados.
 */
@Service
public class DHondtAllocationService {

    public static final String METHOD = "DHONDT";

    public SeatAllocation allocate(
            List<CandidateAggregateSummary> aggregates,
            int seats
    ) {

        long totalValidVotes = aggregates.stream()
                .mapToLong(a -> a.totalVotes() == null ? 0L : a.totalVotes())
                .sum();

        // Votos por lista/partido, preservando orden de aparicion.
        Map<String, Long> votesByParty = new LinkedHashMap<>();
        for (CandidateAggregateSummary a : aggregates) {
            long v = a.totalVotes() == null ? 0L : a.totalVotes();
            votesByParty.merge(a.party(), v, Long::sum);
        }

        Map<String, Integer> seatsByParty =
                computeDHondtSeats(votesByParty, seats);

        List<PartyResult> partyResults = buildPartyResults(
                votesByParty, seatsByParty, totalValidVotes
        );

        List<ElectedCandidate> elected = buildElectedCandidates(
                aggregates, seatsByParty, totalValidVotes
        );

        return new SeatAllocation(totalValidVotes, partyResults, elected);
    }

    /** Reparte {@code seats} curules entre listas por el metodo D'Hondt. */
    private Map<String, Integer> computeDHondtSeats(
            Map<String, Long> votesByParty,
            int seats
    ) {

        record Quotient(String party, double value) {
        }

        List<Quotient> quotients = new ArrayList<>();
        for (Map.Entry<String, Long> e : votesByParty.entrySet()) {
            for (int divisor = 1; divisor <= seats; divisor++) {
                quotients.add(
                        new Quotient(e.getKey(), (double) e.getValue() / divisor)
                );
            }
        }

        // Mayor cociente primero; empate resuelto por mas votos de la lista.
        quotients.sort(
                Comparator.comparingDouble(Quotient::value).reversed()
                        .thenComparing(q -> votesByParty.get(q.party()),
                                Comparator.reverseOrder())
        );

        Map<String, Integer> seatsByParty = new LinkedHashMap<>();
        votesByParty.keySet().forEach(p -> seatsByParty.put(p, 0));

        int assigned = 0;
        for (Quotient q : quotients) {
            if (assigned >= seats) {
                break;
            }
            seatsByParty.merge(q.party(), 1, Integer::sum);
            assigned++;
        }

        return seatsByParty;
    }

    private List<PartyResult> buildPartyResults(
            Map<String, Long> votesByParty,
            Map<String, Integer> seatsByParty,
            long totalValidVotes
    ) {

        return votesByParty.entrySet().stream()
                .map(e -> new PartyResult(
                        e.getKey(),
                        e.getValue(),
                        percentage(e.getValue(), totalValidVotes),
                        seatsByParty.getOrDefault(e.getKey(), 0)
                ))
                .sorted(Comparator.comparingLong(PartyResult::votes).reversed())
                .toList();
    }

    /** Asigna las curules de cada lista a sus candidatos mas votados. */
    private List<ElectedCandidate> buildElectedCandidates(
            List<CandidateAggregateSummary> aggregates,
            Map<String, Integer> seatsByParty,
            long totalValidVotes
    ) {

        // Candidatos por lista, ordenados por votos desc.
        Map<String, List<CandidateAggregateSummary>> byParty = new LinkedHashMap<>();
        for (CandidateAggregateSummary a : aggregates) {
            byParty.computeIfAbsent(a.party(), k -> new ArrayList<>()).add(a);
        }
        byParty.values().forEach(list -> list.sort(
                Comparator.comparingLong(
                        (CandidateAggregateSummary c) ->
                                c.totalVotes() == null ? 0L : c.totalVotes()
                ).reversed()
        ));

        // Lista provisional de electos (sin numero de curul aun).
        List<CandidateAggregateSummary> winners = new ArrayList<>();
        for (Map.Entry<String, List<CandidateAggregateSummary>> e : byParty.entrySet()) {
            int partySeats = seatsByParty.getOrDefault(e.getKey(), 0);
            int take = Math.min(partySeats, e.getValue().size());
            winners.addAll(e.getValue().subList(0, take));
        }

        // Numero de curul global por votos descendentes entre los electos.
        winners.sort(Comparator.comparingLong(
                (CandidateAggregateSummary c) ->
                        c.totalVotes() == null ? 0L : c.totalVotes()
        ).reversed());

        List<ElectedCandidate> elected = new ArrayList<>();
        int seatNumber = 1;
        for (CandidateAggregateSummary c : winners) {
            long votes = c.totalVotes() == null ? 0L : c.totalVotes();
            elected.add(new ElectedCandidate(
                    c.candidateId(),
                    c.candidateName(),
                    c.party(),
                    votes,
                    percentage(votes, totalValidVotes),
                    seatNumber++
            ));
        }

        return elected;
    }

    private double percentage(long votes, long total) {
        if (total <= 0) {
            return 0.0;
        }
        // Redondeo a 2 decimales para estabilidad del hash y presentacion.
        return Math.round((votes * 10000.0) / total) / 100.0;
    }
}
