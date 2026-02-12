package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.UserCreateDTO;
import com.jadeproject.backend.dto.UserResponseDTO;
import com.jadeproject.backend.dto.UserUpdateDTO;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.service.UserService;
import jakarta.validation.Valid;
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
    //Recebe DTO, devolve DTO
    @PostMapping
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserCreateDTO createDto) {
        //Converte DTO de Entrada para Entity (manual mapper)
        User userEntity = new User();
        userEntity.setUsername(createDto.getUsername());
        userEntity.setEmail(createDto.getEmail());
        userEntity.setPasswordHash(createDto.getPassword()); //Service vai hashear isso

        //Chama o Service (logica de negócio)
        User savedUser = userService.registerUser(userEntity);

        //Converte Entity salva para DTO de resposta (esconde a senha)
        return ResponseEntity.status(201).body(toResponseDTO(savedUser));
    }

    //2. BUSCAR POR USERNAME
    //URL: GET http://localhost:8080/users/{username}
    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(toResponseDTO(user))) //Se achar, converte
                .orElse(ResponseEntity.notFound().build());
    }

    //3. UPDATE USUÁRIO
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UserUpdateDTO updateDto) {
        User updatedUser = userService.updateUser(id, updateDto);
        return ResponseEntity.ok(toResponseDTO(updatedUser));
    }

    //Método auxiliar para converter Entity -> ResponseDTO
    private UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
