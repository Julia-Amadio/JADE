package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.repository.MonitorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.util.List;

@Slf4j //Do Lombok para criar logs automaticamente
@Service
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final MonitorHistoryService historyService;
    private final IncidentService incidentService; //DIFF: nova dependência

    //Injeção de dependência via construtor. DIFF: add o incidentService
    public MonitorScheduler(MonitorRepository monitorRepository,
                            MonitorHistoryService historyService,
                            IncidentService incidentService){
        this.monitorRepository = monitorRepository;
        this.historyService = historyService;
        this.incidentService = incidentService;
    }

    //Roda a cada 10 segundos (10000ms) após o término da última execução
    @Scheduled(fixedDelay = 10000)
    public void checkMonitors() {
        //1. O banco filtra e só traz quem precisa rodar
        List<Monitor> monitors = monitorRepository.findMonitorsToProcess();

        if (monitors.isEmpty()) {
            return; //Ninguém pra rodar, volta a dormir.
        }

        log.info("Verificando {} monitores...", monitors.size());

        for (Monitor monitor: monitors) {
            long startTime = System.currentTimeMillis();
            int statusCode = pingUrl(monitor.getUrl());
            long endTime = System.currentTimeMillis();

            int responseTime = (int) (endTime - startTime);
            boolean isUp = statusCode >= 200 && statusCode < 300;

            //Salva no banco de dados
            //Passa o monitor, o HTTP Status Code, tempo de resposta e se houve sucesso
            historyService.saveLog(monitor, statusCode, responseTime, isUp);

            if(isUp) {
                log.info("^ [UP] {} ({}) - Status: {} - Tempo: {}ms",
                        monitor.getName(), monitor.getUrl(), statusCode, responseTime);

                //INTEGRAÇÃO: se está UP, tenta resolver incidentes abertos
                incidentService.handleUpEvent(monitor);
            } else {
                //Se for 0, é erro de conexão (timeout/dns). Se for > 0, é erro HTTP (500, 404)
                String statusMsg = (statusCode == 0) ? "FALHA DE CONEXÃO" : String.valueOf(statusCode);
                log.error("X [DOWN] {} ({}) - Status: {} - Tempo: {}ms",
                        monitor.getName(), monitor.getUrl(), statusMsg, responseTime);

                //INTEGRAÇÃO: Se está DOWN, tenta criar um incidente
                //Formatamos uma mensagem amigável para a descrição do incidente
                String errorReason = (statusCode == 0) ? "Timeout ou Erro de DNS" : "Erro HTTP " + statusCode;
                incidentService.handleDownEvent(monitor, errorReason);
                /*OPERADOR TERNÁRIO: atalho elegante para escrever 'if-else' em uma única linha
                * Variavel = (Condição) ? Valor_se_Verdadeiro : Valor_se_Falso;
                * ? significa "ENTÃO" e : significa "SENÃO"
                * Se o código for 0 -> Salva "Timeout..."
                * Se o código for 500 -> "Erro HTTP 500"*/
            }

            monitor.setLastChecked(OffsetDateTime.now(ZoneOffset.UTC));
            monitorRepository.save(monitor);
        }

        log.info("--------------- FIM DA VERIFICAÇÃO ---------------");
    }

    //Método auxiliar simples para testar a conexão (java puro)
    //DIFF: boolean para int
    private int pingUrl(String urlAddress) {
        try {
            URL url = URI.create(urlAddress).toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); //Espera no máx 3s para conectar
            connection.setReadTimeout(3000);    //Espera no máx 3s para ler

            //Retorna o código real (ex: 200, 404, 500)
            return connection.getResponseCode();

        } catch (Exception e) {
            //Se der erro de DNS, timeout ou qualquer exceção, não tem código HTTP
            //Retorna 0 para indicar "sem resposta"
            return 0;
        }
    }
}
