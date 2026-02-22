package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.IncidentResponseDTO;
import com.jadeproject.backend.model.Incident;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.security.UserDetailsImpl;
import com.jadeproject.backend.service.IncidentService;
import com.jadeproject.backend.service.MonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final MonitorService monitorService; //Nova injeção!

    public IncidentController(IncidentService incidentService, MonitorService monitorService) {
        this.incidentService = incidentService;
        this.monitorService = monitorService;
    }

    //--- Método de segurança ---
    private void checkMonitorOwner(Long monitorId) {
        //1. Busca o monitor
        Monitor monitor = monitorService.findById(monitorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitor não encontrado"));

        //BLINDAGEM 1: verifica se o monitor tem dono (data integrity check)
        if (monitor.getUser() == null || monitor.getUser().getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dados inconsistentes: monitor sem proprietário.");
        }
        Long ownerId = monitor.getUser().getId();

        //2. Pega a autenticação com segurança
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //BLINDAGEM 2: verifica se o tipo é UserDetailsImpl ("crachá")
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        //EXTRAINDO O USER: pega o crachá e tira o User real de lá de dentro
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        //BLINDAGEM 3: "Yoda Condition" para evitar NPE na Role
        //Se currentUser.getRole() for null, "ROLE_ADMIN".equals(null) retorna false (seguro),
        //enquanto null.equals("ROLE_ADMIN") quebraria o código
        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());

        //3. Verifica permissão
        if (!isAdmin && !currentUser.getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para ver este recurso.");
        }
    }

    //LISTAR INCIDENTES DE UM MONITOR ESPECÍFICO
    //URL: GET http://localhost:8080/incidents/monitor/{monitorId}
    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<List<IncidentResponseDTO>> getIncidentsByMonitor(@PathVariable Long monitorId) {
        checkMonitorOwner(monitorId); //Segurança

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
