package com.jadeproject.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class MonitorCreateDTO {

    @NotBlank(message = "O nome do monitor é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String name;

    @NotBlank(message = "A URL é obrigatória")
    @URL(message = "Deve ser uma URL válida (ex: https://google.com)")
    private String url;

    @Min(value = 30, message = "O intervalo mínimo é 30 segundos")
    @Max(value = 86400, message = "O intervalo máximo é 1 dia (86400s)")
    private Integer intervalSeconds;

    private Boolean isActive; //Opcional, o PrePersist da Entity garante true se vier null
}
