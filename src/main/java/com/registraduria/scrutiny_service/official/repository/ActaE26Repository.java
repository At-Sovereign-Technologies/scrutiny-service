package com.registraduria.scrutiny_service.official.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.official.entity.ActaE26;

public interface ActaE26Repository extends JpaRepository<ActaE26, Long> {

    Optional<ActaE26> findByGeneralScrutinyId(Long generalScrutinyId);

    Optional<ActaE26> findByActaNumber(String actaNumber);

    boolean existsByGeneralScrutinyId(Long generalScrutinyId);
}
