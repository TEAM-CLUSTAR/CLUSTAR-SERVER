package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.label.entity.Label;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;

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

        @Schema(description = "이미지 정보 목록")
        List<ImageInfo> images,

        @Schema(description = "첨부 파일 정보 목록")
        List<FileInfo> files,

        @Schema(description = "메모에 딸린 라벨들")
        List<MemoListDashboardResponse.LabelResponse> labelList,

        @Schema(description = "메모 생성 시각", example = "2026-01-13T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "AI 생성 여부", example = "false")
        Boolean isAiGenerated,

        @Schema(
                description = "AI 생성 시 참고한 메모 제목 목록",
                example = "[\"UX 리서치 정리\", \"시험 대비 요약\"]"
        )
        List<String> sourceMemoTitleList
) {

    @Schema(description = "이미지 정보")
    public record ImageInfo(

            @Schema(description = "이미지 ID", example = "1")
            Long imageId,

            @Schema(description = "이미지 URL (Presigned URL)")
            String imageUrl,

            @Schema(description = "이미지 파일명", example = "seminar_slide.png")
            String imageName,

            @Schema(description = "이미지 확장자", example = "png")
            String imageExtension,

            @Schema(description = "이미지 크기", example = "0.24MB")
            String imageSize
    ) {}

    @Schema(description = "첨부 파일 정보")
    public record FileInfo(

            @Schema(description = "파일 ID", example = "1")
            Long fileId,

            @Schema(description = "파일 다운로드 URL (Presigned URL)")
            String fileUrl,

            @Schema(description = "파일명", example = "SOPT_7th_seminar.pdf")
            String fileName,

            @Schema(description = "파일 확장자", example = "pdf")
            String fileExtension,

            @Schema(description = "파일 크기", example = "1.00GB")
            String fileSize
    ) {}

    // 정적 팩터리 메서드
    public static MemoDetailResponse from(
            Memo memo,
            List<ImageInfo> images,
            List<FileInfo> files,
            List<Memo> sourceMemos
    ) {

        return new MemoDetailResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                images,
                files,
                memo.getMemoLabels().stream()
                        .map(MemoLabel::getLabel)
                        .map(MemoListDashboardResponse.LabelResponse::from)
                        .toList(),
                memo.getCreatedAt(),
                memo.getIsAiGenerated(),
                memo.getIsAiGenerated()
                        ? extractSourceTitles(sourceMemos)
                        : Collections.emptyList()
        );
    }

    private static List<String> extractSourceTitles(List<Memo> sourceMemos) {
        if (sourceMemos == null || sourceMemos.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceMemos.stream()
                .map(Memo::getTitle)
                .toList();
    }
}
