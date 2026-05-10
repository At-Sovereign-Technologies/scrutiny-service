package com.registraduria.scrutiny_service.domain.entity;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.domain.enums.E14Status;

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

@Entity
@Table(name = "e14_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class E14Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mesa_code")
    private String mesaCode;

    private String municipality;

    @Column(name = "pdf_hash", length = 5000)
    private String pdfHash;

    @Enumerated(EnumType.STRING)
    private E14Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}