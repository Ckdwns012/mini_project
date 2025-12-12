package com.example.StreetB.Config;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentChunker {

    // CHUNK_SIZE 상수는 더 이상 이 로직에서 사용되지 않습니다.

    /**
     * 법률 문서를 '조' (Article) 단위로 분할합니다.
     * 정규식을 사용하여 새로운 조항이 시작되는 지점에서 텍스트를 분리합니다.
     */
    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        // 정규 표현식 패턴: "제" + 숫자 + "조" + (선택적 괄호 내용)을 찾습니다.
        Pattern pattern = Pattern.compile("(?=\\n\\s*제\\d+조(?:\\(.*?\\))?)");

        // 패턴을 기준으로 텍스트를 분할합니다.
        String[] articles = pattern.split(text);

        for (String article : articles) {
            String trimmedArticle = article.trim();
            if (!trimmedArticle.isEmpty()) {
                chunks.add(trimmedArticle);
            }
        }

        // 문서 시작 부분이 정규식 패턴과 일치하지 않는 경우 (예: "### Page 1 ###" 같은 머리말)
        // 첫 번째 청크가 빈 문자열이 될 수 있습니다. 이 경우 첫 번째 유효한 텍스트를 포함시킵니다.
        if (chunks.isEmpty() && !text.trim().isEmpty()) {
            chunks.add(text.trim());
        }

        return chunks;
    }
}
