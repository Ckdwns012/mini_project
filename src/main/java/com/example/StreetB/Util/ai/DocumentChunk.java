package com.example.StreetB.Util.ai;

public class DocumentChunk {
    private final String fileName;
    private final int chunkIndex;
    private final String content;

    public DocumentChunk(String fileName, int chunkIndex, String content) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    // 유사도 점수를 임시로 저장하기 위한 필드 (검색 시 사용)
    private double similarityScore;

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }
}
