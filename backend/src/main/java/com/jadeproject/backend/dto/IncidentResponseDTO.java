package com.jadeproject.backend.dto;

import com.jadeproject.backend.model.Monitor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class IncidentResponseDTO {

    private Long id;
    private String title;
    private String severity;
    private String description;
    private String status;

    private OffsetDateTime createdAt;
    private OffsetDateTime endedAt;

    private Long monitorId;
}
