package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.repository.MonitorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.util.List;

@Slf4j //Do Lombok para criar logs automaticamente
@Service
public class MonitorScheduler {

    private final MonitorRepository monitorRepository;
    private final MonitorHistoryService historyService; //DIFF: nova dependência

    //Injeção de dependência via construtor. DIFF: add o historyService
    public MonitorScheduler(MonitorRepository monitorRepository,
                            MonitorHistoryService historyService){
        this.monitorRepository = monitorRepository;
        this.historyService = historyService;
    }

    //Roda a cada 10 segundos (10000ms) após o término da última execução
    @Scheduled(fixedDelay = 10000)
    public void checkMonitors() {
        log.info("------- INICIANDO VERIFICAÇÃO DE MONITORES -------");
        List<Monitor> monitors = monitorRepository.findAll();

        if (monitors.isEmpty()) {
            log.info("Nenhum monitor cadastrado!");
        }

        for (Monitor monitor: monitors) {
            //1. Iniciamos o cronômetro
            long startTime = System.currentTimeMillis();

            int statusCode = pingUrl(monitor.getUrl());

            //2. Paramos o cronômetro
            long endTime = System.currentTimeMillis();

            //Calcula a duração -> latência.
            int responseTime = (int) (endTime - startTime);

            //Sucesso é entre 200 e 299
            boolean isUp = statusCode >= 200 && statusCode < 300;

            //3. Salva no banco de dados
            //Passa o monitor, o HTTP Status Code, tempo de resposta e se houve sucesso
            historyService.saveLog(monitor, statusCode, responseTime, isUp);

            if(isUp) {
                //DIFF: add tempo de resposta
                log.info("^ [UP] {} ({}) - Status: {} - Tempo: {}ms",
                        monitor.getName(), monitor.getUrl(), statusCode, responseTime);
            } else {
                //Se for 0, é erro de conexão (timeout/dns). Se for > 0, é erro HTTP (500, 404)
                String statusMsg = (statusCode == 0) ? "FALHA DE CONEXÃO" : String.valueOf(statusCode);
                log.error("X [DOWN] {} ({}) - Status: {} - Tempo: {}ms",
                        monitor.getName(), monitor.getUrl(), statusMsg, responseTime);

                //TODO: chamar aqui o incidentService.handleDownEvent(...)
            }
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