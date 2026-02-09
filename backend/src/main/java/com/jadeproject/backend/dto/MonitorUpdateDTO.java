package com.jadeproject.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class MonitorUpdateDTO {

    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String name;

    @URL(message = "Deve ser uma URL válida")
    private String url;

    @Min(value = 30, message = "O intervalo mínimo é 30 segundos")
    @Max(value = 86400, message = "O intervalo máximo é 1 dia (86400s)")
    private Integer intervalSeconds;

    private Boolean isActive;
}
