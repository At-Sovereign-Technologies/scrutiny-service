package com.registraduria.scrutiny_service.official.service;

/**
 * Contenido binario de un archivo del Acta E-26 (PDF/A-3 o XML firmado) listo
 * para descarga, junto con el nombre de archivo sugerido.
 */
public record ActaDownload(

        String fileName,

        byte[] data
) {
}
