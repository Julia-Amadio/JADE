package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.MonitorHistory;
import com.jadeproject.backend.repository.MonitorHistoryRepository;
import org.springframework.data.domain.PageRequest; //Para solicitar X itens
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorHistoryService {

    private final MonitorHistoryRepository historyRepository;

    public MonitorHistoryService(MonitorHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    //Registra uma nova verificação (ping/http check) no banco
    //Quem vai fazer isso no futuro é o scheduler
    public void saveLog(Monitor monitor, int statusCode, long responseTimeMs, boolean isUp) {
        MonitorHistory log = new MonitorHistory();
        log.setMonitor(monitor);
        log.setStatusCode(statusCode);
        log.setLatency((int) responseTimeMs);
        log.setIsSuccessful(isUp);
        log.setCheckedAt(LocalDateTime.now());
        historyRepository.save(log);
    }

    //Busca histórico recente para o dashboard
    //Usa método top10 do MonitorHistoryRepository
    public List<MonitorHistory> getRecentLogs(Long monitorId) {
        return historyRepository.findByMonitorId(
                monitorId,
                PageRequest.of(0, 10, Sort.by("checkedAt").descending())
        );
    }

    //Histórico completo para relatórios detalhados
    public List<MonitorHistory> getAllLogs(Long monitorId) {
        return historyRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId);
    }
}