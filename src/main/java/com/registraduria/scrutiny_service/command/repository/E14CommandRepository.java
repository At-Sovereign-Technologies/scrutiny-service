package com.registraduria.scrutiny_service.command.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.registraduria.scrutiny_service.domain.entity.E14Record;

public interface E14CommandRepository extends JpaRepository<E14Record, Long> {

}
