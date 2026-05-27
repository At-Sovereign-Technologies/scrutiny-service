package com.registraduria.scrutiny_service.official.enums;

/**
 * Estado del Acta E-26 (CA-3).
 *
 * Una vez generada, el acta queda permanentemente en OFICIAL_INMUTABLE.
 * No existe ninguna transicion de salida: el acta no puede modificarse,
 * reemplazarse ni eliminarse (garantizado tambien por triggers en BD).
 */
public enum ActaStatus {

    /** Acta oficial e inmutable. Estado unico y terminal. */
    OFICIAL_INMUTABLE
}
