package com.example.StreetB.Config;

import com.example.StreetB.dto.ai_DTO.chunkDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;

@Component
public class VectorStoreInMemory {
    /* =========================
       법령 Chunk 저장소 (In-Memory)
     ========================= */
    private final List<chunkDTO> store = new ArrayList<>();

    /* =========================
       기본 관리 메서드
     ========================= */
    public void addChunk(chunkDTO chunk) {
        store.add(chunk);
    }
    public int getSize() {
        return store.size();
    }
    public void clearChunk() {
        store.clear();
    }

    /* =========================
       검색 로직(사용자 질문과 가장 관련성이 높은 법령 청크를 반환)
     ========================= */
    public List<chunkDTO> retrieveRelevantChunks(String userQuery, int topK) {
        if (store.isEmpty()) {
            return List.of();
        }
        // 1.쿼리 단순 토큰화
        List<String> queryWords =
                Arrays.asList(userQuery.toLowerCase().split("\\s+"));
        // 2. 각 청크별 유사도 계산
        for (chunkDTO chunk : store) {
            double score = calculateWeightedSimilarity(chunk, queryWords);
            chunk.setSimilarityScore(score);
        }
        // 3. 점수 기준 정렬
        List<chunkDTO> sortedChunks = new ArrayList<>(store);
        sortedChunks.sort(
                Comparator.comparingDouble(chunkDTO::getSimilarityScore)
                        .reversed()
        );
        // 4. 상위 K개 + 점수 0 이상만 반환
        List<chunkDTO> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, sortedChunks.size()); i++) {
            if (sortedChunks.get(i).getSimilarityScore() > 0) {
                results.add(sortedChunks.get(i));
            }
        }
        return results;
    }


    /* =========================
       유사도 계산 로직(법률 검색용 가중치 기반 유사도 계산)
     ========================= */
    /**
     * 가중치 기준:
     *  - articleTitle : 7
     *  - chapterTitle : 3
     *  - text         : 1
     */
    private double calculateWeightedSimilarity(
            chunkDTO chunk,
            List<String> queryWords
    ) {
        double score = 0.0;
        // 본문 (가중치 1)
        score += countMatches(chunk.getText(), queryWords) * 1;
        // 장 제목 (가중치 3)
        if (chunk.getChapterTitle() != null) {
            score += countMatches(chunk.getChapterTitle(), queryWords) * 3;
        }
        // 조 제목 (가중치 7)
        if (chunk.getArticleTitle() != null) {
            score += countMatches(chunk.getArticleTitle(), queryWords) * 7;
        }
        return score;
    }

    /**
     * 텍스트 내 키워드 매칭 개수 계산
     */
    private long countMatches(String text, List<String> queryWords) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String lower = text.toLowerCase();
        return queryWords.stream()
                .filter(lower::contains)
                .count();
    }
}
