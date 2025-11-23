package com.example.StreetB.Util.ai;

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
    private List<Message> messages;

    // curl 명령어에 있는 필드 추가
    private Double temperature = 0.7;
    private Integer max_tokens = 512; // -1은 무제한을 의미
    private Boolean stream = false; // 기본적으로 스트림 사용 안 함

    // 사용자 편의를 위한 오버로드된 생성자 (기본값 사용)
    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}
