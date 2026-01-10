package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Incident;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.repository.IncidentRepository;
import com.jadeproject.backend.repository.MonitorHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    //LÓGICA: o site CAIU, o que fazer?
    public void handleDownEvent(Monitor monitor, String errorReason) {
        //1. Verifica se já existe incidente aberto para este monitor
        Optional<Incident> openIncident = incidentRepository.findByMonitorIdAndStatus(monitor.getId(), "OPEN");

        if (openIncident.isPresent()) {
            //Se já está aberto, não fazer nada (pode tbm atualizar o log)
            System.out.println("Monitor " + monitor.getName() + " continua DOWN. Incidente já aberto.");
        } else {
            //2. Se não tem, cria um novo incidente
            Incident newIncident = new Incident();
            newIncident.setMonitor(monitor);
            newIncident.setStatus("OPEN");
            newIncident.setSeverity("HIGH"); //ou "CRITICAL"
            newIncident.setTitle("Monitor Down: " + monitor.getName());
            newIncident.setCreatedAt(LocalDateTime.now());
            newIncident.setDescription(errorReason); //ex.: "Timeout", "404 Not Found"

            incidentRepository.save(newIncident);
            System.out.println("[ALERTA] NOVO INCIDENTE CRIADO: " + monitor.getName() + " encontra-se fora do ar.");
            //TODO: futuramente, enviaria o email/slack de alerta
        }
    }

    //Lógica: o site VOLTOU, o que fazer?
    public void handleUpEvent(Monitor monitor) {
        //1. Verifica se existe um incidente que ficou ABERTO
        Optional<Incident> openIncident = incidentRepository.findByMonitorIdAndStatus(monitor.getId(), "OPEN");

        if (openIncident.isPresent()) {
            //2. Se existe, fechar o incidente
            Incident incident = openIncident.get();
            incident.setStatus("RESOLVED");
            incident.setEndedAt(LocalDateTime.now());

            incidentRepository.save(incident);
            System.out.println("[ALERTA] Incidente RESOLVIDO: " + monitor.getName() + " voltou ao normal.");
            //TODO: enviar email de normalização.
        }
    }

    public List<Incident> getIncidentsByMonitor(Long monitorId) {
        return incidentRepository.findByMonitorId(monitorId);
    }
}