package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.MonitorCreateDTO;
import com.jadeproject.backend.dto.MonitorResponseDTO;
import com.jadeproject.backend.dto.MonitorUpdateDTO;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.security.UserDetailsImpl;
import com.jadeproject.backend.service.MonitorService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController //Diz ao Spring: "eu respondo requisições HTTP e devolvo JSON"
@RequestMapping("/monitors") //Todas as URLs aqui começam com /monitors
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    //Método auxiliar de segurança
    //Verifica se o usuário logado é o DONO do ID ou se é ADMIN
    private void checkUserPermission(Long targetUserId) {
        //1. Pega a autenticação
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //BLINDAGEM 1: verifica se é do tipo UserDetailsImpl ("crachá")
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        //Pega o crachá e extrai o User real de dentro dele
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        //BLINDAGEM 2: comparação segura de String
        //Em vez de variavel.equals("FIXO"), usa "FIXO".equals(variavel).
        //Se currentUser.getRole() for nulo, isso retorna false em vez de erro.
        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());

        //3. Lógica de permissão
        if (!isAdmin && !currentUser.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar dados de outro usuário.");
        }
    }

    //1. CRIAR UM MONITOR
    //URL: POST http://localhost:8080/monitors/user/{userId}
    //Body (JSON): { "name": "Meu Site", "url": "https://site.com", "intervalSeconds": X }
    @PostMapping("/user/{userId}")
    public ResponseEntity<MonitorResponseDTO> createMonitor(
            @Valid @RequestBody MonitorCreateDTO createDto,
            @PathVariable Long userId) {

        //Verifica se quem está criando é o próprio dono ou Admin
        checkUserPermission(userId);

        //Converte DTO -> Entity
        Monitor monitorEntity = new Monitor();
        monitorEntity.setName(createDto.getName());
        monitorEntity.setUrl(createDto.getUrl());
        monitorEntity.setIntervalSeconds(createDto.getIntervalSeconds());

        //CLEAN CODE: só altera o status se o usuário EXPLICITAMENTE mandou algo (ex: false)
        //Se vier null no DTO, ignora e mantém o 'true' padrão do Java
        if (createDto.getIsActive() != null) {
            monitorEntity.setIsActive(createDto.getIsActive());
        }

        //Salva
        Monitor savedMonitor = monitorService.createMonitor(monitorEntity, userId);

        //Retorna DTO, agora com 201 (Created)
        //201 é o padrão REST para métodos POST que criam registros. O 200 é genérico demais
        //Usar 201 ajuda quem consome a API a saber exatamente que um registro novo nasceu
        return ResponseEntity.status(201).body(toResponseDTO(savedMonitor));
    }

    //2. LISTAR MONITORES DE UM USUÁRIO (agora retorna lista de DTOs)
    //URL: GET http://localhost:8080/monitors/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MonitorResponseDTO>> getMonitorsByUser(@PathVariable Long userId) {
        //Verifica se quem está pedindo a lista é o dono ou Admin
        checkUserPermission(userId);

        List<Monitor> monitors = monitorService.findAllByUserId(userId);

        //Stream do Java 8 para converter a lista inteira de Entity para DTO
        List<MonitorResponseDTO> dtos = monitors.stream()
                .map(this::toResponseDTO) //Chama o método auxiliar pra cada um
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //3. DELETAR MONITOR (não precisa de DTO pois não retorna corpo)
    //URL: DELETE http://localhost:8080/monitors/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        //1. Busca o monitor para saber quem é o dono
        Monitor existingMonitor = monitorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitor não encontrado"));

        //2. Descobre o ID do dono
        Long ownerId = existingMonitor.getUser().getId();

        //3. Verifica permissão (se é o dono OU se é ADMIN)
        //O método checkUserPermission já tem a lógica.
        checkUserPermission(ownerId);

        //4. Se passou, deleta
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build();
    }

    //4. UPDATE MONITOR
    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponseDTO> updateMonitor(@PathVariable Long id,
                                                            @Valid @RequestBody MonitorUpdateDTO updateDto) {
        //PROBLEMA ANTERIOR: o update recebe ID do Monitor, não do Usuário.
        //Solução rápida: buscar o monitor para saber de quem ele é
        Monitor existingMonitor = monitorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitor não encontrado"));

        //Pega o ID do dono daquele monitor
        Long ownerId = existingMonitor.getUser().getId();

        //Verifica se o usuário logado é o dono desse monitor
        checkUserPermission(ownerId);

        //Se passou, manda pro serviço atualizar
        Monitor updatedMonitor = monitorService.updateMonitor(id, updateDto);
        return ResponseEntity.ok(toResponseDTO(updatedMonitor));
    }

    //5. LISTAR TODOS OS MONITORES (rota global)
    //URL: GET http://localhost:8080/monitors
    @GetMapping //Sem nada entre parênteses, ele assume a rota raiz da classe (/monitors)
    public ResponseEntity<List<MonitorResponseDTO>> getAllMonitors() {
        //Busca tudo no banco via Service
        //A segurança desta rota é feita exclusivamente no SecurityConfig (.hasAuthority("ROLE_ADMIN"))
        List<Monitor> monitors = monitorService.getAllMonitors();

        List<MonitorResponseDTO> dtos = monitors.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //Mapper auxiliar Entity -> ResponseDTO
    private MonitorResponseDTO toResponseDTO(Monitor monitor) {
        MonitorResponseDTO dto = new MonitorResponseDTO();
        dto.setId(monitor.getId());
        dto.setName(monitor.getName());
        dto.setUrl(monitor.getUrl());
        dto.setIntervalSeconds(monitor.getIntervalSeconds());
        dto.setIsActive(monitor.getIsActive());
        dto.setLastChecked(monitor.getLastChecked());
        dto.setCreatedAt(monitor.getCreatedAt());

        //Pega só o ID do usuário para não travar o JSON
        if (monitor.getUser() != null) {
            dto.setUserId(monitor.getUser().getId());
        }

        return dto;
    }
}
