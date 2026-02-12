package com.jadeproject.backend.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class MonitorHistoryResponseDTO {
    
    private Long id;
    private Integer statusCode;
    private Integer latency;
    private Boolean isSuccessful;
    private OffsetDateTime checkedAt;

    //Opcional: mandar o ID do monitor de volta é útil em listas mistas
    private Long monitorId;
}
