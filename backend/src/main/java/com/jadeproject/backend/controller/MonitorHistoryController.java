package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.MonitorHistoryResponseDTO;
import com.jadeproject.backend.model.MonitorHistory;
import com.jadeproject.backend.service.MonitorHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/history")
public class MonitorHistoryController {

    private final MonitorHistoryService historyService;

    public MonitorHistoryController(MonitorHistoryService historyService) {
        this.historyService = historyService;
    }

    //1. OBTER OS 10 ÚLTIMOS LOGS (dashboard rápido)
    //URL: GET http://localhost:8080/history/recent/{monitorId}
    @GetMapping("/recent/{monitorId}")
    public ResponseEntity<List<MonitorHistoryResponseDTO>> getRecentHistory(@PathVariable Long monitorId) {
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
