package com.registraduria.scrutiny_service.mesa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;

public interface MesaRepository extends JpaRepository<MesaRecord, Long> {
    Optional<MesaRecord> findByMesaCode(String mesaCode);
}
