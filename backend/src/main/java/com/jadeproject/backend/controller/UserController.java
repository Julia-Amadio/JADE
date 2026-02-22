package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.UserCreateDTO;
import com.jadeproject.backend.dto.UserResponseDTO;
import com.jadeproject.backend.dto.UserUpdateDTO;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.security.UserDetailsImpl;
import com.jadeproject.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //--- Método de segurança, como no MonitorController ---
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

        //BLINDAGEM 2: Yoda Condition (constante à esquerda)
        boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole());

        //3. Verifica permissão
        //Note: currentUser.getId() dificilmente será null se veio do banco, mas targetUserId pode vir null da URL?
        //O Spring geralmente barra Long null na URL, mas a comparação segura é boa prática.
        if (!isAdmin && !currentUser.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para alterar dados de outro usuário.");
        }
    }

    //--- Método de segurança EXCLUSIVO PARA ADMIN ---
    private void checkAdminPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //BLINDAGEM 1: verifica se está logado usando "crachá"
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        //BLINDAGEM 2: verifica se é ADMIN
        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado. Apenas administradores podem realizar buscas globais.");
        }
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

    //2. BUSCAR USUÁRIO (Painel Admin: por username OU email)
    //Exemplos de URLs válidas:
    //GET http://localhost:8080/users/search?username=julia.jade
    //GET http://localhost:8080/users/search?email=julia@jade.com
    @GetMapping("/search")
    public ResponseEntity<UserResponseDTO> searchUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        //1. SEGURANÇA
        checkAdminPermission();

        //2. Lógica inteligente de busca
        if (username != null && !username.isBlank()) {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
            return ResponseEntity.ok(toResponseDTO(user));
        } else if (email != null && !email.isBlank()) {
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
            return ResponseEntity.ok(toResponseDTO(user));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forneça um 'username' ou 'email' para realizar a busca.");
        }
    }

    //3. UPDATE USUÁRIO
    //Add segurança, da mesma forma
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UserUpdateDTO updateDto) {
        //Verifica se o ID da URL pertence ao usuário logado
        checkUserPermission(id);

        User updatedUser = userService.updateUser(id, updateDto);
        return ResponseEntity.ok(toResponseDTO(updatedUser));
    }

    //4. LISTAR TODOS OS USUÁRIOS (ADMIN ONLY, protegido pelo SecurityConfig)
    //URL: GET http://localhost:8080/users
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        //Busca a lista de entidades
        List<User> users = userService.findAllUsers();

        //Converte para DTO para não vazar a senha (passwordHash)
        List<UserResponseDTO> dtos = users.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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
