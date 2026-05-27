package com.registraduria.scrutiny_service.official.entity;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.official.enums.ActaStatus;

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
 * Acta de Declaratoria de Eleccion E-26, oficial e INMUTABLE (CA-2, CA-3).
 *
 * INMUTABILIDAD: esta entidad se inserta una unica vez. No se exponen flujos
 * de actualizacion; ademas, la tabla acta_oficial tiene triggers que rechazan
 * UPDATE/DELETE a nivel de BD (almacenamiento Write-Once). El {@code contentHash}
 * SHA-256 se reverifica en cada lectura para detectar manipulacion.
 */
@Entity
@Table(name = "acta_oficial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActaE26 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numeracion oficial secuencial, p. ej. E26-000001. */
    @Column(nullable = false, unique = true, length = 50)
    private String actaNumber;

    @Column(nullable = false)
    private Long generalScrutinyId;

    @Column(nullable = false)
    private String sourceScrutinyCode;

    @Column(nullable = false)
    private String electoralMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActaStatus status;

    /** Resultado canonico serializado (JSON); base del hash de integridad. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    /** SHA-256 del contentJson; se reverifica en cada lectura (CA-3). */
    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, length = 512)
    private String pdfPath;

    @Column(nullable = false, length = 512)
    private String xmlPath;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
