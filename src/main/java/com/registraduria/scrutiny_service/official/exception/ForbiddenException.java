package com.registraduria.scrutiny_service.official.exception;

/**
 * Usuario autenticado pero sin el rol requerido (RBAC). Se mapea a HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
