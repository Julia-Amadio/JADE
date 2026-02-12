package com.jadeproject.backend.repository;

import com.jadeproject.backend.model.MonitorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable; //Importante para o "Limit 10"
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitorHistoryRepository extends JpaRepository<MonitorHistory, Long>{

    //Busca logs de um monitor específico
    //OrderByCheckerAtDesc = ordena pela data (mais novo primeiro)
    //SELECT * FROM monitor_history WHERE monitor_id = ? ORDER BY checked_at DESC
    List<MonitorHistory> findByMonitorIdOrderByCheckedAtDesc(Long monitorId);

    //Busca com paginação para pegar o top 10
    List<MonitorHistory> findByMonitorId(Long monitorId, Pageable pageable);
}
