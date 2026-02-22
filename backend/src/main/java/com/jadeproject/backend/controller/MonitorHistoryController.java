package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.MonitorHistoryResponseDTO;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.MonitorHistory;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.security.UserDetailsImpl;
import com.jadeproject.backend.service.MonitorHistoryService;
import com.jadeproject.backend.service.MonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/history")
public class MonitorHistoryController {

    private final MonitorHistoryService historyService;
    private final MonitorService monitorService; //Nova injeção

    public MonitorHistoryController(MonitorHistoryService historyService, MonitorService monitorService) {
        this.historyService = historyService;
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

    //1. OBTER OS 10 ÚLTIMOS LOGS (dashboard rápido)
    //URL: GET http://localhost:8080/history/recent/{monitorId}
    @GetMapping("/recent/{monitorId}")
    public ResponseEntity<List<MonitorHistoryResponseDTO>> getRecentHistory(@PathVariable Long monitorId) {
        checkMonitorOwner(monitorId); //Segurança

        List<MonitorHistory> logs = historyService.getRecentLogs(monitorId);

        //Converte a lista de Entidades para lista de DTOs
        List<MonitorHistoryResponseDTO> dtos = logs.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //2. OBTER TUDO (para relatórios)
    //URL: GET http://localhost:8080/history/all/{monitorId}
    @GetMapping("/all/{monitorId}")
    public ResponseEntity<List<MonitorHistoryResponseDTO>> getAllHistory(@PathVariable Long monitorId) {
        checkMonitorOwner(monitorId); //Idem

        List<MonitorHistory> logs = historyService.getAllLogs(monitorId);

        List<MonitorHistoryResponseDTO> dtos = logs.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //Método auxiliar de conversão (Entity -> DTO)
    private MonitorHistoryResponseDTO toResponseDTO(MonitorHistory history) {
        MonitorHistoryResponseDTO dto = new MonitorHistoryResponseDTO();
        dto.setId(history.getId());
        dto.setStatusCode(history.getStatusCode());
        dto.setLatency(history.getLatency());
        dto.setIsSuccessful(history.getIsSuccessful());
        dto.setCheckedAt(history.getCheckedAt());

        //Null check de segurança, embora o banco exija monitor
        if (history.getMonitor() != null) {
            dto.setMonitorId(history.getMonitor().getId());
        }

        return dto;
    }
}
