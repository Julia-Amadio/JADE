package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.MonitorCreateDTO;
import com.jadeproject.backend.dto.MonitorResponseDTO;
import com.jadeproject.backend.dto.MonitorUpdateDTO;
import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.service.MonitorService;

/*Classe que representa a Resposta HTTP completa
* Quando entregamos algo para o usuário, não entregamos só o dado (o JSON), e sim um "pacote" que contém:
*   1. O corpo (body): o dado em si (ex: o objeto Monitor).
*   2. Status Code: número que diz o que aconteceu (200 = OK, 404 = Não encontrado, 201 = Criado).
*   3. Headers: informações extras (ex: tipo de conteúdo, cache).*/
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

//Traz as "etiquetas" (anotações) usadas para transformar métodos em URLs
/*@RestController: diz "esta classe atende a web".
* @RequestMapping: diz "qual é a URL base" (/monitors).
* @GetMapping: "atenda requisições do tipo GET aqui".
* @PostMapping: "atenda requisições do tipo POST aqui".
* @PathVariable: "pegue o valor que está na URL" (ex: o id em /monitors/5).
* @RequestBody: "pegue o JSON que veio no corpo da requisição e transforme em Objeto Java".*/
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController //Diz ao Spring: "eu respondo requisições HTTP e devolvo JSON"
@RequestMapping("/monitors") //Todas as URLs aqui começam com /monitors
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    //1. CRIAR UM MONITOR
    //URL: POST http://localhost:8080/monitors/user/{userId}
    //Body (JSON): { "name": "Meu Site", "url": "https://site.com", "intervalSeconds": X }
    @PostMapping("/user/{userId}")
    public ResponseEntity<MonitorResponseDTO> createMonitor(
            @Valid @RequestBody MonitorCreateDTO createDto,
            @PathVariable Long userId) {

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
        List<Monitor> monitors = monitorService.findAllByUserId(userId);

        // Stream do Java 8 para converter a lista inteira de Entity para DTO
        List<MonitorResponseDTO> dtos = monitors.stream()
                .map(this::toResponseDTO) // Chama o método auxiliar pra cada um
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //3. DELETAR MONITOR (não precisa de DTO pois não retorna corpo)
    //URL: DELETE http://localhost:8080/monitors/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build();
    }

    //4. UPDATE MONITOR
    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponseDTO> updateMonitor(@PathVariable Long id,
                                                            @Valid @RequestBody MonitorUpdateDTO updateDto) {
        Monitor updatedMonitor = monitorService.updateMonitor(id, updateDto);
        return ResponseEntity.ok(toResponseDTO(updatedMonitor));
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