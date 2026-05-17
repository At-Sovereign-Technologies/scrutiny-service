package com.registraduria.scrutiny_service.scrutiny.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.events.ScrutinyApprovedEvent;
import com.registraduria.scrutiny_service.events.producer.ScrutinyEventProducer;
import com.registraduria.scrutiny_service.scrutiny.dto.ScrutinyApprovalRequest;
import com.registraduria.scrutiny_service.scrutiny.entity.ScrutinyApprovalRecord;
import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyDecision;
import com.registraduria.scrutiny_service.scrutiny.repository.ScrutinyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScrutinyService {

    private final ScrutinyRepository repository;

    private final ScrutinyEventProducer producer;

    public ScrutinyApprovalRecord approve(
            ScrutinyApprovalRequest request
    ) {

        String hash = generateHash(request);

        ScrutinyApprovalRecord approval =
                ScrutinyApprovalRecord.builder()
                        .level(request.level())
                        .decision(request.decision())
                        .delegateName(
                                request.delegateName()
                        )
                        .alerts(request.alerts())
                        .scrutinyHash(hash)
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        repository.save(approval);

        if (request.decision()
                == ScrutinyDecision.APPROVED) {

            producer.publishApproved(

                    new ScrutinyApprovedEvent(

                            request.level(),

                            request.delegateName(),

                            hash
                    )
            );
        }

        return approval;
    }

    private String generateHash(
            ScrutinyApprovalRequest request
    ) {

        try {

            String content =

                    request.level().name() +

                    request.decision().name() +

                    request.delegateName() +

                    request.alerts();

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format("%02x", b)
                );
            }

            return hex.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating scrutiny hash."
            );
        }
    }
}