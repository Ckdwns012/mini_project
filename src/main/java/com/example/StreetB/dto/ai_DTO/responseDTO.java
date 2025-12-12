package com.example.StreetB.dto.ai_DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// 전체 응답(Response) DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class responseDTO {
    private String id;
    private List<choiceDTO> choices;
    // usage 등 다른 필드는 생략 가능
}
