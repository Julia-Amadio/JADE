package com.jadeproject.backend.security;

import com.jadeproject.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        var token = this.recoverToken(request);

        if (token != null) {
            //1. O TokenService valida e devolve o 'subject' (que é o EMAIL)
            var login = tokenService.validateToken(token);

            //2. BUSCA por esse email e SÓ AUTENTICA se o usuário de fato existir
            if (login != null && !login.isEmpty()) {
                userRepository.findByEmail(login).ifPresent(user -> {
                    //Cria o "crachá de segurança" com o usuário dentro
                    UserDetailsImpl userDetails = new UserDetailsImpl(user);

                    //Autentica passando o CRACHÁ (userDetails)
                    var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
