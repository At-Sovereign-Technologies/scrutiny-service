package com.registraduria.scrutiny_service.official.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.official.entity.MagistrateApproval;

public interface MagistrateApprovalRepository
        extends JpaRepository<MagistrateApproval, Long> {

    List<MagistrateApproval> findByGeneralScrutinyIdOrderByApprovedAtUtcAsc(
            Long generalScrutinyId
    );

    long countByGeneralScrutinyId(Long generalScrutinyId);

    boolean existsByGeneralScrutinyIdAndMagistrateId(
            Long generalScrutinyId,
            String magistrateId
    );
}
