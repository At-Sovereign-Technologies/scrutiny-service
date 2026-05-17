package com.registraduria.scrutiny_service.mesa.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.mesa.dto.CloseMesaRequest;
import com.registraduria.scrutiny_service.mesa.dto.CreateMesaRequest;
import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import com.registraduria.scrutiny_service.mesa.service.MesaCommandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaCommandService service;

    @PatchMapping("/{id}/close")
    public MesaRecord closeMesa(

            @PathVariable Long id,

            @RequestBody
            CloseMesaRequest request
    ) {

        return service.closeMesa(
                id,
                request
        );
    }

    @PostMapping
    public MesaRecord createMesa(

            @RequestBody
            CreateMesaRequest request
    ) {

        return service.createMesa(request);
    }
}