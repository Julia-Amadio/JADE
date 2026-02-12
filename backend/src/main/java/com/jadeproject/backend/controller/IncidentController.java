package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.IncidentResponseDTO;
import com.jadeproject.backend.model.Incident;
import com.jadeproject.backend.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    //LISTAR INCIDENTES DE UM MONITOR ESPECÍFICO
    //URL: GET http://localhost:8080/incidents/monitor/{monitorId}
    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<List<IncidentResponseDTO>> getIncidentsByMonitor(@PathVariable Long monitorId) {
        List<Incident> incidents = incidentService.getIncidentsByMonitor(monitorId);

        //Converte a lista de Entidades para lista de DTOs
        List<IncidentResponseDTO> dtos = incidents.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //Método auxiliar de conversão (Entity -> DTO)
    private IncidentResponseDTO toResponseDTO(Incident incident) {
        IncidentResponseDTO dto = new IncidentResponseDTO();

        dto.setId(incident.getId());
        dto.setTitle(incident.getTitle());
        dto.setSeverity(incident.getSeverity());
        dto.setDescription(incident.getDescription());
        dto.setStatus(incident.getStatus());

        if (incident.getMonitor() != null) {
            dto.setMonitorId(incident.getMonitor().getId());
        }

        return dto;
    }
}
