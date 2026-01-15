package org.project.domain.ai.service;

public interface MemoMediaAiService {

    /**
     * 이미지 → 설명 텍스트 생성
     */
    String generateImageDescription(String s3Key);

    // 파일 -> 텍스트 추출
    String extractText(String s3Key);
}

