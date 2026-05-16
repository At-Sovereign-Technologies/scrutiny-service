package com.registraduria.scrutiny_service.command.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.command.dto.CreateE14Request;
import com.registraduria.scrutiny_service.command.dto.SignE14Request;
import com.registraduria.scrutiny_service.command.service.E14CommandService;
import com.registraduria.scrutiny_service.domain.entity.E14Record;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/e14")
@RequiredArgsConstructor
public class E14CommandController {

    private final E14CommandService service;

    @PostMapping
    public E14Record create(
            @Valid @RequestBody CreateE14Request request
    ) {
        return service.create(request);
    }

    @PatchMapping("/{id}/sign")
    public E14Record sign(

            @PathVariable Long id,

            @Valid
            @RequestBody SignE14Request request
    ) {
        return service.sign(id, request);
    }

    @PatchMapping("/{id}/publish")
    public CompletableFuture<E14Record> publish(
            @PathVariable Long id
    ) {
        return service.publish(id);
    }
}