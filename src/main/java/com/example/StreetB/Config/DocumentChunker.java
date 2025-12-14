package com.example.StreetB.Config;

import com.example.StreetB.dto.ai_DTO.chunkDTO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentChunker {

    public List<chunkDTO> chunkText(String fileName, String text) {

        List<chunkDTO> chunks = new ArrayList<>();

        /* =========================
           0. 법령명 추출 (법/시행령/규칙/규정/지침)
           ========================= */
        String lawName = extractLawName(fileName);

        /* =========================
           1. 장 제목 추출 (번호 제거)
           ========================= */
        Pattern chapterPattern =
                Pattern.compile("(?m)^\\s*제\\s*\\d+장\\s*([^\\n]+)");
        Matcher chapterMatcher = chapterPattern.matcher(text);

        List<Integer> chapterPositions = new ArrayList<>();
        List<String> chapterTitles = new ArrayList<>();

        while (chapterMatcher.find()) {
            chapterPositions.add(chapterMatcher.start());
            chapterTitles.add(chapterMatcher.group(1).trim());
        }

        /* =========================
           2. 조 제목 추출 (괄호 안 제목만)
           ========================= */
        Pattern articlePattern =
                Pattern.compile("(?m)^\\s*제\\s*(\\d+)조(?:\\(([^)]+)\\))?");
        Matcher articleMatcher = articlePattern.matcher(text);
        List<Integer> articlePositions = new ArrayList<>();
        List<String> articleTitles = new ArrayList<>();

        while (articleMatcher.find()) {
            articlePositions.add(articleMatcher.start());
            articleTitles.add(
                    articleMatcher.group(2) != null
                            ? articleMatcher.group(2).trim()
                            : null
            );
        }

        /* =========================
           3. 조 단위 Chunk 생성
           ========================= */
        for (int i = 0; i < articlePositions.size(); i++) {
            int start = articlePositions.get(i);
            int end = (i + 1 < articlePositions.size())
                    ? articlePositions.get(i + 1)
                    : text.length();

            String articleText = text.substring(start, end).trim();
            String chapterTitle =
                    findChapterTitle(start, chapterPositions, chapterTitles);
            chunkDTO dto = new chunkDTO(
                    lawName,                        // 법령명
                    chapterTitle,                   // 장
                    articleTitles.get(i),           // 조
                    extractKeywords(articleText),   // 키워드
                    articleText,                    // 본문
                    fileName,                       // 파일명
                    i                               // chunk index
            );
            chunks.add(dto);
        }
        return chunks;
    }

    /* ======================================================
       법령명 추출
       ====================================================== */
    private String extractLawName(String fileName) {
        String name = fileName.replaceAll("\\.[^.]+$", "");
        Pattern pattern = Pattern.compile(
                "([가-힣\\s]+(시행규칙|시행령|법률|법|규정|지침))"
        );
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "알 수 없음";
    }

    /* =========================
       조 위치 기준 장 제목 찾기
       ========================= */
    private String findChapterTitle(
            int articlePos,
            List<Integer> chapterPositions,
            List<String> chapterTitles
    ) {
        String result = null;
        for (int i = 0; i < chapterPositions.size(); i++) {
            if (chapterPositions.get(i) <= articlePos) {
                result = chapterTitles.get(i);
            } else {
                break;
            }
        }
        return result;
    }

    /* =========================
       키워드 추출
       ========================= */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] candidates = {
                "순서", "절차", "방법", "제공", "동의","벌금","금액"
        };
        for (String k : candidates) {
            if (text.contains(k)) {
                keywords.add(k);
            }
        }
        return keywords;
    }
}
