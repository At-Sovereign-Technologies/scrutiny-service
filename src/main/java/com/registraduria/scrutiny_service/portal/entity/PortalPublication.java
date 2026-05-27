package com.registraduria.scrutiny_service.portal.entity;

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
 * Estado de publicacion del portal publico para un escrutinio (CA-4).
 *
 * Mientras {@code official} es false el portal muestra la etiqueta PRELIMINAR;
 * al generarse el Acta E-26 se actualiza a RESULTADOS OFICIALES y se publica el
 * enlace permanente al acta.
 */
@Entity
@Table(name = "portal_publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String scrutinyCode;

    @Column(nullable = false)
    private boolean official;

    @Column(nullable = false, length = 100)
    private String badgeText;

    @Column(length = 512)
    private String actaPermalink;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
