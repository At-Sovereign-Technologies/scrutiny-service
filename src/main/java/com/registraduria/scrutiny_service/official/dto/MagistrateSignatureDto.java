package com.registraduria.scrutiny_service.official.dto;

/**
 * Firma digital de un magistrado aprobador embebida en el Acta E-26 (CA-2).
 *
 * El valor {@code signature} es una firma RSA (base64) sobre el estado aprobado.
 * El campo {@code scheme} documenta el formato; en este servicio se emite la
 * firma criptografica base (RSA-SHA256 / XML-DSig) sobre la que se construye
 * XAdES-BES / PAdES en un despliegue con PKI/HSM.
 */
public record MagistrateSignatureDto(

        String magistrateId,

        String magistrateName,

        String signature,

        String signedAtUtc,

        String scheme
) {
}
