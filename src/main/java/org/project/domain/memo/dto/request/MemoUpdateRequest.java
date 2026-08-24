package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

// 제목/본문/태그는 전체 교체. 이미지/파일은 최종 상태를 보내면 서버가 diff(유지=id, 추가=s3Key, 삭제=빠진 것)를 계산한다.
public record MemoUpdateRequest(

        @Schema(description = "제목", example = "SOPT 세미나 (수정)")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "내용", example = "7차 세미나 내용 수정본.")
        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @Schema(description = "태그 이름 목록(전체 교체)", example = "[\"SOPT\", \"교양\"]")
        List<String> tagNames,

        @Schema(description = "이미지 최종 상태 목록 (유지=imageId, 추가=s3Key)")
        List<ImageEdit> images,

        @Schema(description = "파일 최종 상태 목록 (유지=fileId, 추가=s3Key)")
        List<FileEdit> files
) {

        public record ImageEdit(
                @Schema(description = "유지할 기존 이미지 ID. 새로 추가하는 이미지는 null", example = "10", nullable = true)
                Long imageId,

                @Schema(description = "새로 추가하는 이미지의 S3 key. 기존 유지 시 null", example = "memo-image/1/uuid.png", nullable = true)
                String s3Key,

                @Schema(description = "원본 이미지 파일명 (추가 시)", example = "seminar_slide.png", nullable = true)
                String imageName,

                @Schema(description = "파일 크기(bytes) (추가 시)", example = "245678", nullable = true)
                Long bytes,

                @Schema(description = "확장자 (추가 시)", example = "png", nullable = true)
                String extension,

                @Schema(description = "정렬 우선순위", example = "0")
                Integer priority
        ) {
        }

        public record FileEdit(
                @Schema(description = "유지할 기존 파일 ID. 새로 추가하는 파일은 null", example = "20", nullable = true)
                Long fileId,

                @Schema(description = "새로 추가하는 파일의 S3 key. 기존 유지 시 null", example = "memo-file/1/uuid.pdf", nullable = true)
                String s3Key,

                @Schema(description = "원본 파일명 (추가 시)", example = "SOPT_7th_seminar.pdf", nullable = true)
                String fileName,

                @Schema(description = "파일 크기(bytes) (추가 시)", example = "1048576", nullable = true)
                Long bytes,

                @Schema(description = "확장자 (추가 시)", example = "pdf", nullable = true)
                String extension,

                @Schema(description = "정렬 우선순위", example = "0")
                Integer priority
        ) {
        }
}
