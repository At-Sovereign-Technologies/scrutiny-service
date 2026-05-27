package com.registraduria.scrutiny_service.official.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utilidad de hashing SHA-256. Centraliza el calculo del hash de integridad del
 * Acta E-26 y del estado del escrutinio (CA-1, CA-3) para evitar duplicacion.
 */
public final class Sha256Util {

    private Sha256Util() {
    }

    public static String hexDigest(String content) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    content.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new IllegalStateException("Error computing SHA-256.", e);
        }
    }
}
