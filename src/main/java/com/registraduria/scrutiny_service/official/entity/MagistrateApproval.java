package com.registraduria.scrutiny_service.official.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoria append-only de la aprobacion de un magistrado (CA-1).
 *
 * SEGURIDAD / TRAZABILIDAD: deja constancia inmutable de QUIEN aprobo
 * (magistrado), CUANDO (timestamp UTC), DESDE DONDE (IP) y QUE estado se
 * aprobo (hash del estado). Nunca se actualiza ni se borra.
 */
@Entity
@Table(name = "magistrate_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagistrateApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long generalScrutinyId;

    @Column(nullable = false)
    private String magistrateId;

    @Column(nullable = false)
    private String magistrateName;

    /** Hash del estado del escrutinio en el momento de aprobar (CA-1). */
    @Column(nullable = false, length = 64)
    private String approvedStateHash;

    /** Firma digital RSA (base64) del magistrado sobre el estado aprobado. */
    @Column(nullable = false, length = 1024)
    private String signature;

    /** IP de origen de la peticion de aprobacion (trazabilidad). */
    @Column(nullable = false, length = 45)
    private String ipAddress;

    /** Timestamp de la aprobacion, siempre en UTC (CA-1). */
    @Column(nullable = false)
    private LocalDateTime approvedAtUtc;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
