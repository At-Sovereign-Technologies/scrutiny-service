package com.registraduria.scrutiny_service.vvpat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.events.VvpatMismatchEvent;
import com.registraduria.scrutiny_service.events.producer.VvpatEventProducer;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.repository.MesaRepository;
import com.registraduria.scrutiny_service.vvpat.dto.VvpatScanRequest;
import com.registraduria.scrutiny_service.vvpat.entity.VvpatScanRecord;
import com.registraduria.scrutiny_service.vvpat.enums.VvpatResult;
import com.registraduria.scrutiny_service.vvpat.repository.VvpatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VvpatService {

    private final VvpatRepository repository;

    private final MesaRepository mesaRepository;

    private final VvpatEventProducer producer;

    public VvpatScanRecord scan(
            VvpatScanRequest request
    ) {

        MesaRecord mesa = mesaRepository
                .findByMesaCode(
                        request.mesaCode()
                )
                .orElseThrow();

        List<VvpatScanRecord> previous =
                repository.findByMesaCode(
                        request.mesaCode()
                );

        int attempt =
                previous.size() + 1;

        boolean matches =
                mesa.getValidVotes()
                        .equals(
                                request.physicalVotes()
                        );

        VvpatResult result =
                matches
                        ? VvpatResult.MATCH
                        : VvpatResult.MISMATCH;

        VvpatScanRecord scan =
                VvpatScanRecord.builder()
                        .mesaCode(
                                request.mesaCode()
                        )
                        .juradoId(
                                request.juradoId()
                        )
                        .physicalVotes(
                                request.physicalVotes()
                        )
                        .attempt(attempt)
                        .result(result)
                        .scannedAt(
                                LocalDateTime.now()
                        )
                        .build();

        repository.save(scan);

        if (!matches) {

            producer.publishMismatch(

                    new VvpatMismatchEvent(

                            request.mesaCode(),

                            mesa.getValidVotes(),

                            request.physicalVotes(),

                            attempt
                    )
            );

            if (attempt >= 3) {

                mesa.setQuarantined(true);

                mesaRepository.save(mesa);

                System.out.println(
                        """
                        ================================
                        MESA QUARANTINED
                        Mesa: %s
                        Attempts: %d
                        ================================
                        """
                                .formatted(
                                        mesa.getMesaCode(),
                                        attempt
                                )
                );
            }
        }

        return scan;
    }
}