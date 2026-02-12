package com.jadeproject.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data //Gera getters e setters..
public class UserCreateDTO {

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 3, max = 50, message = "O usuário deve ter entre 3 e 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "O usuário só pode conter letras, números, ponto, traço e underline")
    private String username;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    //O campo chama 'password' (o que o user digita), não 'passwordHash'
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    //@Pattern(regexp = "^[a-zA-Z0-9@#$%^&+=!]+$", message = "Caractere inválido na senha")
    //Removida regex acima pois é muito restritiva (não permite espaço, ç, ~, etc..)
    //Estava sendo utilizado para não permitir emojis. Porém, por sugestão do Gemini (risos..), é melhor deixar o BCrypt lidar com os caracteres.
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$", message = "Senha fraca: precisa de maiúscula, minúscula e número")
    private String password;
}