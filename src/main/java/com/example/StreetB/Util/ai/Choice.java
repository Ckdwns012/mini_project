package com.example.StreetB.Util.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 응답(Response) DTO - Choice 내부 구조
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Choice {
    private Message message;
    private String finish_reason;
}
