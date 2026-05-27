package com.registraduria.scrutiny_service.official.exception;

/**
 * Se lanza ante cualquier intento de modificar, reemplazar o eliminar un Acta
 * E-26. Se mapea a HTTP 403 FORBIDDEN (CA-3).
 */
public class ActaImmutableException extends RuntimeException {

    public ActaImmutableException(String message) {
        super(message);
    }
}
