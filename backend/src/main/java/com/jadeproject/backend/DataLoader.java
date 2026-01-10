package com.jadeproject.backend;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.service.IncidentService;
import com.jadeproject.backend.service.MonitorHistoryService;
import com.jadeproject.backend.service.MonitorService;
import com.jadeproject.backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(UserService userService,
                                   MonitorService monitorService,
                                   MonitorHistoryService historyService,
                                   IncidentService incidentService) {
        return args -> {
            System.out.println("Iniciando carga de dados via Services...");

            //1. CRIAR USUÁRIO
            //Usa findByUsername para não tentar criar duplicado e tomar erro do Service
            User user;
            Optional<User> existingUser = userService.findByUsername("julia_admin");

            if (existingUser.isEmpty()) {
                User newUser = new User();
                newUser.setUsername("julia_admin");
                newUser.setEmail("julia@jade.com");
                newUser.setPasswordHash("123456"); // Futuro: será hash real

                user = userService.registerUser(newUser);
                System.out.println("Usuário criado: " + user.getUsername());
            } else {
                user = existingUser.get();
                System.out.println("[ERRO] Usuário '" + user.getUsername() + "' já existe!");
            }

            //2. CRIAR MONITOR
            //Verifica se o usuário já tem monitores para não duplicar no teste
            if (monitorService.findAllByUserId(user.getId()).isEmpty()) {
                Monitor monitor = new Monitor();
                monitor.setName("Google Check");
                monitor.setUrl("https://google.com");
                monitor.setIntervalSeconds(60);
                //Setar tipo de URL monitorada no futuro
                //monitor.setType("HTTP");

                //O Service vai vincular ao usuário e validar regras
                Monitor savedMonitor = monitorService.createMonitor(monitor, user.getId());
                System.out.println("Monitor criado: " + savedMonitor.getName());

                //3. GERAR HISTÓRICO (Simulação)
                System.out.println("Gerando logs de histórico...");

                //No futuro, é correto que Service use 'isUp' internamente para constatar se o site está "em pé" na emissão do log
                //Simula 3 checks com sucesso (200 OK)
                historyService.saveLog(savedMonitor, 200, 120/*, true*/);
                historyService.saveLog(savedMonitor, 200, 115/*, true*/);
                historyService.saveLog(savedMonitor, 200, 140/*, true*/);

                //Simula 1 falha (500 Error)
                historyService.saveLog(savedMonitor, 500, 0/*, true*/);

                System.out.println("[SUCESSO] 4 Logs de histórico gerados.");

                //4. GERAR INCIDENTE
                //Simulando que o site caiu
                System.out.println("Simulando queda do monitor...");
                incidentService.handleDownEvent(savedMonitor, "Erro 500 - Internal Server Error");

                //--- PAUSA DE 30 SEGUNDOS ---
                try {
                    System.out.println("Aguardando 30 segundos antes de recuperar o sistema...");
                    System.out.println("Verificar o banco AGORA para ver o status OPEN");

                    Thread.sleep(30000); //30000 milissegundos = 30 segundos
                } catch (InterruptedException e) {
                    System.out.println("A pausa foi interrompida!");
                    Thread.currentThread().interrupt(); //Boa prática: restaurar o estado de interrupção
                }
                //----------------------------

                //5. TESTANDO FECHAMENTO
                incidentService.handleUpEvent(savedMonitor);
                System.out.println("Verificar o banco AGORA para ver o status RESOLVED");
            } else {
                System.out.println("O usuário já possui monitores. Pulando criação de dados de teste.");
            }

            System.out.println("Carga de dados finalizada!");
        };
    }
}