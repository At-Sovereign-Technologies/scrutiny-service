package com.registraduria.scrutiny_service.official;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilidad de test para acuñar JWT HS256 validos contra el secreto del servicio.
 * Refleja exactamente el formato que verifica {@code MagistrateSecurityService}.
 */
public final class TestJwt {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

    private TestJwt() {
    }

    /** Token con rol MAGISTRADO_CNE valido (sin expiracion). */
    public static String magistrate(String secret, String sub, String name) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", sub);
        claims.put("name", name);
        claims.put("role", "MAGISTRADO_CNE");
        return sign(secret, claims);
    }

    public static String sign(String secret, Map<String, Object> claims) {
        try {
            String header = URL.encodeToString(
                    "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = URL.encodeToString(
                    MAPPER.writeValueAsBytes(claims));
            String signingInput = header + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig = URL.encodeToString(
                    mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
