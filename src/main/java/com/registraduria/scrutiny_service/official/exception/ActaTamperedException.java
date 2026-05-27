package com.registraduria.scrutiny_service.official.exception;

/**
 * El SHA-256 reverificado no coincide con el almacenado: el acta fue manipulada
 * en almacenamiento. Es un fallo de integridad del servidor -> HTTP 500.
 */
public class ActaTamperedException extends RuntimeException {

    public ActaTamperedException(String message) {
        super(message);
    }
}
