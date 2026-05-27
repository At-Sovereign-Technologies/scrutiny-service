package com.registraduria.scrutiny_service.official.entity;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.official.enums.GeneralScrutinyStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Escrutinio general nacional: agregado consolidado por SR-M4 que los
 * magistrados del CNE aprueban para emitir el Acta E-26 (SR-M5).
 */
@Entity
@Table(name = "general_scrutiny")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralScrutiny {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String scrutinyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneralScrutinyStatus status;

    @Column(nullable = false)
    private String electoralMethod;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private Integer requiredQuorum;

    /** SHA-256 del estado cerrado aprobado por los magistrados (CA-1). */
    @Column(nullable = false, length = 64)
    private String stateHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime officializedAt;
}
