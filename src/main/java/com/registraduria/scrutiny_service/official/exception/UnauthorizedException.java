package com.registraduria.scrutiny_service.official.exception;

/**
 * Credencial ausente o invalida (JWT faltante, mal formado, firma invalida o
 * expirado). Se mapea a HTTP 401 UNAUTHORIZED.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
