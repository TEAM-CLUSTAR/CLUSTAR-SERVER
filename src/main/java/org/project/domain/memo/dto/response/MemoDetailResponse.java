package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = {"memoId", "title", "content", "images", "files", "tagList", "createdAt", "updatedAt", "isAiGenerated", "sourceMemoTitleList", "sourceMemos"})
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

        @Schema(description = "메모에 딸린 태그들")
        List<MemoListDashboardResponse.TagResponse> tagList,

        @Schema(description = "메모 생성 시각", example = "2026-01-13T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "메모 마지막 수정 시각", example = "2026-01-13T11:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "AI 생성 여부", example = "false")
        Boolean isAiGenerated,

        @Schema(
                description = "AI 생성 시 참고한 메모 제목 목록 (영구 삭제된 원본은 제외). 칩 표시는 sourceMemos 사용 권장",
                example = "[\"UX 리서치 정리\", \"시험 대비 요약\"]"
        )
        List<String> sourceMemoTitleList,

        @Schema(description = """
                AI 생성 시 참고한 메모 목록 (생성 시 선택한 순서). AI 메모가 아니면 빈 리스트.
                - ACTIVE: 정상 메모 → 클릭 시 메모 상세로 이동
                - TRASHED: 휴지통에 있음 → 흐리게 표시, 클릭 시 휴지통 메모 상세(GET /trash/{memoId})로 이동
                - DELETED: 영구 삭제됨 → 흐리게 표시, 클릭해도 이동 없음 (title은 AI 메모 생성 시점의 제목)
                """)
        List<SourceMemoInfo> sourceMemos
) {

    public enum SourceMemoStatus {
        ACTIVE, TRASHED, DELETED
    }

    @Schema(description = "AI 생성 시 참고한 메모", requiredProperties = {"memoId", "title", "status"})
    public record SourceMemoInfo(

            @Schema(description = "원본 메모 ID", example = "12")
            Long memoId,

            @Schema(description = "원본 메모 제목 (정상·휴지통은 현재 제목, 영구 삭제는 AI 메모 생성 시점 제목)", example = "6차 세미나")
            String title,

            @Schema(description = "원본 메모 상태", example = "ACTIVE")
            SourceMemoStatus status
    ) {}

    @Schema(description = "이미지 정보", requiredProperties = {"imageId", "imageUrl", "imageName", "imageExtension", "imageSize"})
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

    @Schema(description = "첨부 파일 정보", requiredProperties = {"fileId", "fileUrl", "fileName", "fileExtension", "fileSize"})
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

    // 정적 팩터리 메서드. sourceMemos는 Service에서 원본 상태를 판별해 넘긴다(AI 메모가 아니면 빈 리스트).
    public static MemoDetailResponse from(
            Memo memo,
            List<ImageInfo> images,
            List<FileInfo> files,
            List<SourceMemoInfo> sourceMemos
    ) {

        return new MemoDetailResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                images,
                files,
                memo.getMemoTags().stream()
                        .map(MemoTag::getTag)
                        .map(MemoListDashboardResponse.TagResponse::from)
                        .toList(),
                memo.getCreatedAt(),
                memo.getUpdatedAt(),
                memo.getIsAiGenerated(),
                // 기존 필드는 그대로 '지금 남아 있는(정상·휴지통) 원본 제목'만 담는다
                sourceMemos.stream()
                        .filter(source -> source.status() != SourceMemoStatus.DELETED)
                        .map(SourceMemoInfo::title)
                        .toList(),
                sourceMemos
        );
    }
}
