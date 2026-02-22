package com.jadeproject.backend.service;

import com.jadeproject.backend.model.User;
import com.jadeproject.backend.repository.UserRepository;
import com.jadeproject.backend.security.UserDetailsImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private final UserRepository repository;

    public AuthorizationService(UserRepository repository) {
        this.repository = repository;
    }

    /* * O Spring Security chama este método automaticamente quando alguém tenta logar.
     * O parâmetro chama 'username' por padrão da interface, mas nós vamos usar o EMAIL.
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        //Busca o usuário no banco
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));

        //Empacota no crachá de segurança e devolve
        return new UserDetailsImpl(user);
    }
}
