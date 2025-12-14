package com.example.StreetB.Service;

import com.example.StreetB.Config.ChatRequest;
import com.example.StreetB.Config.DocumentChunker;
import com.example.StreetB.Config.VectorStoreInMemory;
import com.example.StreetB.dto.ai_DTO.chunkDTO;
import com.example.StreetB.dto.ai_DTO.messageDTO;
import com.example.StreetB.dto.ai_DTO.responseDTO;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

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

    /* =========================
       설정값
     ========================= */
    private static final String LM_STUDIO_BASE_URL = "http://localhost:1234/v1";
    private static final String MODEL_NAME = "qwen/qwen3-vl-4b";
    private static final String UPLOAD_DIR = "/Users/ichangjun/Documents/StreetB/uploads";

    private final WebClient webClient;
    private final DocumentChunker documentChunker;
    private final VectorStoreInMemory vectorStore;

    /* =========================
       생성자
     ========================= */
    public aiService(
            WebClient.Builder webClientBuilder,
            DocumentChunker documentChunker,
            VectorStoreInMemory vectorStore
    ) {
        this.documentChunker = documentChunker;
        this.vectorStore = vectorStore;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofMinutes(3))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(180))
                                .addHandlerLast(new WriteTimeoutHandler(180))
                );

        this.webClient = webClientBuilder
                .baseUrl(LM_STUDIO_BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /* =========================
       초기화
     ========================= */
    @PostConstruct
    public void initializeDocumentStore() {
        System.out.println("법령 문서 저장소 초기화 시작");
        vectorStore.clearChunk();
        readAndChunkUploadedFiles();
        System.out.println("로드 완료: " + vectorStore.getSize() + "개 조문");
    }

    /* =========================
       사용자 질문 → AI 응답
     ========================= */
    public Mono<String> askModel(String userPrompt) {
        List<messageDTO> messages = new ArrayList<>();
        // 1. 관련 법령 조문 검색
        List<chunkDTO> relevantChunks =
                vectorStore.retrieveRelevantChunks(userPrompt, 10);

        if (!relevantChunks.isEmpty()) {
            StringBuilder contextBuilder = new StringBuilder();
            for (chunkDTO chunk : relevantChunks) {
                contextBuilder
                        .append("【")
                        .append(chunk.getLawName())
                        .append("】\n")
                        .append(chunk.getText())
                        .append("\n\n");
            }
            messages.add(new messageDTO(
                    "system",
                    """
                    너는 공공기관 법령 질의에 답변하는 실무 보조 AI다.
                    다음 규칙을 반드시 지켜라:
                    1. 답변은 반드시 최대 5개 항목까지만 작성한다.
                    2. 동일하거나 유사한 법적 근거는 하나로 통합한다.
                    3. 같은 조문을 중복해서 언급하지 않는다.
                    4. 불필요한 설명, 반복 문장, 유사 표현을 금지한다.
                    5. 각 항목은 2~3문장 이내로 간결하게 작성한다.
                    6. 법령 근거는 항목별이 아니라 답변 맨 마지막에 한 번만 제시한다.
                    7. 근거 형식은 반드시 [근거: 법령명 제○조] 같이 작성한다.
                    항목 수를 늘리기 위해 의미 없는 세부 항목 분할을 하지 마라.
                    """ + contextBuilder
            ));
        } else {
            messages.add(new messageDTO(
                    "system",
                    "관련 법령을 찾지 못했습니다. 보다 정확한 법률 용어로 다시 질문해 주세요."
            ));
        }
        messages.add(new messageDTO("user", userPrompt));
        ChatRequest requestBody = new ChatRequest(MODEL_NAME, messages);
        // 디버그 출력
        System.out.println("===== AI 프롬프트 =====");
        messages.forEach(m ->
                System.out.println("[" + m.getRole() + "]\n" + m.getContent())
        );
        System.out.println("=======================");

        return webClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer lm-studio")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseDTO.class)
                .map(response -> {
                    if (response != null
                            && response.getChoices() != null
                            && !response.getChoices().isEmpty()) {
                        return response.getChoices()
                                .get(0)
                                .getMessage()
                                .getContent();
                    }
                    return "AI 응답을 받지 못했습니다.";
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just("AI 호출 중 오류 발생: " + e.getMessage());
                });
    }

    /* =========================
       파일 로딩 & 청킹
     ========================= */
    private void readAndChunkUploadedFiles() {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            System.err.println("업로드 디렉토리 없음: " + UPLOAD_DIR);
            return;
        }
        try (Stream<Path> paths = Files.list(uploadPath)) {
            paths.forEach(filePath -> {
                String fileName = filePath.getFileName().toString().toLowerCase();
                String content = "";
                try {
                    if (fileName.endsWith(".txt")) {
                        content = Files.readString(filePath);
                    } else if (fileName.endsWith(".pdf")) {
                        content = extractTextFromPdf(filePath.toFile());
                    }

                    if (!content.isBlank()) {
                        List<chunkDTO> chunks =
                                documentChunker.chunkText(fileName, content);
                        for (chunkDTO chunk : chunks) {
                            vectorStore.addChunk(chunk);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("파일 처리 실패: " + fileName);
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            System.err.println("업로드 디렉토리 접근 실패");
            e.printStackTrace();
        }
    }

    /* =========================
       PDF 처리
     ========================= */
    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // ★ 필수
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            String text = stripper.getText(document);

            // 1.<10.0개정> 같은 편집 이력 제거
            text = text.replaceAll("<[^>]+>", "");
            // 2.페이지 머리글/바닥글 제거 (국가법령정보센터 계열)
            text = text.replaceAll(
                    "(?m)^.*(국가법령정보센터|법제처).*$", ""
            );
            // 3.쪽수만 있는 줄 제거
            text = text.replaceAll("(?m)^\\s*\\d+\\s*$", "");
            // 4.연속 공백/개행 정리
            text = text.replaceAll("\\n{2,}", "\n");
            text = text.replaceAll("[ \\t]{2,}", " ");
            return text.trim();
        }
    }

}
