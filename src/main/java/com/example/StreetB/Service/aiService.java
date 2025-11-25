// src/main/java/com/example/StreetB/Service/aiService.java
package com.example.StreetB.Service;

import com.example.StreetB.Util.ai.ChatRequest;
import com.example.StreetB.Util.ai.ChatResponse;
import com.example.StreetB.Util.ai.Message;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service("aiService")
public class aiService {

    private final String LM_STUDIO_BASE_URL = "http://localhost:1234/v1";
    private final String MODEL_NAME = "gemma-3-270m-it-mlx";
    private final WebClient webClient;

    public aiService(WebClient.Builder webClientBuilder) {

        // HttpClient 설정: 연결 타임아웃 10초, 응답 타임아웃 1분 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 타임아웃 10초
                .responseTimeout(Duration.ofMinutes(3)) // 응답 타임아웃 1분
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(180)) // 읽기 타임아웃 60초
                                .addHandlerLast(new WriteTimeoutHandler(180))); // 쓰기 타임아웃 60초

        this.webClient = webClientBuilder
                .baseUrl(LM_STUDIO_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // HTTP 클라이언트 연결
                .build();
    }

    public String askModel(String userPrompt) {

        // 1. 요청 메시지 구성 (시스템 및 사용자 메시지)
        List<Message> messages = new ArrayList<>();
//      messages.add(new Message("system", "if you can't understand, tell me you don't know, all answer are korean"));
        messages.add(new Message("user", userPrompt));

        // 2. ChatRequest 객체 생성 (DTO의 기본값 사용)
        ChatRequest requestBody = new ChatRequest(
                MODEL_NAME,
                messages
        );

        // 3. API 호출
        Mono<ChatResponse> responseMono = webClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                // 해결책: 'lm-studio' 문자열을 사용하여 401 오류 방지
                .header("Authorization", "Bearer lm-studio")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatResponse.class);

        // 4. 응답 처리 (동기식 block() 사용)
        ChatResponse response = responseMono.block();

        if (response != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        } else {
            return "Failed to get AI response or empty response.";
        }
    }
}
