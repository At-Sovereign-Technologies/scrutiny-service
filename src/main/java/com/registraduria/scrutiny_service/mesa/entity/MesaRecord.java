package com.registraduria.scrutiny_service.mesa.entity;

import com.registraduria.scrutiny_service.mesa.enums.MesaStatus;

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
@Table(name = "mesas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mesaCode;

    @Column(nullable = false)
    private Integer validVotes;

    @Column(nullable = false)
    private Integer blankVotes;

    @Column(nullable = false)
    private Integer nullVotes;

    @Column(nullable = false)
    private Integer unmarkedVotes;

    @Enumerated(EnumType.STRING)
    private MesaStatus status;
}