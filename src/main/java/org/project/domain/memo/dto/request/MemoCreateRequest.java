package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemoCreateRequest(

        @Schema(description = "제목 (빈 문자열 허용)", example = "SOPT 세미나")
        @NotNull(message = "title은 null일 수 없습니다. 제목이 없으면 빈 문자열(\"\")로 보내주세요.")
        String title,

        @Schema(description = "내용 (빈 문자열 허용)", example = "7차 세미나 내용은 ~가 중요~.")
        @NotNull(message = "content는 null일 수 없습니다. 내용이 없으면 빈 문자열(\"\")로 보내주세요.")
        String content,

        @Schema(description = "태그 이름 목록", example = "[\"SOPT\", \"교양\", \"레퍼런스\"]")
        List<String> tagNames,

        @Schema(description = "이미지 메타데이터 목록 (선택)")
        List<ImageRequest> images,

        @Schema(description = "파일 메타데이터 목록 (선택)")
        List<FileRequest> files
) {

        public record ImageRequest(
                @Schema(description = "S3 key", example = "memo-image/1/53238404-f89d-4728-9dc0-efb2a3c7787b.png")
                String s3Key,

                @Schema(
                        description = "원본 이미지 파일명",
                        example = "seminar_slide.png"
                )
                String imageName,

                @Schema(description = "파일 크기(bytes)", example = "245678")
                Long bytes,

                @Schema(description = "확장자", example = "png")
                String extension,

                @Schema(description = "정렬 우선순위", example = "0")
                Integer priority
        ) {
        }

        public record FileRequest(
                @Schema(description = "S3 key", example = "memo-file/1/780fd26c-8ab7-4762-b148-b9c8c071795b.pdf")
                String s3Key,

                @Schema(
                        description = "원본 파일명",
                        example = "SOPT_7th_seminar.pdf"
                )
                String fileName,

                @Schema(description = "파일 크기(bytes)", example = "1048576")
                Long bytes,

                @Schema(description = "확장자", example = "pdf")
                String extension,

                @Schema(description = "정렬 우선순위", example = "0")
                Integer priority
        ) {
        }
}
