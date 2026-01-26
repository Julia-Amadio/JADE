package com.jadeproject.backend.service;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.repository.MonitorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final UserService userService; //Precisa validar se o dono existe!

    public MonitorService(MonitorRepository monitorRepository, UserService userService) {
        this.monitorRepository = monitorRepository;
        this.userService = userService;
    }

    /*Lógica: um usuário não pode ter dois monitores com nomes iguais,mas dois usuários podem ter monitores com nome em comum.
    * Um usuário pode ter dois monitores para a mesma url caso desejar intervalos diferentes.*/

    //Cria um novo monitor para um usuário em específico.
    public Monitor createMonitor(Monitor monitor, Long userId) {
        //1. Validar se o usuário dono existe
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("Erro: Usuário não encontrado para ID " + userId));

        //2. Validar regra de negócio: nome único (escopo do usuário)
        boolean nameAlreadyExists = monitorRepository.existsByNameAndUserId(monitor.getName(), userId);
        if (nameAlreadyExists) {
            throw new RuntimeException("[ERRO] Você já possui um monitor chamado '" + monitor.getName() + "'.");
        }

        //3. [NOVO] Validar regra de negócio: url do monitor deve ser única (ESCOPO DO USUÁRIO)
        boolean urlAlreadyExists = monitorRepository.existsByUrlAndUserId(monitor.getUrl(), userId);
        if (urlAlreadyExists) {
            throw new RuntimeException("[ERRO] Você já está monitorando a URL '" + monitor.getUrl() + "'.");
        }

        //4. Vincular monitor ao usuário
        monitor.setUser(user);

        //5. Validar se a URL é válida/formatada corretamente. Possível usar UrlValidator aqui no futuro
        System.out.println("Criando monitor '" + monitor.getName() + "' para o usuário " + user.getUsername());
        return monitorRepository.save(monitor);
    }

    public List<Monitor> findAllByUserId(Long userId) { return monitorRepository.findByUserId(userId); }

    public Optional<Monitor> findById(Long id) { return monitorRepository.findById(id); }

    //Método para deletar (CRUD)
    public void deleteMonitor(Long id) {
        if (monitorRepository.existsById(id)) {
            monitorRepository.deleteById(id);
        } else {
            throw new RuntimeException("Monitor não encontrado para exclusão.");
        }
    }
}