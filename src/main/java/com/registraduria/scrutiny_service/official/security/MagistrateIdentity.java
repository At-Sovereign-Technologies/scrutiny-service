package com.registraduria.scrutiny_service.official.security;

/**
 * Identidad de un magistrado autenticado, extraida de los claims del JWT
 * verificado (RBAC). El rol ya fue validado al construir esta identidad.
 */
public record MagistrateIdentity(

        String magistrateId,

        String magistrateName,

        String role
) {
}
