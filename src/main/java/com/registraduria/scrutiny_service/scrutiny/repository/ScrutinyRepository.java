package com.registraduria.scrutiny_service.scrutiny.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.scrutiny.entity.ScrutinyApprovalRecord;

public interface ScrutinyRepository
        extends JpaRepository<
                ScrutinyApprovalRecord,
                Long
        > {
}