package com.registraduria.scrutiny_service.official.service;

import org.springframework.stereotype.Service;

import com.registraduria.scrutiny_service.official.entity.ActaE26;
import com.registraduria.scrutiny_service.official.exception.ActaTamperedException;

/**
 * Verificacion de integridad del Acta E-26 (CA-3): recalcula el SHA-256 del
 * contenido almacenado y lo compara con el hash persistido. Se invoca en cada
 * lectura para detectar cualquier manipulacion del almacenamiento.
 */
@Service
public class ActaIntegrityService {

    /** Devuelve true si el hash recalculado coincide con el almacenado. */
    public boolean verify(ActaE26 acta) {
        String recomputed = Sha256Util.hexDigest(acta.getContentJson());
        return recomputed.equals(acta.getContentHash());
    }

    /** Igual que {@link #verify} pero lanza si la integridad falla. */
    public void verifyOrThrow(ActaE26 acta) {
        if (!verify(acta)) {
            throw new ActaTamperedException(
                    "Integridad comprometida: el hash del acta "
                            + acta.getActaNumber() + " no coincide.");
        }
    }
}
