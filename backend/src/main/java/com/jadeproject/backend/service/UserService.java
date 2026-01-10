package com.jadeproject.backend.service;

import com.jadeproject.backend.model.User;
import com.jadeproject.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service //Indica ao Spring que contém lógica de negócio
public class UserService {

    //Injeção de dependência visa construtor
    //'final', garantindo que nunca vai mudar depois de criado (imutabilidade)
    private final UserRepository userRepository;

    //Spring vê o construtor e entende: "para criar um UserService, preciso de um UserRepository"
    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

    //Registra um novo usuário no sistema. Verifica se já existe antes de salvar.
    public User registerUser(User user) {
        //Regra 1: não permite usuário duplicado
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("ERRO: username '" + user.getUsername() + "' já existe.");
        }

        //Regra 2: não permite email duplicado
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("ERRO: email '" + user.getEmail() + "' já existe.");
        }

        //Regra 3: criptografia de senha (faremos isso de verdade quando instalarmos o Spring Security)
        //Por enquanto, o código só simula o processamento da senha
        //user.setPasswordHash( passwordEncoder.encode(user.getPasswordHash()));

        System.out.println("Salvando usuário via Service: " + user.getUsername());
        return userRepository.save(user);
    }

    //Busca um usuário por ID
    public Optional<User> findById(Long id) { return userRepository.findById(id); }

    //Lista todos os usuários (pada RBAC com admin no futuro)
    public List<User> findAllUsers() { return userRepository.findAll(); }

    //Busca por username
    public Optional<User> findByUsername(String username) { return userRepository.findByUsername(username); }
}