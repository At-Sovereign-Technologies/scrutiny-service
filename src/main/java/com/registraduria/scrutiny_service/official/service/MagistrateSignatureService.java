package com.registraduria.scrutiny_service.official.service;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Calcula y verifica firmas digitales RSA-SHA256 (base64) sobre el estado
 * aprobado por cada magistrado (CA-2). El esquema es la base criptografica de
 * XAdES-BES / PAdES.
 */
@Service
@RequiredArgsConstructor
public class MagistrateSignatureService {

    public static final String SCHEME = "RSA-SHA256 (base XAdES-BES/PAdES)";

    private final ActaSigningKeyProvider keyProvider;

    /** Firma "magistrateId|stateHash|timestamp" y devuelve la firma en base64. */
    public String sign(String magistrateId, String stateHash, String timestampUtc) {

        String payload = magistrateId + "|" + stateHash + "|" + timestampUtc;

        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(keyProvider.getKeyPair().getPrivate());
            signer.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando la aprobacion.", e);
        }
    }

    /** Verifica una firma de magistrado contra el mismo payload canonico. */
    public boolean verify(
            String magistrateId,
            String stateHash,
            String timestampUtc,
            String signatureBase64
    ) {

        String payload = magistrateId + "|" + stateHash + "|" + timestampUtc;

        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(keyProvider.getKeyPair().getPublic());
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }
}
