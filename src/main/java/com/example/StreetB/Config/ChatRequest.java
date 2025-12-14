package com.example.StreetB.Config;

import com.example.StreetB.dto.ai_DTO.messageDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String model;
    private List<messageDTO> messages;

    // 최대 생성 길이 제한
    private Integer max_tokens = 1024;

    // 출력 다양성 조절 (담백한 응답을 위해 0.2로 설정)
    private Double temperature = 0.2;

    // 고려할 단어 확률 범위 조절 (보통 1.0으로 유지)
    private Double min_p = 1.0;

    // 스트리밍 사용 여부
    private Boolean stream = false;

    // 이전에 사용된 단어에 대한 페널티 (반복 억제를 위해 1.2로 설정)
    private Double frequency_penalty = 1.5;

    // 새로운 주제/단어 도입에 대한 페널티 (보통 0.0 유지)
    // private Double presence_penalty = 0.0;

    // 사용자 편의를 위한 오버로드된 생성자 (기본값 사용)
    public ChatRequest(String model, List<messageDTO> messages) {
        this.model = model;
        this.messages = messages;
    }
}
