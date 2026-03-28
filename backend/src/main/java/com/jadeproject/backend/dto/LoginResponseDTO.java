package com.jadeproject.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {

    private Long id;
    private String email;
    private String name;
    private String token;
}
