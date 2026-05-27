package com.registraduria.scrutiny_service.official.exception;

/**
 * Transicion no valida del escrutinio general (p. ej. aprobar un escrutinio que
 * no esta CERRADO, o ya OFICIAL). Se mapea a HTTP 409 CONFLICT.
 */
public class InvalidScrutinyStateException extends RuntimeException {

    public InvalidScrutinyStateException(String message) {
        super(message);
    }
}
