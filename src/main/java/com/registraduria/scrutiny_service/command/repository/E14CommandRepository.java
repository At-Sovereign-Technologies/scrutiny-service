package com.registraduria.scrutiny_service.command.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.registraduria.scrutiny_service.domain.entity.E14Record;

public interface E14CommandRepository extends JpaRepository<E14Record, Long> {

    /**
     * Find E14 record by mesa code
     */
    @Query("SELECT e FROM E14Record e WHERE e.mesaCode = :mesaCode")
    Optional<E14Record> findByMesaCode(@Param("mesaCode") String mesaCode);
}
