package com.example.StreetB.dto.ai_DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Message DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class messageDTO {
    private String role;
    private String content;
}

