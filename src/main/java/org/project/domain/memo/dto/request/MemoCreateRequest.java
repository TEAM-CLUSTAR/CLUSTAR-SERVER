package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

        @Valid
        @Schema(description = "이미지 메타데이터 목록 (선택)")
        List<ImageRequest> images,

        @Valid
        @Schema(description = "파일 메타데이터 목록 (선택)")
        List<FileRequest> files
) {

        public record ImageRequest(
                @Schema(description = "S3 key", example = "memo-image/1/53238404-f89d-4728-9dc0-efb2a3c7787b.png")
                String s3Key,

                @Schema(
                        description = "원본 이미지 파일명 (필수)",
                        example = "seminar_slide.png"
                )
                @NotBlank(message = "imageName은 비어 있을 수 없습니다.")
                String imageName,

                @Schema(description = "정렬 우선순위 (필수)", example = "0")
                @NotNull(message = "priority는 null일 수 없습니다.")
                Integer priority
        ) {
        }

        public record FileRequest(
                @Schema(description = "S3 key", example = "memo-file/1/780fd26c-8ab7-4762-b148-b9c8c071795b.pdf")
                String s3Key,

                @Schema(
                        description = "원본 파일명 (필수)",
                        example = "SOPT_7th_seminar.pdf"
                )
                @NotBlank(message = "fileName은 비어 있을 수 없습니다.")
                String fileName,

                @Schema(description = "정렬 우선순위 (필수)", example = "0")
                @NotNull(message = "priority는 null일 수 없습니다.")
                Integer priority
        ) {
        }
}
