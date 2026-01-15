package org.project.domain.ai.service;

public interface MemoImageAiService {

    /**
     * 이미지 → 설명 텍스트 생성
     */
    String generateImageDescription(String s3Key);
}

