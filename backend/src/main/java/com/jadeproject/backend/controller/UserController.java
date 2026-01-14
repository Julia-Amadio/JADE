package com.jadeproject.backend.controller;

import com.jadeproject.backend.model.User;
import com.jadeproject.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //1. CRIAR USUÁRIO
    //URL: POST http://localhost:8080/users
    @PostMapping
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User newUser = userService.registerUser(user);
        return ResponseEntity.ok(newUser);
    }

    //2. BUSCAR POR USERNAME (simula Login por enquanto)
    //URL: GET http://localhost:8080/users/{username}
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok) //Se achar, retorna 200 OK com o user
                .orElse(ResponseEntity.notFound().build()); //Se não, 404 Not Found
    }
}
