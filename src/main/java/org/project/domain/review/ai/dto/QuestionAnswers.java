package org.project.domain.review.ai.dto;

import java.util.List;

// 질문 하나와 그 질문에 대한 사용자 답들(다듬기 재료)
public record QuestionAnswers(String question, List<String> answers) {
}
