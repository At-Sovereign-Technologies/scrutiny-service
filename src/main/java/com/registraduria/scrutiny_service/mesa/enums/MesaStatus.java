package com.registraduria.scrutiny_service.mesa.enums;

/**
 * FSM (Finite State Machine) para Mesa Electoral
 * 
 * Transiciones válidas:
 *   OPEN        → CLOSED (cierre por junta escrutadora)
 *   CLOSED      → VERIFIED (validación VVPAT exitosa)
 *   VERIFIED    → SEALED (sellado de acta E14)
 *   OPEN/CLOSED/VERIFIED → QUARANTINED (irregularidades detectadas)
 */
public enum MesaStatus {

    /**
     * Estado inicial: Mesa abierta, lista para recibir votos.
     */
    OPEN,

    /**
     * Mesa cerrada: Escrutinio completado, acta rellenada manualmente.
     */
    CLOSED,

    /**
     * Verificación completada: VVPAT vs votos manuales coinciden.
     */
    VERIFIED,

    /**
     * Acta E14 sellada oficialmente: Transición final inmutable.
     */
    SEALED,

    /**
     * Mesa en cuarentena: Irregularidades detectadas, pendiente investigación.
     */
    QUARANTINED
}
