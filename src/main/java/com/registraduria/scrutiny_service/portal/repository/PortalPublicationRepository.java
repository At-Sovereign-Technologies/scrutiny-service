package com.registraduria.scrutiny_service.portal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.portal.entity.PortalPublication;

public interface PortalPublicationRepository
        extends JpaRepository<PortalPublication, Long> {

    Optional<PortalPublication> findByScrutinyCode(String scrutinyCode);
}
