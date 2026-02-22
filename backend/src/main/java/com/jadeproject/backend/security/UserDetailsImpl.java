package com.jadeproject.backend.security;

import com.jadeproject.backend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails{

    private final User user;

    // Construtor: recebe o User do banco e guarda aqui dentro
    public UserDetailsImpl(User user) {
        this.user = user;
    }

    // Método de Ouro: permite que a gente pegue a entidade User de volta quando precisar!
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash(); //Pega o hash da entidade
    }

    @Override
    public String getUsername() {
        return user.getEmail(); //Truque do email acontece aqui agora
    }

    //Ainda não existe lógica para as validações a seguir
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
