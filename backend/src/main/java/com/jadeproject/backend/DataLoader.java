package com.jadeproject.backend;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
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
                                   MonitorService monitorService) {
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
                newUser.setPasswordHash("123456"); //Futuro: será hash real

                user = userService.registerUser(newUser);
                System.out.println("Usuário criado: " + user.getUsername());
            } else {
                user = existingUser.get();
                System.out.println("[ERRO] Usuário '" + user.getUsername() + "' já existe!");
            }

            //2. VERIFICAR SE JÁ EXISTEM MONITORES (para não duplicar)
            if (monitorService.findAllByUserId(user.getId()).isEmpty()) {
                System.out.println("Criando monitores de teste...");

                createMonitor(monitorService, user, "Google Check", "https://google.com");
                createMonitor(monitorService, user, "Fantoche - UP", "http://localhost:8080/fantoche/up");
                createMonitor(monitorService, user, "Fantoche - DOWN", "http://localhost:8080/fantoche/down");
                createMonitor(monitorService, user, "Fantoche - TIMEOUT", "http://localhost:8080/fantoche/slow");
                createMonitor(monitorService, user, "Fantoche - CAOS", "http://localhost:8080/fantoche/random");

                System.out.println("Todos os monitores de teste foram criados!");
            } else {
                System.out.println("Monitores já existem no banco. Pulando criação.");
            }

            System.out.println("Carga de dados finalizada. O scheduler vai pegar esses dados em breve.");
        };
    }

    //Método auxiliar para não repetir código
    private void createMonitor(MonitorService service, User user, String name, String url) {
        Monitor monitor = new Monitor();
        monitor.setName(name);
        monitor.setUrl(url);
        monitor.setIntervalSeconds(60);
        service.createMonitor(monitor, user.getId());
        System.out.println("   -> Monitor cadastrado: " + name);
    }
}