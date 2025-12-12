package com.example.StreetB.dto.ai_DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 응답(Response) DTO -  내부 구조
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class choiceDTO {
    private messageDTO message;
    private String finish_reason;
}
