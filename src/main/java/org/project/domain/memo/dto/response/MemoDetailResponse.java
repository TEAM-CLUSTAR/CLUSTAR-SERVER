package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.label.entity.Label;
import org.project.domain.memo.entity.Memo;

import java.awt.*;
import java.security.DigestException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record MemoDetailResponse(

        @Schema(description = "메모 ID", example = "1")
        Long memoId,

        @Schema(description = "메모 제목", example = "집에 빨리 가는 법")
        String title,

        @Schema(description = "메모 내용", example = "발박수 치며 날아 간다.")
        String content,

        @Schema(description = "이미지 링크", example = "https://example.com/image.jpg")
        String imageUrl,

        @Schema(description = "메모에 딸린 라벨들", example = "[\"SOPT\", \"졸업프로젝트\", \"교양\", \"레퍼런스\"]")
        List<String> labelList,

        @Schema(description = "메모 생성 시각", example = "\"2026-01-13T10:30:00\"")
        LocalDateTime createdAt,

        @Schema(description = "AI 생성 여부", example = "false")
        Boolean isAiGenerated,

        @Schema(description = "AI 생성 시 참고한 메모 ID 목록", example = "[1, 2, 3]")
        List<Long> sourceList
) {

    public static MemoDetailResponse from(Memo memo) {

        return new MemoDetailResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getImageUrl(),
                memo.getLabels().stream()
                        .map(Label::getName)
                        .toList(),
                memo.getCreatedAt(),
                memo.getIsAiGenerated(),
                // AI 메모면 source 파싱, 아니면 빈 리스트
                memo.getIsAiGenerated() && memo.getSource() != null
                        ? parseSourceIds(memo.getSource())
                        : Collections.emptyList()
        );
    }

    private static List<Long> parseSourceIds(String source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(source.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            // 잘못된 형식이면 빈 리스트 반환
            return Collections.emptyList();
        }
    }
}
