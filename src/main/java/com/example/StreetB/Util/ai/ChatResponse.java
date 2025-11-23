package com.example.StreetB.Util.ai;

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
public class ChatResponse {
    private String id;
    private List<Choice> choices;
    // usage 등 다른 필드는 생략 가능
}
