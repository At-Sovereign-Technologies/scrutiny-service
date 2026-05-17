package com.registraduria.scrutiny_service.scrutiny.entity;

import java.time.LocalDateTime;

import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyDecision;
import com.registraduria.scrutiny_service.scrutiny.enums.ScrutinyLevel;

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
@Table(name = "scrutiny_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrutinyApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scrutinyHash;

    private String delegateName;

    private String alerts;

    @Enumerated(EnumType.STRING)
    private ScrutinyLevel level;

    @Enumerated(EnumType.STRING)
    private ScrutinyDecision decision;

    private LocalDateTime createdAt;
}