package com.jadeproject.backend.controller;

import com.jadeproject.backend.model.Incident;
import com.jadeproject.backend.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Incident>> getIncidentsByMonitor(@PathVariable Long monitorId) {
        List<Incident> incidents = incidentService.getIncidentsByMonitor(monitorId);
        return ResponseEntity.ok(incidents);
    }
}
