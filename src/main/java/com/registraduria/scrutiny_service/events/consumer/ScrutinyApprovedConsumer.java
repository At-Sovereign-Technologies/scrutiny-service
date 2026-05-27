package com.registraduria.scrutiny_service.events.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.registraduria.scrutiny_service.candidate.service.CandidateVoteQueryService;
import com.registraduria.scrutiny_service.events.CandidateVoteData;
import com.registraduria.scrutiny_service.events.ScrutinyApprovedEvent;
import com.registraduria.scrutiny_service.events.ScrutinyResultsGeneratedEvent;
import com.registraduria.scrutiny_service.events.producer.ScrutinyEventProducer;
import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScrutinyApprovedConsumer {

    private final CandidateVoteQueryService candidateVoteQueryService;

    private final ScrutinyEventProducer scrutinyEventProducer;

    @KafkaListener(
            topics = "scrutiny.approved",
            groupId = "scrutiny-group"
    )
    public void consume(
            ScrutinyApprovedEvent event
    ) {

        if (event.level() == ScrutinyLevel.NATIONAL) {

            List<CandidateVoteData> voteData =
                    candidateVoteQueryService.aggregateAll()
                            .stream()
                            .map(r -> new CandidateVoteData(
                                    r.candidateId(),
                                    r.candidateName(),
                                    r.party(),
                                    r.totalVotes()
                            ))
                            .toList();

            scrutinyEventProducer.publishResultsGenerated(
                    new ScrutinyResultsGeneratedEvent(
                            ScrutinyLevel.NATIONAL,
                            voteData
                    )
            );
        }
    }
}
