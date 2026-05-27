package com.registraduria.scrutiny_service.official.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.registraduria.scrutiny_service.official.dto.ActaE26Response;
import com.registraduria.scrutiny_service.official.exception.ActaImmutableException;
import com.registraduria.scrutiny_service.official.service.ActaQueryService;

import lombok.RequiredArgsConstructor;

/**
 * API de consulta del Acta E-26 (CA-3).
 *
 * INMUTABILIDAD: solo se exponen lecturas. Cualquier intento de modificar,
 * reemplazar o eliminar un acta (PUT/PATCH/DELETE) responde 403 FORBIDDEN de
 * forma explicita. El almacenamiento ademas es Write-Once a nivel de BD.
 */
@RestController
@RequestMapping("/api/v1/actas")
@RequiredArgsConstructor
public class ActaE26Controller {

    private final ActaQueryService service;

    /** Lectura del acta; reverifica el hash de integridad en cada consulta. */
    @GetMapping("/{id}")
    public ActaE26Response get(@PathVariable Long id) {
        return service.getById(id);
    }

    /** Verificacion explicita de integridad (true = intacta). */
    @GetMapping("/{id}/verify")
    public Map<String, Object> verify(@PathVariable Long id) {
        return Map.of(
                "actaId", id,
                "hashVerified", service.verifyIntegrity(id)
        );
    }

    // ---- Mutaciones prohibidas: el acta es inmutable (CA-3) -> 403 -----------

    @PutMapping("/{id}")
    public void update(@PathVariable Long id) {
        throw new ActaImmutableException(
                "El Acta E-26 es inmutable: no puede modificarse.");
    }

    @PatchMapping("/{id}")
    public void patch(@PathVariable Long id) {
        throw new ActaImmutableException(
                "El Acta E-26 es inmutable: no puede modificarse.");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        throw new ActaImmutableException(
                "El Acta E-26 es inmutable: no puede eliminarse.");
    }
}
