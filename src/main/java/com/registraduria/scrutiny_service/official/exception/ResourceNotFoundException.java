package com.registraduria.scrutiny_service.official.exception;

/**
 * Recurso de SR-M5 no encontrado (escrutinio o acta). Se mapea a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
