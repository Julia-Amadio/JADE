package com.jadeproject.backend.controller;

import com.jadeproject.backend.model.Monitor;
import com.jadeproject.backend.service.MonitorService;

/*Classe que representa a Resposta HTTP completa
* Quando entregamos algo para o usuário, não entregamos só o dado (o JSON), e sim um "pacote" que contém:
*   1. O corpo (body): o dado em si (ex: o objeto Monitor).
*   2. Status Code: número que diz o que aconteceu (200 = OK, 404 = Não encontrado, 201 = Criado).
*   3. Headers: informações extras (ex: tipo de conteúdo, cache).*/
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

@RestController //Diz ao Spring: "eu respondo requisições HTTP e devolvo JSON"
@RequestMapping("/monitors") //Todas as URLs aqui começam com /monitors
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    //1. CRIAR UM MONITOR
    //URL: POST http://localhost:8080/monitors/user/{userId}
    //Body (JSON): { "name": "Meu Site", "url": "https://site.com", "intervalSeconds": 60 }
    @PostMapping("/user/{userId}")
    public ResponseEntity<Monitor> createMonitor(@RequestBody Monitor monitor, @PathVariable Long userId) {
        Monitor createdMonitor = monitorService.createMonitor(monitor, userId);
        return ResponseEntity.ok(createdMonitor); //Retorna 200 OK com o objeto criado
    }

    //2. LISTAR MONITORES DE UM USUÁRIO
    //URL: GET http://localhost:8080/monitors/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Monitor>> getMonitorsByUser(@PathVariable Long userId) {
        List<Monitor> monitors = monitorService.findAllByUserId(userId);
        return ResponseEntity.ok(monitors);
    }

    //3. DELETAR MONITOR
    //URL: DELETE http://localhost:8080/monitors/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build(); //Retorna 204 No Content (sucesso sem corpo)
    }
}
