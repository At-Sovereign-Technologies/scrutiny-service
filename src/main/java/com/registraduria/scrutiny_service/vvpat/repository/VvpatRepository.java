package com.registraduria.scrutiny_service.vvpat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.vvpat.entity.VvpatScanRecord;

public interface VvpatRepository
        extends JpaRepository<VvpatScanRecord, Long> {

    List<VvpatScanRecord> findByMesaCode(String mesaCode);
}