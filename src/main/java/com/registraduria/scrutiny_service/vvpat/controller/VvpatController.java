package com.registraduria.scrutiny_service.vvpat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.vvpat.dto.VvpatScanRequest;
import com.registraduria.scrutiny_service.vvpat.entity.VvpatScanRecord;
import com.registraduria.scrutiny_service.vvpat.service.VvpatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/vvpat")
@RequiredArgsConstructor
@Validated
public class VvpatController {

    private final VvpatService service;

    @PostMapping("/scan")
    @ResponseStatus(HttpStatus.CREATED)
    public VvpatScanRecord scan(

            @Valid
            @RequestBody
            VvpatScanRequest request
    ) {

        return service.scan(request);
    }
}