package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.MonitorHistory;
import com.jadeproject.backend.repository.MonitorHistoryRepository;
import com.jadeproject.backend.repository.MonitorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest; //Para solicitar X itens
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true) //Define o padrão como LEITURA (mais seguro e performático)
public class MonitorHistoryService {

    private final MonitorHistoryRepository historyRepository;
    private final MonitorRepository monitorRepository;

    public MonitorHistoryService(MonitorHistoryRepository historyRepository,
                                 MonitorRepository monitorRepository) {
        this.historyRepository = historyRepository;
        this.monitorRepository = monitorRepository;
    }

    //Registra uma nova verificação (ping/http check) no banco
    //Quem vai fazer isso no futuro é o scheduler
    @Transactional
    public void saveLog(Monitor monitor, int statusCode, long responseTimeMs, boolean isUp) {
        MonitorHistory log = new MonitorHistory();
        log.setMonitor(monitor);
        log.setStatusCode(statusCode);
        log.setLatency((int) responseTimeMs);
        log.setIsSuccessful(isUp);
        log.setCheckedAt(OffsetDateTime.now(ZoneOffset.UTC));
        historyRepository.save(log);
        //Atualiza last_checked atomicamente com o registro do log
        monitor.setLastChecked(OffsetDateTime.now(ZoneOffset.UTC));
        monitorRepository.save(monitor);
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
    public Page<MonitorHistory> getPaginatedLogs(Long monitorId, Pageable pageable) {
        return historyRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId, pageable);
    }
}
