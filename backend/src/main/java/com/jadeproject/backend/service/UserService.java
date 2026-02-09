package com.jadeproject.backend.service;

import com.jadeproject.backend.dto.UserUpdateDTO;
import com.jadeproject.backend.exception.DataConflictException;
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
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DataConflictException("O nome de usuário '" + user.getUsername() + "' já está em uso.");
        }

        //Regra 2: não permite email duplicado
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DataConflictException("O e-mail '" + user.getEmail() + "' já está cadastrado.");
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

    public User updateUser(Long id, UserUpdateDTO dto) {
        //Busca o usuário (se não achar, erro)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        //Atualiza campos APENAS se não forem nulos ou duplicados
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new DataConflictException("Este nome de usuário já está em uso.");
            }
            user.setUsername(dto.getUsername());
        }
        //Se estiver trocando de email, verifica se o NOVO email já não é de outra pessoa
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new DataConflictException("Este e-mail já está em uso por outro usuário.");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            //Aqui entra Hash no futuro. Por enquanto, atribuição direta
            user.setPasswordHash(dto.getPassword());
        }

        //Salva e retorna
        return userRepository.save(user);
    }
}