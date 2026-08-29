package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 제목/본문/태그/첨부 모두 "항상 최종 상태"를 보낸다(null 금지, 빈 건 ""/[]). 서버가 현재 상태와 diff(유지=id, 추가=s3Key, 삭제=빠진 것)를 계산한다.
public record MemoUpdateRequest(

        @Schema(description = "제목 (빈 문자열 허용)", example = "SOPT 세미나 (수정)")
        @NotNull(message = "title은 null일 수 없습니다. 제목이 없으면 빈 문자열(\"\")로 보내주세요.")
        String title,

        @Schema(description = "내용 (빈 문자열 허용)", example = "7차 세미나 내용 수정본.")
        @NotNull(message = "content는 null일 수 없습니다. 내용이 없으면 빈 문자열(\"\")로 보내주세요.")
        String content,

        @Schema(description = "태그 이름 목록 (최종 상태, 항상 전달. 태그 없으면 [])", example = "[\"SOPT\", \"교양\"]")
        @NotNull(message = "tagNames는 null일 수 없습니다. 태그가 없으면 빈 배열([])로 보내주세요.")
        List<String> tagNames,

        @Valid
        @Schema(description = "이미지 최종 상태 목록 (유지=imageId, 추가=s3Key, 없으면 [])")
        @NotNull(message = "images는 null일 수 없습니다. 이미지가 없으면 빈 배열([])로 보내주세요.")
        List<ImageEdit> images,

        @Valid
        @Schema(description = "파일 최종 상태 목록 (유지=fileId, 추가=s3Key, 없으면 [])")
        @NotNull(message = "files는 null일 수 없습니다. 파일이 없으면 빈 배열([])로 보내주세요.")
        List<FileEdit> files
) {

        public record ImageEdit(
                @Schema(description = "유지할 기존 이미지 ID. 새로 추가하는 이미지는 null", example = "10", nullable = true)
                Long imageId,

                @Schema(description = "새로 추가하는 이미지의 S3 key. 기존 유지 시 null", example = "memo-image/1/uuid.png", nullable = true)
                String s3Key,

                @Schema(description = "원본 이미지 파일명. 추가(s3Key) 시 필수, 유지 시 null", example = "seminar_slide.png", nullable = true)
                String imageName,

                @Schema(description = "확장자. 추가(s3Key) 시 필수, 유지 시 null", example = "png", nullable = true)
                String extension,

                @Schema(description = "정렬 우선순위 (유지·추가 모두 필수)", example = "0")
                @NotNull(message = "priority는 null일 수 없습니다.")
                Integer priority
        ) {
        }

        public record FileEdit(
                @Schema(description = "유지할 기존 파일 ID. 새로 추가하는 파일은 null", example = "20", nullable = true)
                Long fileId,

                @Schema(description = "새로 추가하는 파일의 S3 key. 기존 유지 시 null", example = "memo-file/1/uuid.pdf", nullable = true)
                String s3Key,

                @Schema(description = "원본 파일명. 추가(s3Key) 시 필수, 유지 시 null", example = "SOPT_7th_seminar.pdf", nullable = true)
                String fileName,

                @Schema(description = "확장자. 추가(s3Key) 시 필수, 유지 시 null", example = "pdf", nullable = true)
                String extension,

                @Schema(description = "정렬 우선순위 (유지·추가 모두 필수)", example = "0")
                @NotNull(message = "priority는 null일 수 없습니다.")
                Integer priority
        ) {
        }
}
