package com.jadeproject.backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.jadeproject.backend.model.User;
import org.springframework.beans.factory.annotation.Value; //Importante
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    //Lê a chave secreta do application.properties (mais seguro)
    //Se não tiver lá, usa o valor padrão "my-secret-key"
    @Value("${api.security.token.secret:my-secret-key}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("JadeProject_API")
                    .withSubject(user.getUsername())
                    .withClaim("role", user.getRole()) //Guarda se é ADMIN ou USER
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("JadeProject_API")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        //Por enquanto, token expira em 24 horas para facilitar testes
        //Diminuir o tempo mais tarde
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
