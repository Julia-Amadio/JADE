package com.jadeproject.backend.dto;

//Não colocar @NotBlank na senha
//As anotações de validação (@Size, @Pattern) no Java ignoram valores null por padrão
//Se o usuário mandar password: null -> validação passa, mantem a senha antiga
//Se o usuário mandar password: "123" -> validação falha, muito curta.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @Size(min = 3, max = 50, message = "O usuário deve ter entre 3 e 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Caracteres inválidos no usuário")
    private String username;

    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9@#$%^&+=!]+$", message = "Caractere inválido na senha")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$", message = "Senha fraca")
    private String password;
}
