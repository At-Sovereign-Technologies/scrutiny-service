package com.registraduria.scrutiny_service.official.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registraduria.scrutiny_service.official.exception.ForbiddenException;
import com.registraduria.scrutiny_service.official.exception.UnauthorizedException;

/**
 * Autenticacion JWT + RBAC estricto para las operaciones de SR-M5 (atributo de
 * Seguridad).
 *
 * Valida un JWT HS256 (header.payload.signature, base64url) firmado con el
 * secreto del servicio, verifica expiracion y exige el rol {@code MAGISTRADO_CNE}.
 * Todo intento no autorizado se registra con IP y motivo (trazabilidad).
 *
 * NOTA: la verificacion HMAC-SHA256 se implementa con la JDK para no acoplar el
 * servicio a una libreria concreta; en produccion se sustituye por el resource
 * server OAuth2/OIDC de la plataforma.
 */
@Service
public class MagistrateSecurityService {

    private static final Logger log =
            LoggerFactory.getLogger(MagistrateSecurityService.class);

    public static final String REQUIRED_ROLE = "MAGISTRADO_CNE";

    private final byte[] secret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MagistrateSecurityService(
            @Value("${scrutiny.security.magistrate-jwt-secret}") String secret
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Autoriza la peticion: valida el JWT y el rol. Lanza 401 si la credencial
     * es invalida y 403 si el rol no es el requerido. Devuelve la identidad del
     * magistrado para su registro en auditoria.
     */
    public MagistrateIdentity authorize(String authorizationHeader, String clientIp) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            log.warn("Intento no autorizado SR-M5 desde IP {}: token ausente", clientIp);
            throw new UnauthorizedException("Token Bearer requerido.");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        JsonNode claims = verifyAndDecode(token, clientIp);

        long now = System.currentTimeMillis() / 1000L;
        if (claims.has("exp") && claims.get("exp").asLong() < now) {
            log.warn("Intento no autorizado SR-M5 desde IP {}: token expirado", clientIp);
            throw new UnauthorizedException("Token expirado.");
        }

        String role = claims.path("role").asText("");
        if (!REQUIRED_ROLE.equals(role)) {
            // Autenticado pero sin el rol -> 403 (RBAC estricto).
            log.warn("Acceso denegado SR-M5 desde IP {}: rol '{}' insuficiente (sub={})",
                    clientIp, role, claims.path("sub").asText(""));
            throw new ForbiddenException(
                    "Se requiere el rol " + REQUIRED_ROLE + "."
            );
        }

        String sub = claims.path("sub").asText("");
        if (sub.isBlank()) {
            log.warn("Intento no autorizado SR-M5 desde IP {}: 'sub' ausente", clientIp);
            throw new UnauthorizedException("Claim 'sub' requerido.");
        }

        String name = claims.path("name").asText(sub);

        log.info("Magistrado autorizado SR-M5: sub={} ip={}", sub, clientIp);
        return new MagistrateIdentity(sub, name, role);
    }

    private JsonNode verifyAndDecode(String token, String clientIp) {

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            log.warn("Intento no autorizado SR-M5 desde IP {}: JWT mal formado", clientIp);
            throw new UnauthorizedException("JWT mal formado.");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = hmacSha256Base64Url(signingInput);

        // Comparacion en tiempo constante para evitar timing attacks.
        if (!constantTimeEquals(expectedSig, parts[2])) {
            log.warn("Intento no autorizado SR-M5 desde IP {}: firma JWT invalida", clientIp);
            throw new UnauthorizedException("Firma del token invalida.");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Intento no autorizado SR-M5 desde IP {}: payload JWT ilegible", clientIp);
            throw new UnauthorizedException("Payload del token invalido.");
        }
    }

    private String hmacSha256Base64Url(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Error verificando la firma del JWT.", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}
