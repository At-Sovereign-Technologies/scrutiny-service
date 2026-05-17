package com.registraduria.scrutiny_service.scrutiny.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.scrutiny.dto.ScrutinyApprovalRequest;
import com.registraduria.scrutiny_service.scrutiny.entity.ScrutinyApprovalRecord;
import com.registraduria.scrutiny_service.scrutiny.service.ScrutinyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/scrutiny")
@RequiredArgsConstructor
@Validated
public class ScrutinyController {

    private final ScrutinyService service;

    @PostMapping("/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public ScrutinyApprovalRecord approve(

            @Valid
            @RequestBody
            ScrutinyApprovalRequest request
    ) {

        return service.approve(request);
    }
}