package com.registraduria.scrutiny_service.official.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.registraduria.scrutiny_service.official.exception.ActaImmutableException;
import com.registraduria.scrutiny_service.official.exception.ActaTamperedException;
import com.registraduria.scrutiny_service.official.exception.ForbiddenException;
import com.registraduria.scrutiny_service.official.exception.InvalidScrutinyStateException;
import com.registraduria.scrutiny_service.official.exception.ResourceNotFoundException;
import com.registraduria.scrutiny_service.official.exception.UnauthorizedException;

/**
 * Mapea las excepciones especificas de SR-M5 a codigos HTTP. Solo declara
 * handlers para estos tipos: no intercepta excepciones de otros modulos, de modo
 * que los contratos preexistentes de SR-M4 no se ven afectados.
 */
@RestControllerAdvice
public class OfficialExceptionHandler {

    /** CA-3: cualquier intento de mutar/eliminar un acta -> 403 FORBIDDEN. */
    @ExceptionHandler(ActaImmutableException.class)
    public ResponseEntity<Map<String, Object>> handleImmutable(ActaImmutableException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** RBAC: autenticado pero sin el rol requerido -> 403 FORBIDDEN. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Credencial JWT ausente/invalida/expirada -> 401 UNAUTHORIZED. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Transicion de estado no valida -> 409 CONFLICT. */
    @ExceptionHandler(InvalidScrutinyStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidScrutinyStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Integridad comprometida (hash no coincide) -> 500 INTERNAL_SERVER_ERROR. */
    @ExceptionHandler(ActaTamperedException.class)
    public ResponseEntity<Map<String, Object>> handleTampered(ActaTamperedException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message
        ));
    }
}
