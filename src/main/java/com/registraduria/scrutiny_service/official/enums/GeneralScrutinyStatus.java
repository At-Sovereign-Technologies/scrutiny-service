package com.registraduria.scrutiny_service.official.enums;

/**
 * Estados del escrutinio general (SR-M5).
 *
 * Transiciones validas:
 *   CERRADO       -> EN_APROBACION (primera aprobacion de magistrado)
 *   EN_APROBACION -> OFICIAL       (se alcanza el quorum -> se genera el Acta E-26)
 *
 * Solo se admite aprobar mientras el escrutinio este CERRADO o EN_APROBACION.
 * OFICIAL es terminal: el acta ya fue generada y es inmutable.
 */
public enum GeneralScrutinyStatus {

    /** Resultados consolidados por SR-M4, listos para aprobacion (CA-1). */
    CERRADO,

    /** Al menos un magistrado ha aprobado; aun no se alcanza el quorum. */
    EN_APROBACION,

    /** Quorum alcanzado, Acta E-26 generada. Estado terminal. */
    OFICIAL
}
