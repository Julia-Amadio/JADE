package com.jadeproject.backend.repository;

import com.jadeproject.backend.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    //Listar todos os incidentes de um monitor específico
    List<Incident> findByMonitorId(Long monitorId);

    //Buscar incidentes abertos de um monitor específico
    //Útil para saber se já tem um incidente aberto antes de criar outro duplicado
    Optional<Incident> findByMonitorIdAndStatus(Long monitorId, String status);
}