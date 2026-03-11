package com.jadeproject.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica para TODOS os endpoints (rotas) da sua API
                .allowedOrigins(
                        "http://localhost:5173", //URL padrão do Vite (React/Vue)
                        "http://localhost:3000"  //URL padrão do Create React App/Next.js
                        //No futuro add aqui a URL do frontend hospedado (ex: https://meu-jade.vercel.app)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Métodos permitidos
                .allowedHeaders("*") //Permite o envio de qualquer Header (incluindo o header de Authorization com o Token JWT futuramente)
                .allowCredentials(true); //Permite o envio de cookies ou credenciais de autenticação
    }
}
