package com.registraduria.scrutiny_service.portal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.portal.dto.PortalResultsResponse;
import com.registraduria.scrutiny_service.portal.service.PortalService;

import lombok.RequiredArgsConstructor;

/**
 * API publica del portal de resultados (CA-4). Solo lectura, alta disponibilidad.
 * Expone el estado del badge (PRELIMINAR vs RESULTADOS OFICIALES) y el enlace
 * permanente al Acta E-26 una vez oficializado.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService service;

    @GetMapping("/results/{scrutinyCode}")
    public PortalResultsResponse results(@PathVariable String scrutinyCode) {
        return service.getResults(scrutinyCode);
    }
}
