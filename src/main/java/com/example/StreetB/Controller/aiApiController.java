// src/main/java/com/example/StreetB/Controller/aiController.java
package com.example.StreetB.Controller;

import com.example.StreetB.Service.aiService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController // 데이터 반환 전용 컨트롤러
@RequestMapping("/lm/api") // API 요청 경로는 /lm/api/...
public class aiApiController {

    private final aiService aiService;

    public aiApiController(aiService aiService) {
        this.aiService = aiService;
    }

    // AI 응답 요청 (POST)
    @PostMapping("/ask")
    public Mono<String> ask(@RequestParam String question) {
        // aiService의 비동기 작업(Mono<String>)을 그대로 반환합니다.
        // Spring WebFlux가 Mono의 완료 시점에 맞춰 비동기적으로 HTTP 응답을 처리합니다.
        return aiService.askModel(question);
    }
}
