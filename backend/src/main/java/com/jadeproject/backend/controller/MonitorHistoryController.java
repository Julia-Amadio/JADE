package com.jadeproject.backend.controller;

import com.jadeproject.backend.model.MonitorHistory;
import com.jadeproject.backend.service.MonitorHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<MonitorHistory>> getRecentHistory(@PathVariable Long monitorId) {
        List<MonitorHistory> logs = historyService.getRecentLogs(monitorId);
        return ResponseEntity.ok(logs);
    }

    //2. OBTER TUDO (para relatórios)
    //URL: GET http://localhost:8080/history/all/{monitorId}
    @GetMapping("/all/{monitorId}")
    public ResponseEntity<List<MonitorHistory>> getAllHistory(@PathVariable Long monitorId) {
        List<MonitorHistory> logs = historyService.getAllLogs(monitorId);
        return ResponseEntity.ok(logs);
    }
}
