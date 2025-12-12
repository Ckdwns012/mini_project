package com.example.StreetB.Service;

import com.example.StreetB.Config.DocumentChunker;
import com.example.StreetB.Config.ChatRequest;
import com.example.StreetB.Config.VectorStoreInMemory;
import com.example.StreetB.dto.ai_DTO.chunkDTO;
import com.example.StreetB.dto.ai_DTO.messageDTO;
import com.example.StreetB.dto.ai_DTO.responseDTO;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


@Service("aiService")
public class aiService {
    private final String LM_STUDIO_BASE_URL = "http://localhost:1234/v1";
    private final String MODEL_NAME = "qwen/qwen3-vl-4b";
    private final String UPLOAD_DIR = "/Users/ichangjun/Documents/StreetB/uploads";
    private final WebClient webClient;
    // private static final int CHUNK_SIZE = 1000; // DocumentChunker로 이동

    // DocumentChunker를 주입받기 위한 final 필드 선언
    private final DocumentChunker documentChunker;
    // 검색 강화를 위해 인메모리 벡터 저장소를 사용합니다.
    private final VectorStoreInMemory vectorStore;

    // 생성자를 통해 모든 의존성을 주입받습니다.
    public aiService(WebClient.Builder webClientBuilder, DocumentChunker documentChunker, VectorStoreInMemory vectorStore) {
        this.documentChunker = documentChunker;
        this.vectorStore = vectorStore; // 이제 스프링이 관리하는 빈을 사용합니다.

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofMinutes(3))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(180))
                                .addHandlerLast(new WriteTimeoutHandler(180)));

        this.webClient = webClientBuilder
                .baseUrl(LM_STUDIO_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // 애플리케이션 시작 시 업로드 디렉토리의 파일을 로드하고 청킹합니다.
    @PostConstruct
    public void initializeDocumentStore() {
        System.out.println("문서 저장소를 초기화 중입니다...");
        vectorStore.clearChunk();
        readAndChunkUploadedFiles();
        System.out.println("총 " + vectorStore.getSize() + "개의 문서 청크가 로드되었습니다.");
    }

    public Mono<String> askModel(String userPrompt) {
        List<messageDTO> messages = new ArrayList<>();

        // 개선된 로직: 사용자 프롬프트와 가장 유사한 관련 청크를 검색합니다.
        List<chunkDTO> relevantChunks = vectorStore.retrieveRelevantChunks(userPrompt, 3); // 상위 3개 청크 가져오기

        if (!relevantChunks.isEmpty()) {
            // 검색된 청크들을 하나의 컨텍스트 문자열로 결합
            StringBuilder contextBuilder = new StringBuilder();
            for (chunkDTO chunk : relevantChunks) {
                contextBuilder.append("---문서 내용---\n 법률: ").append(chunk.getFileName()).append("\n").append(chunk.getContent()).append("\n\n");
            }
            String relevantContext = contextBuilder.toString();
            // 관련 문서가 있을때의 시스템 메시지 이용
            messages.add(new messageDTO("system", "너는 법령을 구조화해서 요약하는 비서다.\n" +
                    "항목별로 정돈된 한국어 문장으로만 답변하라.\n" +
                    "답변을 참고한 법률과 몇 조에 따른 답변인지 근거를 제시하라.\n" +
                    "시행령, 시행규칙, 법률 모두 법률로 취급한다 .\n" +
                    "근거를 제시할때는 [근거: 법률 몇조] 형식으로 모든답변의 맨 마지막에 한줄을 띄어 제시한다.\n" +
                    "문서의 원문 문장을 그대로 다시 쓰지 마라.\n" + relevantContext));
        } else {
            // 관련 문서를 찾지 못한 경우 일반적인 시스템 메시지 사용
            messages.add(new messageDTO("system", "학습된 법률로는 관련된 정보를 찾을 수 없습니다. 정확한 단어를 사용하여 질의 해주시기 바랍니다. 만 송출해"));
        }

        messages.add(new messageDTO("user", userPrompt));

        ChatRequest requestBody = new ChatRequest(MODEL_NAME, messages);

        // --- AI 모델로 보내는 질의 내용 출력 ---
        System.out.println("--- AI 모델로 전송되는 전체 질의 내용 (프롬프트) ---");
        messages.forEach(msg -> System.out.println("[" + msg.getRole() + "]:\n" + msg.getContent() + "\n"));
        System.out.println("--------------------------------------------------");


        // API 호출 및 응답 처리 (이전과 동일한 비동기 방식 유지)
        return webClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer lm-studio")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(body ->
                                Mono.error(new RuntimeException("API Error: " + response.statusCode() + " - " + body))
                        )
                )
                .bodyToMono(responseDTO.class)
                .map(response -> {
                    // getChoice()의 반환 값이 null인지 먼저 확인합니다.
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String rawContent = response.getChoices().get(0).getMessage().getContent();
                        return rawContent;
                    } else {
                        return "Failed to get AI response or empty response.";
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error calling AI model: " + e.getMessage());
                    return Mono.just("Service temporarily unavailable due to an error: " + e.getMessage());
                });
    }

    // 업로드된 파일의 내용을 읽고 작은 청크로 분할하여 인메모리 저장소에 저장합니다.
    private void readAndChunkUploadedFiles() {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            System.err.println("업로드 디렉토리를 찾을 수 없습니다: " + UPLOAD_DIR);
            return;
        }

        try (Stream<Path> paths = Files.list(uploadPath)) {
            paths.forEach(filePath -> {
                String fileName = filePath.getFileName().toString().toLowerCase();
                String content = "";

                try {
                    if (fileName.endsWith(".txt")) {
                        content = new String(Files.readAllBytes(filePath));
                    } else if (fileName.endsWith(".pdf")) {
                        content = extractTextFromPdf(filePath.toFile());
                    }

                    if (!content.isEmpty()) {
                        // DocumentChunker의 조항 단위 분할 로직 사용 (매개변수 없음)
                        List<String> chunks = documentChunker.chunkText(content);
                        for (int i = 0; i < chunks.size(); i++) {
                            // vectorStore.addChunk()는 이제 예외를 던지지 않습니다.
                            vectorStore.addChunk(new chunkDTO(fileName, i, chunks.get(i)));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error processing file: " + fileName);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error listing files in upload directory.");
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
