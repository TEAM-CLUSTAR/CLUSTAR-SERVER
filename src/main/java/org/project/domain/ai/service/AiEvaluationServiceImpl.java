package org.project.domain.ai.service;

import org.project.domain.ai.dto.response.AiEvaluationResult;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service
public class AiEvaluationServiceImpl implements AiEvaluationService {

    @Override
    public AiEvaluationResult evaluate(
            String userPrompt,
            MemoAiResponse response
    ) {
        List<String> ragContexts = extractRagContexts(response.usedPrompt());
        String content = response.content();

        double relevance = evaluateRelevance(userPrompt, content, ragContexts);
        double faithfulness = evaluatePromptFaithfulness(response);
        double groundedness = evaluateGroundedness(content, ragContexts);
        boolean taskAlignment = evaluateTaskAlignment(userPrompt, content);

        return AiEvaluationResult.of(
                relevance,
                faithfulness,
                groundedness,
                taskAlignment
        );
    }

    // Relevance Score - 문맥에 맞는가
    private double evaluateRelevance(
            String userPrompt,
            String content,
            List<String> ragContexts
    ) {
        if (ragContexts.isEmpty()) return 0.0;

        long matched =
                ragContexts.stream()
                        .filter(ctx ->
                                containsAnyKeyword(content, extractKeywords(ctx))
                        )
                        .count();

        return (double) matched / ragContexts.size();
    }

    // Prompt Faithfulness - 시스템 프롬프트를 지켰는가
    private double evaluatePromptFaithfulness(MemoAiResponse response) {
        String content = response.content();
        int score = 0;
        int total = 3;

        // 1. 구조화 (파트/섹션)
        if (content.contains("###") || content.contains("파트")) score++;

        // 2. 중복 제거된 서술형 문장
        if (!content.contains("메모 1") && !content.contains("메모 2")) score++;

        // 3. 단순 나열 아님
        if (content.length() > 300 && content.contains("의미")) score++;

        return (double) score / total;
    }

    // Groundedness - 문서 기반인가
    private double evaluateGroundedness(
            String content,
            List<String> ragContexts
    ) {
        if (ragContexts.isEmpty()) return 0.0;

        String mergedContext = String.join(" ", ragContexts);

        long groundedSentences =
                Arrays.stream(content.split("\\."))
                        .filter(sentence ->
                                containsAnyKeyword(
                                        mergedContext,
                                        extractKeywords(sentence)
                                )
                        )
                        .count();

        long totalSentences =
                Math.max(1, content.split("\\.").length);

        return (double) groundedSentences / totalSentences;
    }

    // Task Alignment - 요청을 제대로 수행했는가
    private boolean evaluateTaskAlignment(
            String userPrompt,
            String content
    ) {
        if (userPrompt.contains("정리") || userPrompt.contains("통합")) {
            return content.length() > 200 &&
                    (content.contains("###") || content.contains("-"));
        }
        return !content.isBlank();
    }


    /**
     * 헬퍼 메서드
     */
    private List<String> extractRagContexts(String usedPrompt) {
        if (!usedPrompt.contains("[CONTEXT]")) return List.of();

        String contextPart =
                usedPrompt.substring(usedPrompt.indexOf("[CONTEXT]"));

        return Arrays.stream(contextPart.split("\\[SOURCE:"))
                .skip(1)
                .map(s -> s.substring(s.indexOf("]") + 1).trim())
                .toList();
    }

    private boolean containsAnyKeyword(
            String text,
            List<String> keywords
    ) {
        return keywords.stream().anyMatch(text::contains);
    }

    private List<String> extractKeywords(String text) {
        return Arrays.stream(text
                        .replaceAll("[^가-힣a-zA-Z0-9 ]", "")
                        .split(" "))
                .filter(w -> w.length() >= 2)
                .distinct()
                .limit(5)
                .toList();
    }
}
