package com.registraduria.scrutiny_service.official.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Provee el par de claves RSA usado para firmar el Acta E-26: tanto las firmas
 * por magistrado (CA-2) como la firma XML-DSig del documento.
 *
 * NOTA DE SEGURIDAD: en este servicio la clave se genera en memoria al arrancar,
 * suficiente para emitir firmas verificables de extremo a extremo. En produccion
 * esta clave debe provenir de un HSM / PKI institucional (certificado X.509 por
 * magistrado) para constituir firmas XAdES-BES / PAdES legalmente validas.
 */
@Component
@Getter
public class ActaSigningKeyProvider {

    private final KeyPair keyPair;

    public ActaSigningKeyProvider() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo inicializar la clave de firma del acta.", e
            );
        }
    }
}
