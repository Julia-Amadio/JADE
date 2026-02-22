package com.jadeproject.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O formato do email é inválido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String password;
}
