package com.registraduria.scrutiny_service.vvpat.entity;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.vvpat.enums.VvpatResult;

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

@Entity
@Table(name = "vvpat_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VvpatScanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mesaCode;

    private String juradoId;

    private Integer physicalVotes;

    @Enumerated(EnumType.STRING)
    private VvpatResult result;

    private Integer attempt;

    private LocalDateTime scannedAt;
}