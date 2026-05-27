package com.registraduria.scrutiny_service.official.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.official.entity.GeneralScrutiny;

public interface GeneralScrutinyRepository
        extends JpaRepository<GeneralScrutiny, Long> {

    Optional<GeneralScrutiny> findByScrutinyCode(String scrutinyCode);

    boolean existsByScrutinyCode(String scrutinyCode);
}
