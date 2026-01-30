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

    //Injeção de dependência via construtor
    public MonitorScheduler(MonitorRepository monitorRepository){
        this.monitorRepository = monitorRepository;
    }

    //Roda a cada 60 segundos (60000ms) após o término da última execução
    //Alterando temporariamente para 10seg para testes
    @Scheduled(fixedDelay = 10000)
    public void checkMonitors() {
        log.info("------- INICIANDO VERIFICAÇÃO DE MONITORES -------");
        List<Monitor> monitors = monitorRepository.findAll();

        if (monitors.isEmpty()) {
            log.info("Nenhum monitor cadastrado!");
        }

        for (Monitor monitor: monitors) {
            boolean isUp = pingUrl(monitor.getUrl());

            if(isUp) {
                log.info("^ [UP] {} ({})", monitor.getName(), monitor.getUrl());
            } else {
                log.error("X [DOWN] {} ({})", monitor.getName(), monitor.getUrl());
                //Futuro: salvar no banco o Incidente
            }
        }

        log.info("--------------- FIM DA VERIFICAÇÃO ---------------");
    }

    //Método auxiliar simples para testar a conexão (java puro)
    private boolean pingUrl(String urlAddress) {
        try {
            URL url = URI.create(urlAddress).toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); //Espera no máx 3s para conectar
            connection.setReadTimeout(3000);    //Espera no máx 3s para ler

            int responseCode = connection.getResponseCode();

            //Considera UP se o código for 200 e 299
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            //Se der erro de DNS, timeout ou qualquer exceção, considera DOWN
            return false;
        }
    }
}