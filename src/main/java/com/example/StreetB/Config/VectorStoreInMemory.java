package com.example.StreetB.Config;

import com.example.StreetB.dto.ai_DTO.chunkDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;

@Component
public class VectorStoreInMemory {
    // 실제로는 임베딩 벡터를 저장해야 하지만, 여기서는 단순화를 위해 텍스트 청크만 저장
    private final List<chunkDTO> store = new ArrayList<>();

    public void addChunk(chunkDTO chunk) {
        store.add(chunk);
    }

    public int getSize() {
        return store.size();
    }

    public void clearChunk(){
        this.store.clear();
    }

    /**
     * 사용자 쿼리와 가장 관련성이 높은 청크를 검색합니다.
     * 실제 벡터 검색이 아닌, 단순 키워드 유사도 점수를 매기는 방식으로 구현했습니다.
     * 단순 벡터임베딩은 법률 데이터 청크에서 적합한 조항을 선택하지 못하고, 참조데이터가 많아지면 성능이 급격하게 떨어집니다.
     * 향후, 청크에 키워드를 부여해서 키워드 + 임베딩의 하이브리드 검색 기능으로 발전이 필요합니다.
     */
    public List<chunkDTO> retrieveRelevantChunks(String userQuery, int topK) {
        if (store.isEmpty()) {
            return List.of();
        }

        // 쿼리를 단어로 분할 (아주 단순한 토큰화)
        List<String> queryWords = Arrays.asList(userQuery.toLowerCase().split("\\s+"));

        // 각 청크와 쿼리 간의 유사도 점수 계산 (단어 출현 빈도 기반)
        for (chunkDTO chunk : store) {
            double score = calculateKeywordSimilarity(chunk.getContent(), queryWords);
            chunk.setSimilarityScore(score);
        }

        // 점수 기준으로 내림차순 정렬하고 상위 K개 선택
        List<chunkDTO> sortedChunks = new ArrayList<>(store);
        sortedChunks.sort(Comparator.comparingDouble(chunkDTO::getSimilarityScore).reversed());

        // 점수가 0보다 큰 청크만 반환
        List<chunkDTO> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, sortedChunks.size()); i++) {
            if (sortedChunks.get(i).getSimilarityScore() > 0) {
                results.add(sortedChunks.get(i));
            }
        }

        return results;
    }

    /**
     * 단순한 키워드 기반 유사도 계산 헬퍼 함수
     */
    private double calculateKeywordSimilarity(String text, List<String> queryWords) {
        String lowerCaseText = text.toLowerCase();
        long matchCount = queryWords.stream()
                .filter(lowerCaseText::contains)
                .count();
        // 일치하는 단어 수에 비례하여 점수 반환
        return (double) matchCount;
    }
}
