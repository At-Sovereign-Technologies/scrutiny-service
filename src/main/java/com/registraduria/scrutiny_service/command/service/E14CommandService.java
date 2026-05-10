package com.registraduria.scrutiny_service.command.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.command.dto.CreateE14Request;
import com.registraduria.scrutiny_service.command.dto.SignE14Request;
import com.registraduria.scrutiny_service.command.repository.E14CommandRepository;
import com.registraduria.scrutiny_service.domain.entity.E14Record;
import com.registraduria.scrutiny_service.domain.enums.E14Status;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class E14CommandService {

    private final E14CommandRepository repository;

    public E14Record create(CreateE14Request request) {

        E14Record record = E14Record.builder()
                .mesaCode(request.mesaCode())
                .municipality(request.municipality())
                .pdfHash(request.pdfHash())
                .status(E14Status.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(record);
    }

    public E14Record sign(
            Long id,
            SignE14Request request
    ) {

        E14Record record = repository.findById(id)
                .orElseThrow();

        if (record.getStatus() != E14Status.DRAFT) {
            throw new RuntimeException(
                    "Only draft records can be signed."
            );
        }

        record.setStatus(E14Status.SIGNED);

        return repository.save(record);
    }

    public E14Record publish(Long id) {

        E14Record record = repository.findById(id)
                .orElseThrow();

        if (record.getStatus() != E14Status.SIGNED) {
            throw new RuntimeException(
                    "Only signed records can be published."
            );
        }

        record.setStatus(E14Status.PUBLISHED);

        return repository.save(record);
    }
}
