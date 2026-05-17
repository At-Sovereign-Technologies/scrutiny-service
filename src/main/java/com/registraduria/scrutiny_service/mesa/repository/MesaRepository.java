package com.registraduria.scrutiny_service.mesa.repository;

import com.registraduria.scrutiny_service.mesa.entity.MesaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaRepository extends JpaRepository<MesaRecord, Long> {

}
