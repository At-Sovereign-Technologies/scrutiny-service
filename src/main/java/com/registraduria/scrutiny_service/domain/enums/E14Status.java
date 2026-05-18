package com.registraduria.scrutiny_service.domain.enums;

/**
 * Estados del Acta E14 (Formulario Electoral de Resultados)
 * 
 * Transiciones válidas:
 *   DRAFT     → SIGNED (acta firmada por mesa escrutadora)
 *   SIGNED    → PUBLISHED (publicada en escrutinio)
 *   PUBLISHED → SEALED (sellada oficialmente - final)
 *   * (excepto SEALED) → QUARANTINED (irregularidad detectada)
 */
public enum E14Status {

    /**
     * Acta en borrador: Generada, pendiente firma
     */
    DRAFT,

    /**
     * Acta firmada: Suscrita por mesa escrutadora
     */
    SIGNED,

    /**
     * Acta publicada: Disponible en escrutinio público
     */
    PUBLISHED,

    /**
     * Acta sellada: Estado final, registrada oficialmente
     */
    SEALED,

    /**
     * Acta en cuarentena: Irregularidad detectada, pendiente investigación
     */
    QUARANTINED
}
