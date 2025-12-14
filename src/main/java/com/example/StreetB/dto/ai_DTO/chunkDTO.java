package com.example.StreetB.dto.ai_DTO;

import lombok.Getter;

import java.util.List;

@Getter
public class chunkDTO {

    /* ===== 법령 메타데이터 ===== */
    private final String lawName;
    private final String chapterTitle;
    private final String articleTitle;
    private final List<String> keywords;
    /* ===== 본문 ===== */
    private final String text;
    /* ===== 관리용 ===== */
    private final String fileName;
    private final int chunkIndex;
    /* ===== 검색용 ===== */
    private double similarityScore;

    public chunkDTO(
            String lawName,
            String chapterTitle,
            String articleTitle,
            List<String> keywords,
            String text,
            String fileName,
            int chunkIndex
    ) {
        this.lawName = lawName;
        this.chapterTitle = chapterTitle;
        this.articleTitle = articleTitle;
        this.keywords = keywords;
        this.text = text;
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
    }
    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }
}
