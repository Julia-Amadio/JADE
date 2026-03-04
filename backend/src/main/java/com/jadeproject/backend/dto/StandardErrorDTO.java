package com.jadeproject.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder //Ajuda a instanciar o objeto facilmente
public class StandardErrorDTO {
    private OffsetDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    //Usado apenas para erros de validação (onde temos erros específicos por campo)
    private Map<String, String> validationErrors;
}
