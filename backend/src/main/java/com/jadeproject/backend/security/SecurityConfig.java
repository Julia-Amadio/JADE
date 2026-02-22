package com.jadeproject.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        //--- 1. ROTAS PÚBLICAS ---
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll() //Cadastro de usuários

                        //--- 2. ÁREA RESTRITA DO ADMIN ---
                        //Apenas admin vê a lista completa de usuários
                        .requestMatchers(HttpMethod.GET, "/users").hasAuthority("ROLE_ADMIN")
                        //Apenas admin deleta usuários
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("ROLE_ADMIN")
                        //Apenas admin vê a lista completa global de monitores (sem filtro de id)
                        .requestMatchers(HttpMethod.GET, "/monitors").hasAuthority("ROLE_ADMIN")
                        //Apenas admin utiliza rotas da busca inteligente (query parameter)
                        .requestMatchers(HttpMethod.GET, "/users/search").hasRole("ADMIN")

                        //--- 3. ROTAS DE USO COMUM (User & Admin) ---
                        //DELETE /monitors/{id}
                        //A segurança de "quem é dono de qual" é feita DENTRO do Controller
                        .requestMatchers(HttpMethod.DELETE, "/monitors/**").authenticated()

                        //POST, PUT e GET /user/{id}
                        //Todos autenticados podem tentar. O Controller verifica se o ID bate
                        .requestMatchers("/monitors/**").authenticated()

                        //Histórico e Incidentes
                        //Admin pode ver tudo, User só o seu. O Controller dessas rotas deve validar
                        .requestMatchers("/history/**").authenticated()
                        .requestMatchers("/incidents/**").authenticated()

                        //Qualquer outra coisa exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
