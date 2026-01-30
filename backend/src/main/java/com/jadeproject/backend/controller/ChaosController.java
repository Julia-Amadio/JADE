package com.jadeproject.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/fantoche")
public class ChaosController {

    //1. Sempre UP
    @GetMapping("/up")
    public ResponseEntity<String> alwaysUp() {
        return ResponseEntity.ok("Estou vivo e saudável!");
    }

    //2. Sempre DOWN/500
    @GetMapping("/down")
    public ResponseEntity<String> alwaysDown() {
        return ResponseEntity.internalServerError().body("Deu ruim!");
    }

    //3. Simula timeout
    //O scheduler espera 3s. Esse aqui dorme por 5s. Vai dar erro.
    @GetMapping("/slow")
    public ResponseEntity<String> slowMotion() throws InterruptedException {
        Thread.sleep(5000);
        return ResponseEntity.ok("Acordei... mas tarde demais. zzz");
    }

    //4. Aleatório
    @GetMapping("/random")
    public ResponseEntity<String> randomMood() {
        if (new Random().nextBoolean()) {
            return ResponseEntity.ok("Hoje estou feliz!");
        } else {
            return ResponseEntity.status(503).body("Hoje não quero papo.");
        }
    }
}
