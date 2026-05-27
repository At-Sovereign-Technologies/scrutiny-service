package com.registraduria.scrutiny_service.official.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.official.dto.CloseGeneralScrutinyRequest;
import com.registraduria.scrutiny_service.official.dto.GeneralScrutinyResponse;
import com.registraduria.scrutiny_service.official.dto.MagistrateApprovalResponse;
import com.registraduria.scrutiny_service.official.security.MagistrateIdentity;
import com.registraduria.scrutiny_service.official.security.MagistrateSecurityService;
import com.registraduria.scrutiny_service.official.security.RequestClientIp;
import com.registraduria.scrutiny_service.official.service.GeneralScrutinyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API del escrutinio general y la aprobacion de magistrados (SR-M5).
 *
 * SEGURIDAD: el cierre y la aprobacion exigen JWT con rol MAGISTRADO_CNE (RBAC).
 * La consulta de estado es de solo lectura y abierta.
 */
@RestController
@RequestMapping("/api/v1/general-scrutiny")
@RequiredArgsConstructor
public class GeneralScrutinyController {

    private final GeneralScrutinyService service;
    private final MagistrateSecurityService security;

    @PostMapping("/close")
    @ResponseStatus(HttpStatus.CREATED)
    public GeneralScrutinyResponse close(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CloseGeneralScrutinyRequest request,
            HttpServletRequest httpRequest
    ) {
        // RBAC: solo un magistrado del CNE puede cerrar el escrutinio general.
        security.authorize(authHeader, RequestClientIp.resolve(httpRequest));
        return service.close(request);
    }

    @PostMapping("/{scrutinyCode}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public MagistrateApprovalResponse approve(
            @PathVariable String scrutinyCode,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest
    ) {
        String clientIp = RequestClientIp.resolve(httpRequest);
        // CA-1: la identidad del magistrado proviene del JWT verificado (RBAC).
        MagistrateIdentity magistrate = security.authorize(authHeader, clientIp);
        return service.approve(scrutinyCode, magistrate, clientIp);
    }

    @GetMapping("/{scrutinyCode}")
    public GeneralScrutinyResponse get(@PathVariable String scrutinyCode) {
        return service.getByCode(scrutinyCode);
    }
}
