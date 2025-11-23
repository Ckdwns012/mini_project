// src/main/java/com/example/StreetB/Controller/aiController.java
package com.example.StreetB.Controller;

import com.example.StreetB.Service.aiService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@RestController // 데이터 반환 전용 컨트롤러
@RequestMapping("/lm/api") // API 요청 경로는 /lm/api/...
public class aiApiController {

    private final aiService aiService;

    public aiApiController(aiService aiService) {
        this.aiService = aiService;
    }

    // AI 응답 요청 (POST)
    @PostMapping("/ask")
    public String ask(@RequestParam String question) {
        // aiService는 순수 문자열 반환. @RestController가 이를 HTTP 응답 본문에 그대로 작성
        return aiService.askModel(question);
    }
}
