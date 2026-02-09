package com.jadeproject.backend.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class MonitorResponseDTO {
    private Long id;
    private String name;
    private String url;
    private Integer intervalSeconds;
    private Boolean isActive;
    private OffsetDateTime lastChecked;
    private OffsetDateTime createdAt;

    //Opcional: se o front precisar saber de qual user é esse monitor,
    //manda só o ID, não o objeto User inteiro
    private Long userId;
}
