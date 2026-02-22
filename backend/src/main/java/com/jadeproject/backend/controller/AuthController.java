package com.jadeproject.backend.controller;

import com.jadeproject.backend.dto.LoginRequestDTO;
import com.jadeproject.backend.dto.LoginResponseDTO;
import com.jadeproject.backend.model.User;
import com.jadeproject.backend.security.TokenService;

import com.jadeproject.backend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {

        //1. Encapsula email e senha num token "não autenticado"
        //O Spring Security usa esse objeto para trafegar as credenciais
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.getEmail(), data.getPassword());

        //2. Tenta autenticar
        //O Manager chama o AuthorizationService -> Busca no BD -> Compara hash da senha
        //Se a senha estiver errada, o Spring lança uma exceção automaticamente (403 Forbidden)
        var auth = this.authenticationManager.authenticate(usernamePassword);

        //3. Se chegou aqui, a senha está correta
        //Pegamos o crachá e extraímos o User real de dentro dele
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        User user = userDetails.getUser();

        //4. Gera o Token JWT
        String token = tokenService.generateToken(user);

        //5. Retornamos o DTO de resposta com Email, Nome e Token
        return ResponseEntity.ok(new LoginResponseDTO(user.getEmail(), user.getUsername(), token));
    }
}
