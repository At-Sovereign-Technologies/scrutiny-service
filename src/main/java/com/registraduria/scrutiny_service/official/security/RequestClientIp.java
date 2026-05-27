package com.registraduria.scrutiny_service.official.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve la IP de origen de la peticion para el registro de auditoria (CA-1),
 * respetando el primer salto declarado en X-Forwarded-For tras un proxy/balanceador.
 */
public final class RequestClientIp {

    private RequestClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
