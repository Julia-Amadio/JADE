package com.jadeproject.backend.repository;

import com.jadeproject.backend.model.MonitorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitorHistoryRepository extends JpaRepository<MonitorHistory, Long>{

    //Busca logs de um monitor específico
    //OrderByCheckerAtDesc = ordena pela data (mais novo primeiro)
    //SELECT * FROM monitor_history WHERE monitor_id = ? ORDER BY checked_at DESC
    List<MonitorHistory> findByMonitorIdOrderByCheckedAtDesc(Long monitorId);

    //Limitando a busca pegando só os últimos 10 checks
    List<MonitorHistory> findTop10ByMonitorIdOrderByCheckedAtDesc(Long monitorId);
}