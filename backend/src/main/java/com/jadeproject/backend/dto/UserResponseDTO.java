package com.jadeproject.backend.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private OffsetDateTime createdAt;
}
