package com.jadeproject.backend.service;

import com.jadeproject.backend.dto.MonitorUpdateDTO;
import com.jadeproject.backend.exception.DataConflictException;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.repository.MonitorRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final UserService userService; //Precisa validar se o dono existe!
    private static final int MIN_INTERVAL_SECONDS = 30;

    public MonitorService(MonitorRepository monitorRepository, UserService userService) {
        this.monitorRepository = monitorRepository;
        this.userService = userService;
    }

    /*Lógica: um usuário não pode ter dois monitores com nomes iguais,mas dois usuários podem ter monitores com nome em comum.
    * Um usuário pode ter dois monitores para a mesma url caso desejar intervalos diferentes.*/

    //Cria um novo monitor para um usuário em específico.
    @Transactional
    public Monitor createMonitor(Monitor monitor, Long userId) {
        //1. Validar se o usuário dono existe
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("Erro: Usuário não encontrado para ID " + userId));

        //2. NOVA VALIDAÇÃO: usuário deve inserir intervalo mínimo de 30 segundos entre pings
        if (monitor.getIntervalSeconds() < MIN_INTERVAL_SECONDS) {
            throw new RuntimeException("O intervalo mínimo permitido é de " + MIN_INTERVAL_SECONDS + " segundos.");
        }

        //3. Validar regra de negócio: nome único (escopo do usuário)
        if (monitorRepository.existsByNameAndUserId(monitor.getName(), userId)) {
            throw new DataConflictException("Você já possui um monitor com o nome '" + monitor.getName() + "'.");
        }

        //4. Validar regra de negócio: url do monitor deve ser única (ESCOPO DO USUÁRIO)
        if (monitorRepository.existsByUrlAndUserId(monitor.getUrl(), userId)) {
            throw new DataConflictException("A URL '" + monitor.getUrl() + "' já está sendo monitorada por você.");
        }

        //5. Vincular monitor ao usuário
        monitor.setUser(user);
        monitor.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("Criando monitor '" + monitor.getName() + "' para o usuário " + user.getUsername());
        return monitorRepository.save(monitor);
    }

    public List<Monitor> findAllByUserId(Long userId) { return monitorRepository.findByUserId(userId); }

    public Optional<Monitor> findById(Long id) { return monitorRepository.findById(id); }

    //Método para deletar (CRUD)
    @Transactional
    public void deleteMonitor(Long id) {
        if (monitorRepository.existsById(id)) {
            monitorRepository.deleteById(id);
        } else {
            throw new RuntimeException("Monitor não encontrado para exclusão.");
        }
    }

    //Update parcial
    @Transactional
    public Monitor updateMonitor(Long id, MonitorUpdateDTO dto) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Monitor não encontrado."));

        Long userId = monitor.getUser().getId();

        //Verifica mudança de NOME
        if (dto.getName() != null && !dto.getName().equals(monitor.getName())) {
            //Se mudou o nome, verifica se o NOVO nome já não existe na lista dele
            if (monitorRepository.existsByNameAndUserId(dto.getName(), userId)) {
                throw new DataConflictException("Você já possui um monitor com o nome '" + dto.getName() + "'.");
            }
            monitor.setName(dto.getName());
        }

        //Verifica mudança de URL
        if (dto.getUrl() != null && !dto.getUrl().equals(monitor.getUrl())) {
            if (monitorRepository.existsByUrlAndUserId(dto.getUrl(), userId)) {
                throw new DataConflictException("A URL '" + dto.getUrl() + "' já está sendo monitorada por você.");
            }
            monitor.setUrl(dto.getUrl());
        }

        //Outros campos simples (sem validação de duplicidade)
        if (dto.getIntervalSeconds() != null) {
            monitor.setIntervalSeconds(dto.getIntervalSeconds());
        }
        if (dto.getIsActive() != null) {
            monitor.setIsActive(dto.getIsActive());
        }

        return monitorRepository.save(monitor);
    }
}