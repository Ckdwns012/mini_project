package com.example.StreetB.Service;

import com.example.StreetB.Util.ai.*;
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
    private static final int CHUNK_SIZE = 1000; // 청크 크기 (예: 1000자)

    // 검색 강화를 위해 인메모리 벡터 저장소를 사용합니다.
    private final VectorStoreInMemory vectorStore = new VectorStoreInMemory();

    public aiService(WebClient.Builder webClientBuilder) {
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

    /**
     * 애플리케이션 시작 시 업로드 디렉토리의 파일을 로드하고 청킹합니다.
     */
    @PostConstruct
    public void initializeDocumentStore() {
        System.out.println("문서 저장소를 초기화 중입니다...");
        vectorStore.clearChunk();
        readAndChunkUploadedFiles();
        System.out.println("총 " + vectorStore.getSize() + "개의 문서 청크가 로드되었습니다.");
    }

    public Mono<String> askModel(String userPrompt) {
        List<Message> messages = new ArrayList<>();

        // 개선된 로직: 사용자 프롬프트와 가장 유사한 관련 청크를 검색합니다.
        List<DocumentChunk> relevantChunks = vectorStore.retrieveRelevantChunks(userPrompt, 3); // 상위 3개 청크 가져오기

        if (!relevantChunks.isEmpty()) {
            // 검색된 청크들을 하나의 컨텍스트 문자열로 결합
            StringBuilder contextBuilder = new StringBuilder();
            for (DocumentChunk chunk : relevantChunks) {
                contextBuilder.append("---문서 내용---\n 법률: ").append(chunk.getFileName()).append("\n").append(chunk.getContent()).append("\n\n");
            }
            String relevantContext = contextBuilder.toString();
            // 관련 문서가 있을때의 시스템 메시지 이용
            messages.add(new Message("system", "너는 법령을 구조화해서 요약하는 비서다.\n" +
                    "항목별로 정돈된 한국어 문장으로만 답변하라.\n" +
                    "답변을 참고한 법률과 몇 조에 따른 답변인지 근거를 제시하라.\n" +
                    "시행령, 시행규칙, 법률 모두 법률로 취급한다 .\n" +
                    "근거를 제시할때는 [근거: 법률 몇조] 형식으로 답변 마지막에 제시한다.\n" +
                    "문서의 원문 문장을 그대로 다시 쓰지 마라.\n" + relevantContext));
        } else {
            // 관련 문서를 찾지 못한 경우 일반적인 시스템 메시지 사용
            messages.add(new Message("system", "학습된 법률로는 관련된 정보를 찾을 수 없습니다. 정확한 단어를 사용하여 질의 해주시기 바랍니다. 만 송출해"));
        }

        messages.add(new Message("user", userPrompt));

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
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    if (response != null && !response.getChoices().isEmpty()) {
                        String rawContent = response.getChoices().get(0).getMessage().getContent();
                        // 여기서 텍스트를 가공합니다.
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

    /*
     * AI 응답 문자열을 원하는 형식으로 가공하는 함수
     * - 모든 '*' 문자를 제거합니다.
     * - '숫자.' 앞에 줄바꿈을 추가합니다.
     * @param rawResponse 원본 API 응답 문자열
     * @return 가공된 문자열
     */
//    private String formatText(String rawResponse) {
//        if (rawResponse == null) {
//            return "";
//        }
//
//        // 1. 모든 '*' 문자를 제거합니다.
//        String noStars = rawResponse.replace("*", "");
//
//        // 2. '숫자.' 패턴 앞에 줄바꿈을 삽입합니다. (정규 표현식 사용)
//        //    (\\s)는 앞선 공백을 캡처 그룹 1로, (\\d+\\.)는 숫자+. 패턴을 캡처 그룹 2로 잡습니다.
//        //    replaceAll("\n$2")는 캡처된 공백을 무시하고 숫자+. 앞에 \n을 삽입합니다.
//        Pattern pattern = Pattern.compile("(\\s)(\\d+\\.)");
//        Matcher matcher = pattern.matcher(noStars);
//
//        String formattedText = matcher.replaceAll("\n$2");
//
//        // 3. (선택사항) 문자열 시작 부분의 "AI: " 접두사나 불필요한 시작 공백을 제거합니다.
//        formattedText = formattedText.trim().replaceFirst("^AI: ", "");
//
//        return formattedText;
//    }

    /**
     * 업로드된 파일의 내용을 읽고 작은 청크로 분할하여 인메모리 저장소에 저장합니다.
     */
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
                        // 텍스트를 청크로 분할하고 저장소에 추가
                        List<String> chunks = chunkText(content, CHUNK_SIZE);
                        for (int i = 0; i < chunks.size(); i++) {
                            vectorStore.addChunk(new DocumentChunk(fileName, i, chunks.get(i)));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 텍스트를 고정된 크기의 청크로 분할하는 단순 헬퍼 함수
     */
    private List<String> chunkText(String text, int size) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += size) {
            chunks.add(text.substring(i, Math.min(length, i + size)));
        }
        return chunks;
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
