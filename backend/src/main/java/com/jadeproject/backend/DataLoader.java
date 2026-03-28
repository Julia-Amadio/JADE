package com.jadeproject.backend;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.service.MonitorService;
import com.jadeproject.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@Slf4j
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(UserService userService,
                                   MonitorService monitorService) {
        return args -> {
            log.info("Iniciando carga de dados via Services...");

            //1. CRIAR USUÁRIO
            //Usa findByUsername para não tentar criar duplicado e tomar erro do Service
            User user;
            Optional<User> existingUser = userService.findByUsername("jade_admin");

            if (existingUser.isEmpty()) {
                User newUser = new User();
                newUser.setUsername("jade_admin");
                newUser.setEmail("jadeadmin@example.com");
                newUser.setPasswordHash("SenhaForte123!");
                newUser.setRole("ROLE_ADMIN");

                user = userService.registerUser(newUser);
                log.info("Usuário criado: {}", user.getUsername());
            } else {
                user = existingUser.get();
                log.info("[ERRO] Usuário '{}' já existe!", user.getUsername());
            }

            //2. VERIFICAR SE JÁ EXISTEM MONITORES (para não duplicar)
            if (monitorService.findAllByUserId(user.getId()).isEmpty()) {
                log.info("Criando monitores de teste...");

                createMonitor(monitorService, user, "Google Check", "https://google.com");
                createMonitor(monitorService, user, "Fantoche - UP", "http://localhost:8080/fantoche/up");
                createMonitor(monitorService, user, "Fantoche - DOWN", "http://localhost:8080/fantoche/down");
                createMonitor(monitorService, user, "Fantoche - TIMEOUT", "http://localhost:8080/fantoche/slow");
                createMonitor(monitorService, user, "Fantoche - CAOS", "http://localhost:8080/fantoche/random");

                log.info("Todos os monitores de teste foram criados!");
            } else {
                log.info("Monitores já existem no banco. Pulando criação.");
            }

            log.info("Carga de dados finalizada. O scheduler vai pegar esses dados em breve.");
        };
    }

    //Método auxiliar para não repetir código
    private void createMonitor(MonitorService service, User user, String name, String url) {
        Monitor monitor = new Monitor();
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setIntervalSeconds(60);
        service.createMonitor(monitor, user.getId());
        log.info("   -> Monitor cadastrado: {}", name);
    }
}
